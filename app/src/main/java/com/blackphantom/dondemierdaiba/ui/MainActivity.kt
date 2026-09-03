package com.blackphantom.dondemierdaiba.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.data.ContentEntity
import com.blackphantom.dondemierdaiba.data.ContentType
import com.blackphantom.dondemierdaiba.databinding.ActivityMainBinding
import com.blackphantom.dondemierdaiba.dialogs.AddContentDialog
import com.blackphantom.dondemierdaiba.dialogs.EditProgressDialog
import com.blackphantom.dondemierdaiba.util.BackupManager
import com.blackphantom.dondemierdaiba.util.Prefs
import com.blackphantom.dondemierdaiba.widget.SeriesWidgetProvider
import com.blackphantom.dondemierdaiba.work.EpisodeCheckScheduler
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val types = arrayOf(ContentType.SERIE, ContentType.PELICULA, ContentType.VIDEO)
    private val fragments = arrayOfNulls<ContentListFragment>(3)
    private var currentTab = 0
    private var isGrid = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* el usuario decide, no forzamos nada */ }

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleImportUri(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isGrid = Prefs.isGridView(this)
        updateToggleIcon()

        setupFragments()
        setupTabs()
        setupSearch()
        setupToggleView()
        setupFab()
        setupCheckUpdates()
        setupBackupMenu()
        updateFabLabel()
        requestNotificationPermissionIfNeeded()
        val showedBatteryDialog = requestBatteryOptimizationExemptionIfNeeded()

        handleIncomingIntent(intent)

        // Si en esta misma apertura también salió el diálogo de batería, no
        // arrancamos el tour encima para no pisarnos con dos cosas a la vez.
        // Se muestra solo en la próxima apertura.
        if (!Prefs.wasOnboardingShown(this) && !showedBatteryDialog) {
            binding.root.post { startOnboarding() }
        }
    }

    private fun startOnboarding() {
        val targets = OnboardingController.Targets(
            tabLayout = { binding.tabLayout },
            searchBar = { binding.searchContainer },
            toggleViewButton = { binding.btnToggleView },
            checkUpdatesButton = { binding.btnCheckUpdates },
            backupMenuButton = { binding.btnBackupMenu },
            addButton = { binding.btnAdd },
            goToFirstTab = { binding.tabLayout.getTabAt(0)?.select() }
        )
        OnboardingController(this, binding.root, targets).start()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Cubre 3 formas de "llegar" a la app desde afuera:
     * 1) Tocando un archivo de backup compartido (WhatsApp, mail, etc.)
     * 2) Tocando una serie en el widget de pantalla de inicio
     * 3) Tocando el "+" del widget para agregar una serie nueva
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { handleImportUri(it) }
        }

        val editItemId = intent.getLongExtra(EXTRA_OPEN_EDIT_ITEM_ID, -1L)
        if (editItemId > 0) {
            binding.tabLayout.getTabAt(0)?.select()
            EditProgressDialog.newInstance(editItemId).show(supportFragmentManager, "edit_from_widget")
            intent.removeExtra(EXTRA_OPEN_EDIT_ITEM_ID)
        }

        if (intent.getBooleanExtra(EXTRA_OPEN_ADD_SERIE, false)) {
            binding.tabLayout.getTabAt(0)?.select()
            AddContentDialog.newInstance(ContentType.SERIE).show(supportFragmentManager, "add_from_widget")
            intent.removeExtra(EXTRA_OPEN_ADD_SERIE)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * En Android, el sistema (y sobre todo las capas propias de fabricantes
     * como Xiaomi/Samsung/Huawei) puede demorar o directamente no ejecutar
     * el chequeo periódico de nuevos capítulos si la app "no se usó hace
     * rato". Excluirla del ahorro de batería lo hace mucho más confiable.
     * Se pregunta una sola vez (no se vuelve a molestar si el usuario dice
     * "ahora no"), y no hace nada si el dispositivo ya la tiene excluida.
     */
    private fun requestBatteryOptimizationExemptionIfNeeded(): Boolean {
        if (Prefs.wasBatteryPromptShown(this)) return false

        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager ?: return false
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return false

        Prefs.setBatteryPromptShown(this)

        AlertDialog.Builder(this)
            .setTitle(R.string.battery_dialog_title)
            .setMessage(R.string.battery_dialog_message)
            .setPositiveButton(R.string.battery_dialog_open_settings) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Algunos fabricantes no implementan bien este intent; no rompemos nada.
                }
            }
            .setNegativeButton(R.string.battery_dialog_not_now, null)
            .show()

        return true
    }

    private fun setupCheckUpdates() {
        binding.btnCheckUpdates.setOnClickListener {
            EpisodeCheckScheduler.checkNow(this)
            Toast.makeText(this, getString(R.string.checking_updates), Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Backup: compartir / importar ----------

    private fun setupBackupMenu() {
        binding.btnBackupMenu.setOnClickListener { showBackupMenu() }
    }

    private fun showBackupMenu() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_backup_menu, null)
        dialog.setContentView(view)
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundResource(android.R.color.transparent)
        }

        view.findViewById<View>(R.id.btnExport).setOnClickListener {
            dialog.dismiss()
            exportAndShare()
        }
        view.findViewById<View>(R.id.btnImport).setOnClickListener {
            dialog.dismiss()
            // "*/*" en vez de "application/json": muchos gestores de archivos y
            // apps de chat marcan los .json con un mime type genérico distinto.
            importFileLauncher.launch("*/*")
        }
        view.findViewById<View>(R.id.btnReplayTutorial).setOnClickListener {
            dialog.dismiss()
            startOnboarding()
        }

        dialog.show()
    }

    private fun exportAndShare() {
        lifecycleScope.launch {
            try {
                val file = BackupManager.exportToFile(this@MainActivity)
                val uri = FileProvider.getUriForFile(
                    this@MainActivity, "$packageName.fileprovider", file
                )
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(sendIntent, getString(R.string.backup_export_action)))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, getString(R.string.backup_export_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleImportUri(uri: Uri) {
        lifecycleScope.launch {
            val items = BackupManager.readItemsFromUri(this@MainActivity, uri)
            if (items.isNullOrEmpty()) {
                Toast.makeText(this@MainActivity, getString(R.string.backup_import_error), Toast.LENGTH_LONG).show()
            } else {
                showImportConfirmDialog(items)
            }
        }
    }

    private fun showImportConfirmDialog(items: List<ContentEntity>) {
        val series = items.count { it.type == ContentType.SERIE.name }
        val peliculas = items.count { it.type == ContentType.PELICULA.name }
        val videos = items.count { it.type == ContentType.VIDEO.name }

        AlertDialog.Builder(this)
            .setTitle(R.string.backup_import_confirm_title)
            .setMessage(getString(R.string.backup_import_confirm_message, items.size, series, peliculas, videos))
            .setPositiveButton(R.string.backup_import_confirm_yes) { _, _ ->
                lifecycleScope.launch {
                    val summary = BackupManager.importItems(this@MainActivity, items)
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.backup_import_success, summary.total),
                        Toast.LENGTH_LONG
                    ).show()
                    SeriesWidgetProvider.requestRefresh(this@MainActivity)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------- Tabs / lista / búsqueda ----------

    private fun setupFragments() {
        val tx: FragmentTransaction = supportFragmentManager.beginTransaction()
        types.forEachIndexed { index, type ->
            val fragment = ContentListFragment.newInstance(type)
            fragments[index] = fragment
            tx.add(R.id.fragmentContainer, fragment, "tab_$index")
            if (index != 0) tx.hide(fragment)
        }
        tx.commit()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                switchTab(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun switchTab(position: Int) {
        if (position == currentTab) return
        val tx = supportFragmentManager.beginTransaction()
        fragments[currentTab]?.let { tx.hide(it) }
        fragments[position]?.let { tx.show(it) }
        tx.commit()
        currentTab = position
        updateFabLabel()
        binding.editSearch.text?.clear()
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fragments[currentTab]?.setQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupToggleView() {
        binding.btnToggleView.setOnClickListener {
            isGrid = !isGrid
            Prefs.setGridView(this, isGrid)
            updateToggleIcon()
            fragments.forEach { it?.setGridMode(isGrid) }
        }
    }

    private fun updateToggleIcon() {
        binding.btnToggleView.setImageResource(if (isGrid) R.drawable.ic_list else R.drawable.ic_grid)
        binding.btnToggleView.contentDescription =
            getString(if (isGrid) R.string.view_list else R.string.view_grid)
    }

    private fun setupFab() {
        binding.btnAdd.setOnClickListener {
            AddContentDialog.newInstance(types[currentTab]).show(supportFragmentManager, "add_content")
        }
    }

    private fun updateFabLabel() {
        binding.btnAdd.text = when (types[currentTab]) {
            ContentType.SERIE -> getString(R.string.add_serie)
            ContentType.PELICULA -> getString(R.string.add_pelicula)
            ContentType.VIDEO -> getString(R.string.add_video)
        }
    }

    companion object {
        const val EXTRA_OPEN_EDIT_ITEM_ID = "extra_open_edit_item_id"
        const val EXTRA_OPEN_ADD_SERIE = "extra_open_add_serie"
    }
}
