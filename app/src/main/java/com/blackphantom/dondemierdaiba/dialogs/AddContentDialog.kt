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
import com.blackphantom.dondemierdaiba.databinding.DialogAddContentBinding
import com.blackphantom.dondemierdaiba.util.ImagePickerHelper
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class AddContentDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddContentBinding? = null
    private val binding get() = _binding!!

    private lateinit var type: ContentType
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
            renderPreview()
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) galleryLauncher.launch("image/*")
        else Toast.makeText(requireContext(), "Se necesita permiso para acceder a la galería", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = ContentType.valueOf(requireArguments().getString(ARG_TYPE)!!)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        // Igual que en EditProgressDialog: sacamos el fondo blanco por defecto que
        // se asoma detrás de las esquinas redondeadas de nuestro sheet custom.
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundResource(android.R.color.transparent)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtTitle.text = when (type) {
            ContentType.SERIE -> getString(R.string.add_serie)
            ContentType.PELICULA -> getString(R.string.add_pelicula)
            ContentType.VIDEO -> getString(R.string.add_video)
        }

        binding.rowSerie.visibility = if (type == ContentType.SERIE) View.VISIBLE else View.GONE
        binding.chipGroupPlatform.visibility = if (type == ContentType.VIDEO) View.VISIBLE else View.GONE
        binding.imgPreviewContainer.visibility = if (type == ContentType.VIDEO) View.GONE else View.VISIBLE

        if (type == ContentType.VIDEO) {
            setupPlatformChips()
        } else {
            binding.imgPreview.setOnClickListener {
                ImagePickerHelper.showImageSourceChooser(
                    requireContext(),
                    onPickGallery = { requestGalleryImage() },
                    onWebUrl = { url ->
                        pickedImageUri = url
                        pickedImageSource = ImageSource.WEB
                        renderPreview()
                    }
                )
            }
        }

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { save() }
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
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#26FFFFFF")
            )
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor(platform.colorHex))
            chip.chipStrokeWidth = 3f
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedPlatform = platform
                    pickedImageUri = platform.name
                    pickedImageSource = ImageSource.PRESET
                }
            }
            binding.chipGroupPlatform.addView(chip)
        }
    }

    private fun renderPreview() {
        when (pickedImageSource) {
            ImageSource.LOCAL, ImageSource.WEB -> {
                Glide.with(this).load(pickedImageUri).into(binding.imgPreview)
            }
            else -> binding.imgPreview.setImageResource(R.drawable.ic_image_placeholder)
        }
    }

    private fun save() {
        val name = binding.edtName.text.toString().trim()
        if (name.isEmpty()) {
            binding.edtName.error = getString(R.string.field_name)
            return
        }

        if (type == ContentType.VIDEO && selectedPlatform == null) {
            Toast.makeText(requireContext(), getString(R.string.choose_platform), Toast.LENGTH_SHORT).show()
            return
        }

        val minute = binding.edtMinute.text.toString().trim().toIntOrNull() ?: 0
        val season = binding.edtSeason.text.toString().trim().toIntOrNull() ?: 0
        val episode = binding.edtEpisode.text.toString().trim().toIntOrNull() ?: 0

        val entity = ContentEntity(
            type = type.name,
            name = name,
            imageUri = pickedImageUri,
            imageSource = pickedImageSource.name,
            season = if (type == ContentType.SERIE) season else 0,
            episode = if (type == ContentType.SERIE) episode else 0,
            minute = minute,
            platform = selectedPlatform?.name
        )

        lifecycleScope.launch {
            AppDatabase.getInstance(requireContext()).contentDao().insert(entity)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TYPE = "arg_type"
        fun newInstance(type: ContentType): AddContentDialog {
            val f = AddContentDialog()
            f.arguments = Bundle().apply { putString(ARG_TYPE, type.name) }
            return f
        }
    }
}
