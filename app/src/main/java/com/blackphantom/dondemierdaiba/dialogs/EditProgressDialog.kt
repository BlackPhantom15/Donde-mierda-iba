package com.blackphantom.dondemierdaiba.dialogs

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.data.AppDatabase
import com.blackphantom.dondemierdaiba.data.ContentType
import com.blackphantom.dondemierdaiba.data.ImageSource
import com.blackphantom.dondemierdaiba.data.VideoPlatform
import com.blackphantom.dondemierdaiba.databinding.DialogEditProgressBinding
import com.blackphantom.dondemierdaiba.util.NumberPickerStyler
import com.blackphantom.dondemierdaiba.util.PlatformVisuals
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class EditProgressDialog : BottomSheetDialogFragment() {

    private var _binding: DialogEditProgressBinding? = null
    private val binding get() = _binding!!
    private var itemId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = requireArguments().getLong(ARG_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        // El BottomSheetDialog trae un fondo blanco por defecto detrás del sheet,
        // que se asoma en las esquinas redondeadas de nuestro fondo custom. Lo hacemos
        // transparente para que solo se vea el fondo "vidrio" que dibujamos nosotros.
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundResource(android.R.color.transparent)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dao = AppDatabase.getInstance(requireContext()).contentDao()

        lifecycleScope.launch {
            val item = dao.getById(itemId) ?: run { dismiss(); return@launch }

            binding.txtName.text = item.name

            when (item.imageSourceEnum) {
                ImageSource.LOCAL, ImageSource.WEB -> {
                    binding.txtPlatformInitial.visibility = View.GONE
                    binding.imgPreview.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.bg_glass_input)
                    Glide.with(this@EditProgressDialog).load(item.imageUri).into(binding.imgPreview)
                }
                ImageSource.PRESET -> {
                    val platform = VideoPlatform.fromNameOrNull(item.imageUri)
                    val colorHex = platform?.colorHex ?: "#8B5CF6"
                    binding.imgPreview.setImageDrawable(null)
                    binding.imgPreview.background =
                        PlatformVisuals.glassBackground(requireContext(), colorHex)
                    binding.txtPlatformInitial.apply {
                        visibility = View.VISIBLE
                        text = PlatformVisuals.initial(platform)
                        setTextColor(Color.parseColor(colorHex))
                    }
                }
                ImageSource.NONE -> {
                    binding.txtPlatformInitial.visibility = View.GONE
                    binding.imgPreview.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.bg_glass_input)
                    binding.imgPreview.setImageResource(R.drawable.ic_image_placeholder)
                }
            }

            // Configurar "candados" según el tipo
            val textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            val dividerColor = ContextCompat.getColor(requireContext(), R.color.glass_stroke)

            binding.pickerMinute.minValue = 0
            binding.pickerMinute.maxValue = 999
            binding.pickerMinute.value = item.minute.coerceIn(0, 999)
            binding.pickerMinute.wrapSelectorWheel = false
            NumberPickerStyler.style(binding.pickerMinute, textColor, dividerColor)

            if (item.typeEnum == ContentType.SERIE) {
                binding.columnSeason.visibility = View.VISIBLE
                binding.columnEpisode.visibility = View.VISIBLE
                binding.dividerSeasonEpisode.visibility = View.VISIBLE
                binding.dividerEpisodeMinute.visibility = View.VISIBLE

                binding.pickerSeason.minValue = 0
                binding.pickerSeason.maxValue = 200
                binding.pickerSeason.value = item.season.coerceIn(0, 200)
                binding.pickerSeason.wrapSelectorWheel = false
                NumberPickerStyler.style(binding.pickerSeason, textColor, dividerColor)

                binding.pickerEpisode.minValue = 0
                binding.pickerEpisode.maxValue = 999
                binding.pickerEpisode.value = item.episode.coerceIn(0, 999)
                binding.pickerEpisode.wrapSelectorWheel = false
                NumberPickerStyler.style(binding.pickerEpisode, textColor, dividerColor)
            }

            binding.btnCancel.setOnClickListener { dismiss() }

            binding.btnDelete.setOnClickListener {
                lifecycleScope.launch {
                    dao.delete(item)
                    dismiss()
                }
            }

            binding.btnUpdate.setOnClickListener {
                val updated = item.copy(
                    season = if (item.typeEnum == ContentType.SERIE) binding.pickerSeason.value else item.season,
                    episode = if (item.typeEnum == ContentType.SERIE) binding.pickerEpisode.value else item.episode,
                    minute = binding.pickerMinute.value,
                    updatedAt = System.currentTimeMillis()
                )
                lifecycleScope.launch {
                    dao.update(updated)
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "arg_id"
        fun newInstance(id: Long): EditProgressDialog {
            val f = EditProgressDialog()
            f.arguments = Bundle().apply { putLong(ARG_ID, id) }
            return f
        }
    }
}
