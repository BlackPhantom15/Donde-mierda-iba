package com.blackphantom.dondemierdaiba.ui

import android.graphics.RectF
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.util.Prefs

/**
 * Tour guiado de bienvenida: atenúa la pantalla dejando un "agujero" sobre
 * el botón que se está explicando en cada paso, con un globo de texto al
 * lado. No usa ninguna librería externa, todo hecho con [TutorialOverlayView].
 */
class OnboardingController(
    private val activity: MainActivity,
    private val rootContainer: FrameLayout,
    private val targets: Targets,
    private val onFinished: (() -> Unit)? = null
) {

    /** Los elementos reales de la pantalla que vamos a ir señalando. */
    class Targets(
        val tabLayout: () -> View,
        val searchBar: () -> View,
        val toggleViewButton: () -> View,
        val checkUpdatesButton: () -> View,
        val backupMenuButton: () -> View,
        val addButton: () -> View,
        val goToFirstTab: () -> Unit
    )

    private data class Step(
        val target: (() -> View)?,
        val shape: TutorialOverlayView.HoleShape,
        val titleRes: Int,
        val bodyRes: Int,
        val beforeShow: (() -> Unit)? = null
    )

    private var overlay: TutorialOverlayView? = null
    private var bubbleView: View? = null
    private var currentStepIndex = 0

    private val steps: List<Step> by lazy {
        listOf(
            Step(null, TutorialOverlayView.HoleShape.NONE, R.string.tutorial_welcome_title, R.string.tutorial_welcome_body),
            Step(
                targets.tabLayout, TutorialOverlayView.HoleShape.PILL,
                R.string.tutorial_tabs_title, R.string.tutorial_tabs_body,
                beforeShow = targets.goToFirstTab
            ),
            Step(targets.searchBar, TutorialOverlayView.HoleShape.PILL, R.string.tutorial_search_title, R.string.tutorial_search_body),
            Step(targets.toggleViewButton, TutorialOverlayView.HoleShape.TILE, R.string.tutorial_toggle_title, R.string.tutorial_toggle_body),
            Step(targets.checkUpdatesButton, TutorialOverlayView.HoleShape.TILE, R.string.tutorial_check_updates_title, R.string.tutorial_check_updates_body),
            Step(targets.backupMenuButton, TutorialOverlayView.HoleShape.TILE, R.string.tutorial_backup_title, R.string.tutorial_backup_body),
            Step(targets.addButton, TutorialOverlayView.HoleShape.PILL, R.string.tutorial_add_title, R.string.tutorial_add_body)
        )
    }

    fun start() {
        currentStepIndex = 0

        val ov = TutorialOverlayView(activity)
        overlay = ov
        // Por encima de TODO (el botón Agregar y la barra tienen elevación propia).
        ov.elevation = 100f * activity.resources.displayMetrics.density
        rootContainer.addView(ov, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val bubble = LayoutInflater.from(activity).inflate(R.layout.view_tutorial_bubble, ov, false)
        bubbleView = bubble
        val bubbleParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        bubbleParams.gravity = Gravity.TOP or Gravity.START
        ov.addView(bubble, bubbleParams)

        bubble.findViewById<View>(R.id.btnTutorialSkip).setOnClickListener { finish() }
        bubble.findViewById<Button>(R.id.btnTutorialNext).setOnClickListener { advance() }

        showStep(0)
    }

    private fun showStep(index: Int) {
        val step = steps[index]
        val ov = overlay ?: return
        val bubble = bubbleView ?: return

        step.beforeShow?.invoke()

        // Un post extra por si cambiamos de pestaña recién: dejamos que el
        // layout termine de acomodarse antes de medir dónde está todo.
        ov.post {
            val target = step.target?.invoke()
            ov.setTarget(target, step.shape)

            bubble.findViewById<TextView>(R.id.txtTutorialStepCount).text =
                activity.getString(R.string.tutorial_step_count, index + 1, steps.size)
            bubble.findViewById<TextView>(R.id.txtTutorialTitle).text = activity.getString(step.titleRes)
            bubble.findViewById<TextView>(R.id.txtTutorialBody).text = activity.getString(step.bodyRes)

            val nextBtn = bubble.findViewById<Button>(R.id.btnTutorialNext)
            nextBtn.text = if (index == steps.lastIndex) {
                activity.getString(R.string.tutorial_done)
            } else {
                activity.getString(R.string.tutorial_next)
            }

            bubble.post { positionBubble(ov, bubble, ov.currentHoleRect()) }
        }
    }

    private fun positionBubble(overlay: TutorialOverlayView, bubble: View, holeRect: RectF?) {
        val density = activity.resources.displayMetrics.density
        val margin = (16 * density).toInt()
        val screenW = overlay.width
        val screenH = overlay.height
        val bubbleW = bubble.width
        val bubbleH = bubble.height
        if (screenW == 0 || screenH == 0 || bubbleW == 0 || bubbleH == 0) return

        val params = bubble.layoutParams as FrameLayout.LayoutParams

        if (holeRect == null) {
            // Paso de bienvenida: centrado en la pantalla.
            params.leftMargin = ((screenW - bubbleW) / 2).coerceAtLeast(margin)
            params.topMargin = ((screenH - bubbleH) / 2).coerceAtLeast(margin)
        } else {
            val maxLeft = (screenW - bubbleW - margin).coerceAtLeast(margin)
            var left = (holeRect.centerX() - bubbleW / 2f).toInt()
            left = left.coerceIn(margin, maxLeft)

            val spaceBelow = screenH - holeRect.bottom
            val spaceAbove = holeRect.top
            val top = if (spaceBelow >= bubbleH + margin || spaceBelow >= spaceAbove) {
                (holeRect.bottom + margin).toInt()
            } else {
                (holeRect.top - bubbleH - margin).toInt()
            }

            val maxTop = (screenH - bubbleH - margin).coerceAtLeast(margin)
            params.leftMargin = left
            params.topMargin = top.coerceIn(margin, maxTop)
        }
        bubble.layoutParams = params
    }

    private fun advance() {
        if (currentStepIndex >= steps.lastIndex) {
            finish()
        } else {
            currentStepIndex++
            showStep(currentStepIndex)
        }
    }

    private fun finish() {
        Prefs.setOnboardingShown(activity)
        overlay?.let { rootContainer.removeView(it) }
        overlay = null
        bubbleView = null
        onFinished?.invoke()
    }
}
