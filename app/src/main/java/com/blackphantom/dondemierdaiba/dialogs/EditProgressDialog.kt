package com.blackphantom.dondemierdaiba.dialogs

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.data.AppDatabase
import com.blackphantom.dondemierdaiba.data.ContentEntity
import com.blackphantom.dondemierdaiba.data.ContentType
import com.blackphantom.dondemierdaiba.data.ImageSource
import com.blackphantom.dondemierdaiba.data.VideoPlatform
import com.blackphantom.dondemierdaiba.databinding.DialogEditProgressBinding
import com.blackphantom.dondemierdaiba.util.ImagePickerHelper
import com.blackphantom.dondemierdaiba.util.NumberPickerStyler
import com.blackphantom.dondemierdaiba.util.PlatformVisuals
import com.blackphantom.dondemierdaiba.widget.SeriesWidgetProvider
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class EditProgressDialog : BottomSheetDialogFragment() {

    private var _binding: DialogEditProgressBinding? = null
    private val binding get() = _binding!!
    private var itemId: Long = -1

    // Estado editable de la imagen/plataforma, se inicializa con lo que ya
    // tenía guardado el ítem y se va actualizando con lo que elija el usuario.
    private var currentItem: ContentEntity? = null
    private var pickedImageUri: String? = null
    private var pickedImageSource: ImageSource = ImageSource.NONE
    private var selectedPlatform: VideoPlatform? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { /* algunos proveedores no lo permiten, no pasa nada */ }
            pickedImageUri = uri.toString()
            pickedImageSource = ImageSource.LOCAL
            renderImage()
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) galleryLauncher.launch("image/*")
        else Toast.makeText(requireContext(), "Se necesita permiso para acceder a la galería", Toast.LENGTH_SHORT).show()
    }

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

        childFragmentManager.setFragmentResultListener(
            ImageWebSearchDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val url = bundle.getString(ImageWebSearchDialog.ARG_URL)
            if (!url.isNullOrBlank()) {
                pickedImageUri = url
                pickedImageSource = ImageSource.WEB
                renderImage()
            }
        }

        val dao = AppDatabase.getInstance(requireContext()).contentDao()

        lifecycleScope.launch {
            val item = dao.getById(itemId) ?: run { dismiss(); return@launch }
            currentItem = item

            binding.txtName.setText(item.name)

            // Estado inicial de imagen/plataforma = lo que ya tenía guardado
            pickedImageUri = item.imageUri
            pickedImageSource = item.imageSourceEnum
            selectedPlatform = VideoPlatform.fromNameOrNull(item.platform)
            renderImage()

            // La imagen se puede tocar para cambiarla en los 3 tipos de contenido
            binding.imgPreviewContainer.setOnClickListener {
                ImagePickerHelper.showImageSourceChooser(
                    requireContext(),
                    fragmentManager = childFragmentManager,
                    initialQuery = item.name,
                    onPickGallery = { requestGalleryImage() },
                    onManualUrl = { url ->
                        pickedImageUri = url
                        pickedImageSource = ImageSource.WEB
                        renderImage()
                    }
                )
            }

            // Para videos, además se puede cambiar directamente la plataforma
            if (item.typeEnum == ContentType.VIDEO) {
                binding.chipGroupPlatform.visibility = View.VISIBLE
                setupPlatformChips()
            }

            // Pin "viendo ahora": solo aplica a series, son las que aparecen
            // en el widget de pantalla de inicio.
            if (item.typeEnum == ContentType.SERIE) {
                binding.btnPinWidget.visibility = View.VISIBLE
                updatePinIcon(item.isPinnedToWidget)
                binding.btnPinWidget.setOnClickListener {
                    val newValue = !(currentItem?.isPinnedToWidget ?: false)
                    lifecycleScope.launch {
                        dao.setPinnedToWidget(item.id, newValue, System.currentTimeMillis())
                        currentItem = currentItem?.copy(isPinnedToWidget = newValue)
                        updatePinIcon(newValue)
                        SeriesWidgetProvider.requestRefresh(requireContext())
                        Toast.makeText(
                            requireContext(),
                            getString(if (newValue) R.string.widget_pin_added else R.string.widget_pin_removed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                    if (item.typeEnum == ContentType.SERIE) SeriesWidgetProvider.requestRefresh(requireContext())
                    dismiss()
                }
            }

            binding.btnUpdate.setOnClickListener {
                val newName = binding.txtName.text.toString().trim()
                if (newName.isEmpty()) {
                    binding.txtName.error = getString(R.string.field_name)
                    return@setOnClickListener
                }

                val updated = item.copy(
                    name = newName,
                    season = if (item.typeEnum == ContentType.SERIE) binding.pickerSeason.value else item.season,
                    episode = if (item.typeEnum == ContentType.SERIE) binding.pickerEpisode.value else item.episode,
                    minute = binding.pickerMinute.value,
                    imageUri = pickedImageUri,
                    imageSource = pickedImageSource.name,
                    platform = if (item.typeEnum == ContentType.VIDEO) selectedPlatform?.name else item.platform,
                    isPinnedToWidget = currentItem?.isPinnedToWidget ?: item.isPinnedToWidget,
                    updatedAt = System.currentTimeMillis()
                )
                lifecycleScope.launch {
                    dao.update(updated)
                    if (item.typeEnum == ContentType.SERIE) SeriesWidgetProvider.requestRefresh(requireContext())
                    dismiss()
                }
            }
        }
    }

    private fun requestGalleryImage() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            galleryLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun setupPlatformChips() {
        binding.chipGroupPlatform.removeAllViews()
        VideoPlatform.values().forEach { platform ->
            val chip = Chip(requireContext())
            chip.text = platform.label
            chip.isCheckable = true
            chip.isChecked = platform == selectedPlatform
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#26FFFFFF")
            )
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor(platform.colorHex))
            chip.chipStrokeWidth = 3f
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedPlatform = platform
                    // Elegir una plataforma nueva vuelve la imagen a su badge
                    // preestablecido (a menos que después toquen la imagen de nuevo).
                    pickedImageUri = platform.name
                    pickedImageSource = ImageSource.PRESET
                    renderImage()
                }
            }
            binding.chipGroupPlatform.addView(chip)
        }
    }

    private fun updatePinIcon(pinned: Boolean) {
        binding.btnPinWidget.setImageResource(
            if (pinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outline
        )
    }

    private fun renderImage() {
        when (pickedImageSource) {
            ImageSource.LOCAL, ImageSource.WEB -> {
                binding.txtPlatformInitial.visibility = View.GONE
                binding.imgPreview.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_glass_input)
                Glide.with(this).load(pickedImageUri).into(binding.imgPreview)
            }
            ImageSource.PRESET -> {
                val platform = VideoPlatform.fromNameOrNull(pickedImageUri)
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
