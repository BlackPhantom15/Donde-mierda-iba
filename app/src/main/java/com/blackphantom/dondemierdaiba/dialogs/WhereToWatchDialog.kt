package com.blackphantom.dondemierdaiba.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.databinding.DialogWhereToWatchBinding
import com.blackphantom.dondemierdaiba.network.TmdbApi
import kotlinx.coroutines.launch

/**
 * Modal "Dónde ver": consulta en TMDb (los mismos datos que agrega
 * JustWatch, servidos de forma legal y gratuita) en qué plataformas de
 * streaming está disponible una serie o película, en Argentina. A
 * propósito tiene un estilo "vidrio" translúcido distinto del resto de
 * la app, que es neumórfica.
 */
class WhereToWatchDialog : BottomSheetDialogFragment() {

    private var _binding: DialogWhereToWatchBinding? = null
    private val binding get() = _binding!!

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
    ): View {
        _binding = DialogWhereToWatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = requireArguments().getString(ARG_NAME).orEmpty()
        val isMovie = requireArguments().getBoolean(ARG_IS_MOVIE)

        if (!TmdbApi.hasKey()) {
            binding.progressWhereToWatch.visibility = View.GONE
            showEmpty(getString(R.string.where_to_watch_error))
            return
        }

        lifecycleScope.launch {
            val providers = TmdbApi.findWatchProviders(name, isMovie)
            if (_binding == null) return@launch
            binding.progressWhereToWatch.visibility = View.GONE

            if (providers.isNullOrEmpty()) {
                showEmpty(getString(R.string.where_to_watch_empty))
            } else {
                binding.providersContainer.visibility = View.VISIBLE
                binding.txtWhereToWatchAttribution.visibility = View.VISIBLE
                providers.forEach { provider -> addProviderRow(provider) }
            }
        }
    }

    private fun addProviderRow(provider: TmdbApi.Provider) {
        val row = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_watch_provider, binding.providersContainer, false)

        row.findViewById<TextView>(R.id.txtProviderName).text = provider.name
        row.findViewById<TextView>(R.id.txtProviderType).text = provider.type

        val cornerPx = (8 * resources.displayMetrics.density).toInt()
        Glide.with(this)
            .load(provider.logoUrl)
            .transform(RoundedCorners(cornerPx))
            .into(row.findViewById<ImageView>(R.id.imgProviderLogo))

        binding.providersContainer.addView(row)
    }

    private fun showEmpty(message: String) {
        binding.txtWhereToWatchEmpty.text = message
        binding.txtWhereToWatchEmpty.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_NAME = "arg_name"
        private const val ARG_IS_MOVIE = "arg_is_movie"

        fun newInstance(name: String, isMovie: Boolean): WhereToWatchDialog {
            val f = WhereToWatchDialog()
            f.arguments = Bundle().apply {
                putString(ARG_NAME, name)
                putBoolean(ARG_IS_MOVIE, isMovie)
            }
            return f
        }
    }
}
