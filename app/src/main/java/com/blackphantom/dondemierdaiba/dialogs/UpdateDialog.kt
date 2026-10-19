package com.blackphantom.dondemierdaiba.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.app.DownloadManager
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.update.UpdateManager
import com.blackphantom.dondemierdaiba.util.Prefs
import com.blackphantom.dondemierdaiba.work.EpisodeCheckScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Diálogo de actualización, paso por paso y siempre preguntando:
 * buscar -> mostrar novedades -> "¿Descargar?" -> progreso -> "¿Instalar?".
 * La instalación final la confirma la persona en el instalador de Android.
 */
class UpdateDialog(private val activity: AppCompatActivity) {

    private val dialog = Dialog(activity).apply { requestWindowFeature(Window.FEATURE_NO_TITLE) }
    private val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_update, null)

    private val installedText = view.findViewById<TextView>(R.id.updateInstalled)
    private val spinner = view.findViewById<ProgressBar>(R.id.updateSpinner)
    private val statusText = view.findViewById<TextView>(R.id.updateStatus)
    private val notesText = view.findViewById<TextView>(R.id.updateNotes)
    private val progressBar = view.findViewById<ProgressBar>(R.id.updateProgress)
    private val progressText = view.findViewById<TextView>(R.id.updateProgressText)
    private val chkAuto = view.findViewById<CheckBox>(R.id.chkAutoCheck)
    private val btnPrimary = view.findViewById<Button>(R.id.btnUpdatePrimary)
    private val btnSecondary = view.findViewById<Button>(R.id.btnUpdateSecondary)

    private var job: Job? = null
    private var release: UpdateManager.Release? = null
    private var downloadId: Long = -1L
    private var apkReadyFile: java.io.File? = null

    fun show() {
        dialog.setContentView(view)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.65f)
            setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.setOnDismissListener { job?.cancel() }

        installedText.text = activity.getString(
            R.string.update_installed_version, UpdateManager.currentVersionName(activity)
        )
        chkAuto.isChecked = Prefs.isUpdateCheckEnabled(activity)
        chkAuto.setOnCheckedChangeListener { _, checked ->
            Prefs.setUpdateCheckEnabled(activity, checked)
            EpisodeCheckScheduler.scheduleUpdateCheck(activity)
        }

        dialog.show()
        startCheck()
    }

    // ---------- Pasos ----------

    private fun startCheck() {
        render(
            status = activity.getString(R.string.update_checking),
            spinnerVisible = true,
            secondary = activity.getString(R.string.close) to { dialog.dismiss() }
        )
        job?.cancel()
        job = activity.lifecycleScope.launch {
            when (val result = UpdateManager.check(activity)) {
                is UpdateManager.CheckResult.Available -> {
                    release = result.release
                    Prefs.setPendingUpdate(activity, result.release.tag)
                    showAvailable(result.release)
                }
                is UpdateManager.CheckResult.UpToDate -> {
                    Prefs.setPendingUpdate(activity, "")
                    render(
                        status = activity.getString(
                            R.string.update_up_to_date, UpdateManager.currentVersionName(activity)
                        ),
                        primary = activity.getString(R.string.update_check_again) to { startCheck() },
                        secondary = activity.getString(R.string.close) to { dialog.dismiss() }
                    )
                }
                is UpdateManager.CheckResult.Error -> render(
                    status = activity.getString(R.string.update_error_network),
                    primary = activity.getString(R.string.update_retry) to { startCheck() },
                    secondary = activity.getString(R.string.close) to { dialog.dismiss() }
                )
            }
        }
    }

    private fun showAvailable(r: UpdateManager.Release) {
        val size = if (r.apkSize > 0) " · " + Formatter.formatShortFileSize(activity, r.apkSize) else ""
        render(
            status = activity.getString(R.string.update_available, r.tag) + size,
            notes = r.notes.ifBlank { null },
            primary = activity.getString(R.string.update_download) to { askDownload(r) },
            secondary = activity.getString(R.string.update_not_now) to { dialog.dismiss() }
        )
    }

    private fun askDownload(r: UpdateManager.Release) {
        job?.cancel()
        job = activity.lifecycleScope.launch {
            // Si ya se bajó antes y está íntegro, se ofrece instalar directo.
            if (UpdateManager.isAlreadyDownloaded(activity, r)) {
                apkReadyFile = UpdateManager.apkFile(activity, r)
                showReady()
                return@launch
            }
            val id = UpdateManager.startDownload(activity, r)
            if (id == null) {
                render(
                    status = activity.getString(R.string.update_error_download),
                    primary = activity.getString(R.string.update_retry) to { askDownload(r) },
                    secondary = activity.getString(R.string.close) to { dialog.dismiss() }
                )
                return@launch
            }
            downloadId = id
            trackDownload(r, id)
        }
    }

    private suspend fun trackDownload(r: UpdateManager.Release, id: Long) {
        render(
            status = activity.getString(R.string.update_downloading, r.tag),
            progress = 0,
            progressLabel = "",
            secondary = activity.getString(R.string.cancel) to {
                job?.cancel()
                UpdateManager.cancelDownload(activity, id)
                showAvailable(r)
            }
        )
        while (true) { // delay() corta el bucle si se cancela el trabajo (cierra el diálogo)
            val st = UpdateManager.queryDownload(activity, id)
            when (st.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    verifyAndOfferInstall(r)
                    return
                }
                DownloadManager.STATUS_FAILED, -1 -> {
                    render(
                        status = activity.getString(R.string.update_error_download),
                        primary = activity.getString(R.string.update_retry) to { askDownload(r) },
                        secondary = activity.getString(R.string.close) to { dialog.dismiss() }
                    )
                    return
                }
                else -> {
                    val pct = if (st.total > 0) ((st.done * 100) / st.total).toInt() else 0
                    val label = if (st.total > 0) {
                        Formatter.formatShortFileSize(activity, st.done) + " / " +
                            Formatter.formatShortFileSize(activity, st.total)
                    } else ""
                    progressBar.progress = pct
                    progressBar.isIndeterminate = st.total <= 0
                    progressText.text = if (label.isNotEmpty()) "$pct% · $label" else ""
                }
            }
            delay(400)
        }
    }

    private suspend fun verifyAndOfferInstall(r: UpdateManager.Release) {
        val file = UpdateManager.apkFile(activity, r)
        if (file != null && UpdateManager.verify(activity, file, r)) {
            apkReadyFile = file
            showReady()
        } else {
            file?.delete()
            render(
                status = activity.getString(R.string.update_error_verify),
                primary = activity.getString(R.string.update_retry) to { askDownload(r) },
                secondary = activity.getString(R.string.close) to { dialog.dismiss() }
            )
        }
    }

    private fun showReady() {
        render(
            status = activity.getString(R.string.update_ready),
            primary = activity.getString(R.string.update_install) to { askInstall() },
            secondary = activity.getString(R.string.update_later) to { dialog.dismiss() }
        )
    }

    private fun askInstall() {
        val file = apkReadyFile ?: return
        if (!UpdateManager.canInstall(activity)) {
            // Permiso de "instalar apps desconocidas": se da una sola vez desde los ajustes.
            render(
                status = activity.getString(R.string.update_need_permission),
                primary = activity.getString(R.string.update_open_settings) to {
                    UpdateManager.openInstallPermissionSettings(activity)
                    render(
                        status = activity.getString(R.string.update_back_from_settings),
                        primary = activity.getString(R.string.update_install) to { askInstall() },
                        secondary = activity.getString(R.string.update_later) to { dialog.dismiss() }
                    )
                },
                secondary = activity.getString(R.string.update_later) to { dialog.dismiss() }
            )
            return
        }
        if (UpdateManager.install(activity, file)) {
            dialog.dismiss()
        } else {
            render(
                status = activity.getString(R.string.update_error_install),
                primary = activity.getString(R.string.update_install) to { askInstall() },
                secondary = activity.getString(R.string.close) to { dialog.dismiss() }
            )
        }
    }

    // ---------- Pintado ----------

    private fun render(
        status: String,
        spinnerVisible: Boolean = false,
        notes: String? = null,
        progress: Int? = null,
        progressLabel: String? = null,
        primary: Pair<String, () -> Unit>? = null,
        secondary: Pair<String, () -> Unit>? = null
    ) {
        spinner.visibility = if (spinnerVisible) View.VISIBLE else View.GONE
        statusText.text = status
        notesText.visibility = if (notes != null) View.VISIBLE else View.GONE
        notesText.text = notes.orEmpty()
        progressBar.visibility = if (progress != null) View.VISIBLE else View.GONE
        progressText.visibility = if (progress != null) View.VISIBLE else View.GONE
        if (progress != null) {
            progressBar.isIndeterminate = false
            progressBar.progress = progress
            progressText.text = progressLabel.orEmpty()
        }
        if (primary != null) {
            btnPrimary.visibility = View.VISIBLE
            btnPrimary.text = primary.first
            btnPrimary.setOnClickListener { primary.second() }
        } else {
            btnPrimary.visibility = View.GONE
        }
        if (secondary != null) {
            btnSecondary.visibility = View.VISIBLE
            btnSecondary.text = secondary.first
            btnSecondary.setOnClickListener { secondary.second() }
        } else {
            btnSecondary.visibility = View.GONE
        }
    }
}
