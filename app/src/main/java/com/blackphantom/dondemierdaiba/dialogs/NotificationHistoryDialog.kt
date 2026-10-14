package com.blackphantom.dondemierdaiba.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.data.AppDatabase
import com.blackphantom.dondemierdaiba.data.NotificationLogEntity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * Campanita: lista de los últimos avisos de capítulos/temporadas nuevas que
 * mandó la app, por si se borró la notificación del sistema.
 */
class NotificationHistoryDialog : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundResource(android.R.color.transparent)
            sheet?.let { BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_notification_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dao = AppDatabase.getInstance(requireContext()).notificationLogDao()
        val contentDao = AppDatabase.getInstance(requireContext()).contentDao()
        val container = view.findViewById<ViewGroup>(R.id.historyContainer)
        val empty = view.findViewById<View>(R.id.txtHistoryEmpty)
        val clearBtn = view.findViewById<View>(R.id.btnClearHistory)

        var firstRender = true
        val unreadIds = mutableSetOf<Long>()
        dao.observeAll().observe(viewLifecycleOwner) { list ->
            container.removeAllViews()
            empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            clearBtn.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            if (firstRender) unreadIds.addAll(list.filter { !it.isRead }.map { it.id })
            val inflater = LayoutInflater.from(requireContext())
            for (entry in list) {
                container.addView(buildRow(inflater, container, entry, entry.id in unreadIds) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        if (contentDao.getById(entry.itemId) != null) {
                            dismiss()
                            EditProgressDialog.newInstance(entry.itemId)
                                .show(parentFragmentManager, "edit_from_history")
                        } else {
                            Toast.makeText(requireContext(), R.string.notif_history_item_gone, Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
            if (firstRender) {
                firstRender = false
                // Al abrir la campanita, todo queda como leído; los puntitos de
                // esta apertura se conservan en unreadIds.
                viewLifecycleOwner.lifecycleScope.launch { dao.markAllRead() }
            }
        }

        clearBtn.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch { dao.clearAll() }
        }
    }

    private fun buildRow(
        inflater: LayoutInflater, parent: ViewGroup, entry: NotificationLogEntity, unread: Boolean, onClick: () -> Unit
    ): View {
        val row = inflater.inflate(R.layout.item_notification_log, parent, false)
        row.findViewById<TextView>(R.id.txtLogTitle).text = entry.title
        row.findViewById<TextView>(R.id.txtLogBody).text = entry.body
        row.findViewById<TextView>(R.id.txtLogTime).text = DateUtils.getRelativeTimeSpanString(
            entry.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        )
        row.findViewById<View>(R.id.dotUnread).visibility = if (unread) View.VISIBLE else View.INVISIBLE
        row.setOnClickListener { onClick() }
        return row
    }
}
