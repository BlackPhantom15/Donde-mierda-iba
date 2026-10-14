package com.blackphantom.dondemierdaiba.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.network.TmdbApi
import com.blackphantom.dondemierdaiba.util.Prefs
import kotlinx.coroutines.launch

/**
 * Modal para que cada persona cargue su propia clave (gratuita) de TMDb y
 * así active "Dónde ver". La clave se prueba contra TMDb antes de guardarse
 * y queda solo en el dispositivo.
 */
object TmdbKeyDialog {

    fun show(context: Context, onChanged: () -> Unit) {
        val dialog = Dialog(context).apply { requestWindowFeature(Window.FEATURE_NO_TITLE) }
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_tmdb_key, null)
        dialog.setContentView(view)

        val input = view.findViewById<EditText>(R.id.edtTmdbKey)
        val status = view.findViewById<TextView>(R.id.txtTmdbStatus)
        val btnSave = view.findViewById<Button>(R.id.btnTmdbSave)
        val btnRemove = view.findViewById<Button>(R.id.btnTmdbRemove)

        val current = Prefs.getTmdbKey(context)
        input.setText(current)
        btnRemove.visibility = if (current.isBlank()) View.GONE else View.VISIBLE

        view.findViewById<View>(R.id.btnTmdbGetKey).setOnClickListener {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/settings/api"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                Toast.makeText(context, R.string.tmdb_key_no_browser, Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {
            val key = input.text.toString().trim()
            if (key.isEmpty()) {
                status.text = context.getString(R.string.tmdb_key_empty)
                return@setOnClickListener
            }
            btnSave.isEnabled = false
            status.text = context.getString(R.string.tmdb_key_testing)
            val scope = (context as? LifecycleOwner)?.lifecycleScope
            if (scope == null) {
                // Sin ciclo de vida disponible: guardamos sin probar.
                Prefs.setTmdbKey(context, key); dialog.dismiss(); onChanged(); return@setOnClickListener
            }
            scope.launch {
                when (TmdbApi.validateKey(key)) {
                    true -> {
                        Prefs.setTmdbKey(context, key)
                        Toast.makeText(context, R.string.tmdb_key_saved, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        onChanged()
                    }
                    false -> {
                        status.text = context.getString(R.string.tmdb_key_invalid)
                        btnSave.isEnabled = true
                    }
                    null -> {
                        status.text = context.getString(R.string.tmdb_key_network_error)
                        btnSave.isEnabled = true
                    }
                }
            }
        }

        btnRemove.setOnClickListener {
            Prefs.setTmdbKey(context, "")
            Toast.makeText(context, R.string.tmdb_key_removed, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onChanged()
        }

        view.findViewById<View>(R.id.btnTmdbClose).setOnClickListener { dialog.dismiss() }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.65f)
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        dialog.show()
    }
}
