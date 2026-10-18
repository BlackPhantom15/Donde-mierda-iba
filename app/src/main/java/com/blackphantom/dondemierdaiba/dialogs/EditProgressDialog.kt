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
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.data.AppDatabase
import com.blackphantom.dondemierdaiba.data.ContentDao
import com.blackphantom.dondemierdaiba.data.ContentEntity
import com.blackphantom.dondemierdaiba.data.ContentType
import com.blackphantom.dondemierdaiba.data.ImageSource
import com.blackphantom.dondemierdaiba.data.VideoPlatform
import com.blackphantom.dondemierdaiba.databinding.DialogEditProgressBinding
import com.blackphantom.dondemierdaiba.network.TvMazeApi
import com.blackphantom.dondemierdaiba.util.EpisodeCounts
import com.blackphantom.dondemierdaiba.util.EpisodeRatings
import com.blackphantom.dondemierdaiba.util.EpisodeWatched
import com.blackphantom.dondemierdaiba.util.ImagePickerHelper
import com.blackphantom.dondemierdaiba.util.ImagePreviewHelper
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

    // Capítulos por temporada (según TVmaze), para no dejar poner un capítulo
    // que esa temporada no tiene. Vacío si la serie no está vinculada o
    // todavía no se pudo consultar.
    private var episodeCounts: Map<Int, Int> = emptyMap()

    // Calificación y progreso "visto completo", editados en memoria acá y
    // recién persistidos al confirmar (botón de tilde o "Actualizar").
    private var workingEpisodeRatingsJson: String? = null
    private var workingEpisodeWatchedJson: String? = null
    private var workingMovieRating: Int = 0
    private var workingIsCompleted: Boolean = false

    // Última temporada/capítulo "confirmados" (la rueda quedó quieta ahí). Sirven
    // para saber si la persona avanzó o retrocedió y marcar lo anterior como visto.
    private var anchorSeason: Int = 0
    private var anchorEpisode: Int = 0

    private val starButtons: List<ImageButton> by lazy {
        listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
    }

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
            episodeCounts = EpisodeCounts.fromJson(item.episodeCountsJson)
            workingEpisodeRatingsJson = item.episodeRatingsJson
            workingEpisodeWatchedJson = item.episodeWatchedJson
            workingMovieRating = item.rating
            workingIsCompleted = item.isCompleted

            binding.txtName.setText(item.name)

            // Estado inicial de imagen/plataforma = lo que ya tenía guardado
            pickedImageUri = item.imageUri
            pickedImageSource = item.imageSourceEnum
            selectedPlatform = VideoPlatform.fromNameOrNull(item.platform)
            renderImage()

            // Tocar la imagen: si ya hay una foto real (no un badge de
            // plataforma), primero se muestra en grande con opción de
            // cambiarla; si no hay nada que previsualizar, va directo al
            // selector.
            binding.imgPreviewContainer.setOnClickListener {
                val uri = pickedImageUri
                if ((pickedImageSource == ImageSource.LOCAL || pickedImageSource == ImageSource.WEB) && !uri.isNullOrBlank()) {
                    ImagePreviewHelper.showPreview(requireContext(), uri) {
                        openImageChooser(item.name)
                    }
                } else {
                    openImageChooser(item.name)
                }
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

            // Favorito: sube la serie/película/video al tope de la lista (y del
            // widget). Se guarda al instante, igual que el pin del widget.
            updateFavoriteIcon(item.isFavorite)
            binding.btnFavorite.setOnClickListener {
                val newValue = !(currentItem?.isFavorite ?: false)
                lifecycleScope.launch {
                    dao.setFavorite(item.id, newValue)
                    currentItem = currentItem?.copy(isFavorite = newValue)
                    updateFavoriteIcon(newValue)
                    if (item.typeEnum == ContentType.SERIE) SeriesWidgetProvider.requestRefresh(requireContext())
                    Toast.makeText(
                        requireContext(),
                        getString(if (newValue) R.string.favorite_added else R.string.favorite_removed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Confirmar rápido: hace exactamente lo mismo que "Actualizar",
            // pero sin tener que bajar hasta el final del modal.
            binding.btnQuickConfirm.setOnClickListener { performUpdate(item, dao) }

            // Configurar "candados"
            val textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            val dividerColor = ContextCompat.getColor(requireContext(), R.color.accent_orange)

            // Duración: horas + minutos, se guardan como un único total de minutos.
            val totalMinutes = item.minute.coerceIn(0, 5999)
            binding.pickerHour.minValue = 0
            binding.pickerHour.maxValue = 99
            binding.pickerHour.value = totalMinutes / 60
            binding.pickerHour.wrapSelectorWheel = false
            NumberPickerStyler.style(binding.pickerHour, textColor, dividerColor)

            binding.pickerMinute.minValue = 0
            binding.pickerMinute.maxValue = 59
            binding.pickerMinute.value = totalMinutes % 60
            binding.pickerMinute.wrapSelectorWheel = false
            NumberPickerStyler.style(binding.pickerMinute, textColor, dividerColor)

            if (item.typeEnum == ContentType.SERIE) {
                binding.rowSeasonEpisode.visibility = View.VISIBLE
                binding.dividerEpisodeMinute.visibility = View.VISIBLE

                val maxSeason = episodeCounts.keys.maxOrNull() ?: 200
                binding.pickerSeason.minValue = 0
                binding.pickerSeason.maxValue = maxSeason
                binding.pickerSeason.value = item.season.coerceIn(0, maxSeason)
                binding.txtSeasonLabel.text = if (episodeCounts.isNotEmpty()) {
                    getString(R.string.field_season_of, maxSeason)
                } else {
                    getString(R.string.field_season)
                }

                binding.pickerEpisode.minValue = 0

                // Números en verde: capítulos ya vistos de la temporada elegida y
                // temporadas que quedaron completas (todos sus capítulos vistos).
                binding.pickerSeason.isGreen = { season ->
                    season > 0 && EpisodeWatched.isSeasonComplete(
                        workingEpisodeWatchedJson, currentItem?.episodeCountsJson, season
                    )
                }
                binding.pickerEpisode.isGreen = { episode ->
                    episode > 0 && EpisodeWatched.isWatched(
                        workingEpisodeWatchedJson, binding.pickerSeason.value, episode
                    )
                }
                applyEpisodeLimitFor(binding.pickerSeason.value, initialValue = item.episode)

                // Al cambiar la temporada, recalculamos al toque el máximo de
                // capítulo permitido según lo que esa temporada tiene de verdad,
                // y refrescamos la calificación/estado de "visto" de ese capítulo.
                binding.pickerSeason.setOnValueChangedListener { _, newSeason ->
                    applyEpisodeLimitFor(newSeason, initialValue = null)
                    refreshSeriesRatingAndWatchedUi()
                }
                binding.pickerEpisode.setOnValueChangedListener { _, _ ->
                    refreshSeriesRatingAndWatchedUi()
                }

                // Marcado automático: al quedar la rueda quieta, si se avanzó, lo que
                // quedó atrás se pinta de verde como visto.
                anchorSeason = binding.pickerSeason.value
                anchorEpisode = binding.pickerEpisode.value
                binding.pickerSeason.setOnValueSettledListener { onSeasonSettled(it) }
                binding.pickerEpisode.setOnValueSettledListener { onEpisodeSettled(it) }
            }

            setupRatingAndWatchedUi(item)
            setupWhereToWatch(item)

            binding.btnCancel.setOnClickListener { dismiss() }

            binding.btnDelete.setOnClickListener {
                lifecycleScope.launch {
                    dao.delete(item)
                    if (item.typeEnum == ContentType.SERIE) SeriesWidgetProvider.requestRefresh(requireContext())
                    dismiss()
                }
            }

            binding.btnUpdate.setOnClickListener { performUpdate(item, dao) }
        }
    }

    // ---------- Calificación con estrellas + marcar como visto ----------

    private fun setupRatingAndWatchedUi(item: ContentEntity) {
        binding.txtRatingLabel.text = if (item.typeEnum == ContentType.SERIE) {
            getString(R.string.rating_label_series)
        } else {
            getString(R.string.rating_label_movie)
        }

        starButtons.forEachIndexed { index, star ->
            star.setOnClickListener { onStarClicked(item, index + 1) }
        }

        if (item.typeEnum == ContentType.SERIE) {
            refreshSeriesRatingAndWatchedUi()
        } else {
            drawStars(workingMovieRating)
            binding.txtSeasonAverage.visibility = View.GONE
        }

        binding.btnMarkWatched.setOnClickListener { onMarkWatchedClicked(item) }
        updateMarkWatchedButtonUi(item)
    }

    private fun onStarClicked(item: ContentEntity, stars: Int) {
        if (item.typeEnum == ContentType.SERIE) {
            val season = binding.pickerSeason.value
            val episode = binding.pickerEpisode.value
            // Tocar la misma estrella que ya estaba puesta la "apaga" (borra la nota).
            val current = EpisodeRatings.ratingFor(workingEpisodeRatingsJson, season, episode)
            val newValue = if (current == stars) 0 else stars
            workingEpisodeRatingsJson = EpisodeRatings.rate(workingEpisodeRatingsJson, season, episode, newValue)
            refreshSeriesRatingAndWatchedUi()
        } else {
            workingMovieRating = if (workingMovieRating == stars) 0 else stars
            drawStars(workingMovieRating)
        }
    }

    /** Redibuja estrellas + promedio de temporada según la temporada/capítulo elegidos ahora mismo. */
    private fun refreshSeriesRatingAndWatchedUi() {
        val season = binding.pickerSeason.value
        val episode = binding.pickerEpisode.value

        drawStars(EpisodeRatings.ratingFor(workingEpisodeRatingsJson, season, episode))

        val (average, count) = EpisodeRatings.seasonAverage(workingEpisodeRatingsJson, season)
        if (count > 0) {
            binding.txtSeasonAverage.visibility = View.VISIBLE
            binding.txtSeasonAverage.text = getString(R.string.season_average_format, season, average, count)
        } else {
            binding.txtSeasonAverage.visibility = View.GONE
        }

        currentItem?.let { updateMarkWatchedButtonUi(it) }
    }

    private fun drawStars(rating: Int) {
        starButtons.forEachIndexed { index, star ->
            star.setImageResource(if (index < rating) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
        }
    }

    private fun onMarkWatchedClicked(item: ContentEntity) {
        if (item.typeEnum == ContentType.SERIE) {
            toggleWholeSeriesWatched(item)
        } else {
            workingIsCompleted = !workingIsCompleted
            if (workingIsCompleted) {
                Toast.makeText(requireContext(), getString(R.string.movie_complete_toast), Toast.LENGTH_SHORT).show()
            }
        }
        updateMarkWatchedButtonUi(item)
    }

    /**
     * "Marcar visto" en una serie: pone en verde TODAS las temporadas y capítulos
     * de una sola vez (y deja las ruedas en el último capítulo y el contador en 0).
     * Si ya estaba toda completa, lo toca de nuevo y los desmarca a todos.
     */
    private fun toggleWholeSeriesWatched(item: ContentEntity) {
        val counts = EpisodeCounts.fromJson(currentItem?.episodeCountsJson ?: item.episodeCountsJson)
        if (counts.isEmpty()) {
            // Serie sin datos de TVmaze: no sabemos cuántos capítulos tiene cada
            // temporada, así que solo se marca/desmarca la serie como terminada.
            workingIsCompleted = !workingIsCompleted
            if (workingIsCompleted) {
                Toast.makeText(requireContext(), getString(R.string.series_complete_toast), Toast.LENGTH_LONG).show()
                resetDuration()
            }
            return
        }

        val wasComplete = EpisodeWatched.isSeriesComplete(workingEpisodeWatchedJson, currentItem?.episodeCountsJson)
        if (wasComplete) {
            val set = EpisodeWatched.fromJson(workingEpisodeWatchedJson).toMutableSet()
            counts.forEach { (season, total) -> for (ep in 1..total) set.remove("$season-$ep") }
            workingEpisodeWatchedJson = EpisodeWatched.toJson(set)
            workingIsCompleted = false
            Toast.makeText(requireContext(), getString(R.string.series_unmarked_toast), Toast.LENGTH_SHORT).show()
        } else {
            val set = EpisodeWatched.fromJson(workingEpisodeWatchedJson).toMutableSet()
            counts.forEach { (season, total) -> for (ep in 1..total) set.add("$season-$ep") }
            workingEpisodeWatchedJson = EpisodeWatched.toJson(set)
            workingIsCompleted = true

            val lastSeason = counts.keys.max()
            binding.pickerSeason.value = lastSeason
            applyEpisodeLimitFor(lastSeason, initialValue = counts[lastSeason] ?: 0)
            anchorSeason = lastSeason
            anchorEpisode = binding.pickerEpisode.value
            resetDuration()
            Toast.makeText(requireContext(), getString(R.string.series_complete_toast), Toast.LENGTH_LONG).show()
        }
        refreshWheelColors()
        refreshSeriesRatingAndWatchedUi()
    }

    /** Capítulo visto = contador de horas y minutos en 0. */
    private fun resetDuration() {
        binding.pickerHour.value = 0
        binding.pickerMinute.value = 0
    }

    /**
     * La rueda de capítulo quedó quieta. Si se avanzó dentro de la misma temporada,
     * todos los capítulos anteriores quedan marcados como vistos (verde) y el
     * contador arranca en 0 para el nuevo. Si se cayó sobre uno ya visto, también 0.
     */
    private fun onEpisodeSettled(newEpisode: Int) {
        val season = binding.pickerSeason.value
        if (season == anchorSeason && newEpisode > anchorEpisode) {
            var json = workingEpisodeWatchedJson
            for (ep in 1 until newEpisode) json = EpisodeWatched.setWatched(json, season, ep, true)
            workingEpisodeWatchedJson = json
            resetDuration()
            refreshWheelColors()
            checkSeasonDoneToast(season)
        } else if (newEpisode != anchorEpisode &&
            EpisodeWatched.isWatched(workingEpisodeWatchedJson, season, newEpisode)
        ) {
            resetDuration()
        }
        anchorSeason = season
        anchorEpisode = newEpisode
        refreshSeriesRatingAndWatchedUi()
    }

    /**
     * La rueda de temporada quedó quieta. Si se pasó a una temporada más adelante,
     * las anteriores quedan completas (todos sus capítulos en verde), se salta al
     * capítulo 1 de la nueva y el contador vuelve a 0.
     */
    private fun onSeasonSettled(newSeason: Int) {
        if (newSeason > anchorSeason) {
            val counts = EpisodeCounts.fromJson(currentItem?.episodeCountsJson)
            var json = workingEpisodeWatchedJson
            for (s in maxOf(anchorSeason, 1) until newSeason) {
                val total = counts[s] ?: continue
                for (ep in 1..total) json = EpisodeWatched.setWatched(json, s, ep, true)
            }
            workingEpisodeWatchedJson = json
            applyEpisodeLimitFor(newSeason, initialValue = 1)
            resetDuration()
            refreshWheelColors()
            val finished = maxOf(anchorSeason, 1)
            if (anchorSeason >= 1 && EpisodeWatched.isSeasonComplete(json, currentItem?.episodeCountsJson, finished)) {
                Toast.makeText(requireContext(), getString(R.string.season_complete_toast, finished), Toast.LENGTH_LONG).show()
            }
        } else if (newSeason != anchorSeason) {
            val ep = binding.pickerEpisode.value
            if (EpisodeWatched.isWatched(workingEpisodeWatchedJson, newSeason, ep)) resetDuration()
        }
        anchorSeason = newSeason
        anchorEpisode = binding.pickerEpisode.value
        refreshSeriesRatingAndWatchedUi()
    }

    private fun checkSeasonDoneToast(season: Int) {
        val counts = currentItem?.episodeCountsJson
        val total = EpisodeCounts.fromJson(counts)[season] ?: return
        // Solo avisamos cuando llegó al último capítulo y todos los anteriores están en verde.
        if (binding.pickerEpisode.value == total &&
            (1 until total).all { EpisodeWatched.isWatched(workingEpisodeWatchedJson, season, it) }
        ) {
            Toast.makeText(requireContext(), getString(R.string.last_episode_hint, season), Toast.LENGTH_LONG).show()
        }
    }

    /** Repinta los números verdes de las dos ruedas (temporada y capítulo). */
    private fun refreshWheelColors() {
        binding.pickerSeason.refreshColors()
        binding.pickerEpisode.refreshColors()
    }

    private fun updateMarkWatchedButtonUi(item: ContentEntity) {
        val watched = if (item.typeEnum == ContentType.SERIE) {
            val counts = currentItem?.episodeCountsJson
            if (EpisodeCounts.fromJson(counts).isEmpty()) workingIsCompleted
            else EpisodeWatched.isSeriesComplete(workingEpisodeWatchedJson, counts)
        } else {
            workingIsCompleted
        }
        val isSeries = item.typeEnum == ContentType.SERIE
        binding.btnMarkWatched.text = when {
            watched && isSeries -> getString(R.string.mark_series_watched_done)
            watched -> getString(R.string.mark_watched_done)
            isSeries -> getString(R.string.mark_series_watched_action)
            else -> getString(R.string.mark_watched_action)
        }
    }

    // ---------- Dónde ver ----------

    private fun setupWhereToWatch(item: ContentEntity) {
        if (item.typeEnum == ContentType.VIDEO) {
            binding.btnWhereToWatch.visibility = View.GONE
            return
        }
        binding.btnWhereToWatch.setOnClickListener {
            WhereToWatchDialog.newInstance(
                name = binding.txtName.text.toString().trim().ifBlank { item.name },
                isMovie = item.typeEnum == ContentType.PELICULA
            ).show(childFragmentManager, "where_to_watch")
        }
    }

    // ---------- Guardar ----------

    private fun performUpdate(item: ContentEntity, dao: ContentDao) {
        val newName = binding.txtName.text.toString().trim()
        if (newName.isEmpty()) {
            binding.txtName.error = getString(R.string.field_name)
            return
        }

        binding.btnUpdate.isEnabled = false
        binding.btnQuickConfirm.isEnabled = false

        lifecycleScope.launch {
            var tvMazeId = item.tvMazeId
            var remoteSeason = item.remoteSeason
            var remoteEpisode = item.remoteEpisode
            var episodeCountsJson = item.episodeCountsJson

            // Si la serie NO estaba vinculada a TVmaze y le cambiaron
            // el nombre, probamos vincularla de nuevo con el nombre
            // corregido. Si encuentra algo y todavía no hay imagen
            // elegida a mano, usamos el póster que trae TVmaze.
            val shouldRelink = item.typeEnum == ContentType.SERIE &&
                item.tvMazeId == null && newName != item.name

            if (shouldRelink) {
                binding.btnUpdate.text = getString(R.string.checking_updates)
                val match = TvMazeApi.searchShow(newName)
                if (match != null) {
                    tvMazeId = match.id

                    if (pickedImageSource == ImageSource.NONE && !match.imageUrl.isNullOrBlank()) {
                        pickedImageUri = match.imageUrl
                        pickedImageSource = ImageSource.WEB
                    }

                    val latest = TvMazeApi.latestAiredEpisode(match.id)
                    if (latest != null) {
                        remoteSeason = latest.season
                        remoteEpisode = latest.number
                    }

                    val counts = TvMazeApi.episodesBySeason(match.id)
                    if (counts != null) episodeCountsJson = EpisodeCounts.toJson(counts)

                    if (isAdded) {
                        Toast.makeText(requireContext(), getString(R.string.tvmaze_linked), Toast.LENGTH_LONG).show()
                    }
                }
            }

            val updated = item.copy(
                name = newName,
                season = if (item.typeEnum == ContentType.SERIE) binding.pickerSeason.value else item.season,
                episode = if (item.typeEnum == ContentType.SERIE) binding.pickerEpisode.value else item.episode,
                minute = binding.pickerHour.value * 60 + binding.pickerMinute.value,
                imageUri = pickedImageUri,
                imageSource = pickedImageSource.name,
                platform = if (item.typeEnum == ContentType.VIDEO) selectedPlatform?.name else item.platform,
                isPinnedToWidget = currentItem?.isPinnedToWidget ?: item.isPinnedToWidget,
                isFavorite = currentItem?.isFavorite ?: item.isFavorite,
                tvMazeId = tvMazeId,
                remoteSeason = remoteSeason,
                remoteEpisode = remoteEpisode,
                episodeCountsJson = episodeCountsJson,
                rating = if (item.typeEnum == ContentType.SERIE) item.rating else workingMovieRating,
                episodeRatingsJson = workingEpisodeRatingsJson,
                episodeWatchedJson = workingEpisodeWatchedJson,
                isCompleted = if (item.typeEnum == ContentType.SERIE) {
                    if (EpisodeCounts.fromJson(episodeCountsJson).isEmpty()) workingIsCompleted
                    else EpisodeWatched.isSeriesComplete(workingEpisodeWatchedJson, episodeCountsJson)
                } else {
                    workingIsCompleted
                },
                updatedAt = System.currentTimeMillis()
            )

            dao.update(updated)
            if (item.typeEnum == ContentType.SERIE) SeriesWidgetProvider.requestRefresh(requireContext())
            dismiss()
        }
    }

    /**
     * Ajusta el máximo del candado de capítulo según la temporada elegida.
     * Si no tenemos datos de esa temporada (serie no vinculada, o TVmaze
     * todavía no la tiene cargada), no restringe nada.
     */
    private fun applyEpisodeLimitFor(season: Int, initialValue: Int?) {
        val maxForSeason = episodeCounts[season]
        val newMax = maxForSeason ?: 999
        val desiredValue = initialValue ?: binding.pickerEpisode.value
        val clampedValue = desiredValue.coerceIn(0, newMax)

        binding.pickerEpisode.maxValue = newMax
        binding.pickerEpisode.value = clampedValue

        binding.txtEpisodeLabel.text = if (maxForSeason != null) {
            getString(R.string.field_episode_of, maxForSeason)
        } else {
            getString(R.string.field_episode)
        }
    }

    private fun openImageChooser(itemName: String) {
        ImagePickerHelper.showImageSourceChooser(
            requireContext(),
            fragmentManager = childFragmentManager,
            initialQuery = itemName,
            onPickGallery = { requestGalleryImage() },
            onManualUrl = { url ->
                pickedImageUri = url
                pickedImageSource = ImageSource.WEB
                renderImage()
            }
        )
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

    private fun updateFavoriteIcon(favorite: Boolean) {
        binding.btnFavorite.setImageResource(
            if (favorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        binding.btnFavorite.contentDescription = getString(
            if (favorite) R.string.favorite_remove_cd else R.string.favorite_add_cd
        )
    }

    private fun updatePinIcon(pinned: Boolean) {
        binding.btnPinWidget.setImageResource(
            if (pinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outline
        )
    }

    private fun renderImage() {
        // Radio en píxeles equivalente a los 16dp de bg_glass_input: si no se
        // convierte a densidad, en pantallas de alta densidad el recorte de
        // Glide queda más chico que el del fondo y se ven las puntas cuadradas
        // de la foto asomando por las esquinas redondeadas del marco.
        val cornerPx = (16 * resources.displayMetrics.density).toInt()

        when (pickedImageSource) {
            ImageSource.LOCAL, ImageSource.WEB -> {
                binding.txtPlatformInitial.visibility = View.GONE
                binding.imgPreview.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_glass_input)
                Glide.with(this)
                    .load(pickedImageUri)
                    .transform(CenterCrop(), RoundedCorners(cornerPx))
                    .into(binding.imgPreview)
            }
            ImageSource.PRESET -> {
                val platform = VideoPlatform.fromNameOrNull(pickedImageUri)
                val colorHex = platform?.colorHex ?: "#E8792E"
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
