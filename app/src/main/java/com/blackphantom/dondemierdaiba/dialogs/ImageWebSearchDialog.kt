package com.blackphantom.dondemierdaiba.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.blackphantom.dondemierdaiba.adapter.ImageResultAdapter
import com.blackphantom.dondemierdaiba.databinding.DialogImageWebSearchBinding
import com.blackphantom.dondemierdaiba.network.OpenverseApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ImageWebSearchDialog : BottomSheetDialogFragment() {

    private var _binding: DialogImageWebSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ImageResultAdapter
    private var searchJob: Job? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundResource(android.R.color.transparent)
            // Le damos casi toda la altura de la pantalla para que la grilla de
            // resultados tenga lugar para respirar, como un buscador de verdad.
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet as View)
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            sheet.layoutParams.height = (resources.displayMetrics.heightPixels * 0.85).toInt()
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogImageWebSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ImageResultAdapter { result ->
            setFragmentResult(REQUEST_KEY, bundleOf(ARG_URL to result.fullUrl))
            dismiss()
        }
        binding.recyclerResults.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerResults.adapter = adapter

        val initialQuery = requireArguments().getString(ARG_INITIAL_QUERY).orEmpty()
        binding.edtQuery.setText(initialQuery)

        binding.btnSearch.setOnClickListener { runSearch() }
        binding.edtQuery.setOnEditorActionListener { _, actionId, event ->
            val isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH
            val isEnterKeyDown = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (isSearchAction || isEnterKeyDown) {
                runSearch()
                true
            } else {
                false
            }
        }

        if (initialQuery.isNotBlank()) runSearch()
    }

    private fun runSearch() {
        val query = binding.edtQuery.text.toString().trim()
        if (query.isEmpty()) return

        searchJob?.cancel()
        binding.progress.visibility = View.VISIBLE
        binding.txtEmpty.visibility = View.GONE

        searchJob = lifecycleScope.launch {
            val results = OpenverseApi.search(query)
            if (_binding == null) return@launch
            binding.progress.visibility = View.GONE
            adapter.submitList(results)
            binding.txtEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "image_web_search_result"
        const val ARG_URL = "picked_url"
        private const val ARG_INITIAL_QUERY = "arg_initial_query"

        fun newInstance(initialQuery: String = ""): ImageWebSearchDialog {
            val f = ImageWebSearchDialog()
            f.arguments = Bundle().apply { putString(ARG_INITIAL_QUERY, initialQuery) }
            return f
        }
    }
}
