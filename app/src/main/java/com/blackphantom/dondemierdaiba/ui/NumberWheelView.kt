package com.blackphantom.dondemierdaiba.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.blackphantom.dondemierdaiba.R
import kotlin.math.abs

/**
 * "Candado" numérico vertical (rueda con 3 filas visibles) que reemplaza al
 * NumberPicker de Android SOLO para temporada y capítulo. Se hizo a mano porque
 * el NumberPicker nativo no permite pintar cada número de un color distinto, y
 * acá necesitamos poner en verde los capítulos vistos y las temporadas completas.
 *
 * Mantiene la misma API que se usaba con el NumberPicker (minValue, maxValue,
 * value y un listener de cambio), más [isGreen] y [refreshColors].
 */
class NumberWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val density = resources.displayMetrics.density
    private val itemHeightPx = (46 * density).toInt()

    private var currentValue = 0
    private var onChange: ((oldValue: Int, newValue: Int) -> Unit)? = null
    private var onSettled: ((value: Int) -> Unit)? = null
    private var onOverscrollForward: (() -> Unit)? = null
    private var touchStartY = 0f
    private var overscrollFired = false

    var minValue: Int = 0
        set(v) { field = v; onRangeChanged() }

    var maxValue: Int = 0
        set(v) { field = v; onRangeChanged() }

    var value: Int
        get() = currentValue
        set(v) {
            currentValue = v.coerceIn(minValue, maxOf(minValue, maxValue))
            scrollToCurrent()
        }

    /** Si devuelve true para un número, ese número se dibuja en verde. */
    var isGreen: ((Int) -> Boolean)? = null

    private val normalColor = ContextCompat.getColor(context, R.color.text_primary)
    private val greenColor = ContextCompat.getColor(context, R.color.completed_green)

    private val wheelAdapter = WheelAdapter()
    private val snapHelper = LinearSnapHelper()

    private val manager = object : LinearLayoutManager(context) {
        override fun onLayoutCompleted(state: RecyclerView.State?) {
            super.onLayoutCompleted(state)
            styleChildrenAndTrack()
        }
    }

    private val recycler = RecyclerView(context).apply {
        layoutManager = manager
        adapter = wheelAdapter
        overScrollMode = OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        clipToPadding = false
        itemAnimator = null
        setPadding(0, itemHeightPx, 0, itemHeightPx)
        // Que el sheet de abajo no se "robe" el gesto mientras se gira la rueda.
        isNestedScrollingEnabled = false
    }

    init {
        addView(recycler, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        snapHelper.attachToRecyclerView(recycler)

        // Dos líneas naranjas con efecto de luz que marcan la fila elegida,
        // igual que las del NumberPicker de horas y minutos.
        val orange = ContextCompat.getColor(context, R.color.accent_orange)
        for (topOffset in listOf(itemHeightPx, itemHeightPx * 2)) {
            val line = View(context).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(0x00000000, orange, orange, 0x00000000)
                )
            }
            addView(
                line,
                LayoutParams(LayoutParams.MATCH_PARENT, (1.5f * density).toInt()).apply {
                    gravity = Gravity.TOP
                    topMargin = topOffset
                }
            )
        }

        recycler.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                    rv.parent?.requestDisallowInterceptTouchEvent(true)
                }
                trackOverscroll(e)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                trackOverscroll(e)
            }
        })

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                styleChildrenAndTrack()
            }

            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) commitCenterValue()
            }
        })
    }

    fun setOnValueChangedListener(listener: (oldValue: Int, newValue: Int) -> Unit) {
        onChange = listener
    }

    /** Se llama una sola vez cuando la rueda deja de girar y queda clavada en un número. */
    fun setOnValueSettledListener(listener: (value: Int) -> Unit) {
        onSettled = listener
    }

    /**
     * Se llama cuando la persona, estando ya en el último número, sigue girando
     * la rueda hacia adelante (como queriendo pasar al siguiente). Una vez por gesto.
     */
    fun setOnOverscrollForwardListener(listener: () -> Unit) {
        onOverscrollForward = listener
    }

    private fun trackOverscroll(e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartY = e.y
                overscrollFired = false
            }
            MotionEvent.ACTION_MOVE -> {
                val pulledForward = touchStartY - e.y > 28 * density
                if (!overscrollFired && pulledForward && maxValue > minValue &&
                    currentValue == maxValue && !recycler.canScrollVertically(1)
                ) {
                    overscrollFired = true
                    onOverscrollForward?.invoke()
                }
            }
        }
    }

    /** Vuelve a pintar los números (por ejemplo después de marcar un capítulo como visto). */
    fun refreshColors() {
        wheelAdapter.notifyDataSetChanged()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Siempre 3 filas de alto y un ancho fijo, igual que un NumberPicker.
        super.onMeasure(
            MeasureSpec.makeMeasureSpec((88 * density).toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(itemHeightPx * 3, MeasureSpec.EXACTLY)
        )
    }

    private fun onRangeChanged() {
        currentValue = currentValue.coerceIn(minValue, maxOf(minValue, maxValue))
        wheelAdapter.notifyDataSetChanged()
        scrollToCurrent()
    }

    private fun scrollToCurrent() {
        if (wheelAdapter.itemCount == 0) return
        // Con offset 0 la fila queda en el borde de arriba del área sin padding,
        // o sea, justo en el centro de la rueda.
        manager.scrollToPositionWithOffset(currentValue - minValue, 0)
    }

    /** Estira/aclara cada fila según qué tan lejos está del centro y detecta cuál quedó al medio. */
    private fun styleChildrenAndTrack() {
        val centerY = recycler.height / 2f
        var bestChild: View? = null
        var bestDistance = Float.MAX_VALUE

        for (i in 0 until recycler.childCount) {
            val child = recycler.getChildAt(i)
            val distance = abs((child.top + child.bottom) / 2f - centerY)
            val n = (distance / itemHeightPx).coerceAtMost(1f)
            child.alpha = 1f - 0.6f * n
            child.scaleX = 1f - 0.18f * n
            child.scaleY = 1f - 0.18f * n
            if (distance < bestDistance) {
                bestDistance = distance
                bestChild = child
            }
        }

        // Solo avisamos cambios cuando la persona está girando la rueda; los
        // cambios hechos por código (value = ...) no disparan el listener,
        // igual que el NumberPicker.
        if (recycler.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
            val position = bestChild?.let { recycler.getChildAdapterPosition(it) } ?: RecyclerView.NO_POSITION
            if (position != RecyclerView.NO_POSITION) applyValue(minValue + position)
        }
    }

    private fun commitCenterValue() {
        val snapped = snapHelper.findSnapView(manager) ?: return
        val position = recycler.getChildAdapterPosition(snapped)
        if (position != RecyclerView.NO_POSITION) applyValue(minValue + position)
        onSettled?.invoke(currentValue)
    }

    private fun applyValue(newValue: Int) {
        if (newValue == currentValue) return
        val old = currentValue
        currentValue = newValue
        onChange?.invoke(old, newValue)
    }

    private inner class WheelAdapter : RecyclerView.Adapter<WheelAdapter.Holder>() {

        inner class Holder(val text: TextView) : RecyclerView.ViewHolder(text)

        override fun getItemCount(): Int = maxOf(0, maxValue - minValue + 1)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val tv = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, itemHeightPx
                )
                gravity = Gravity.CENTER
                textSize = 22f
                includeFontPadding = false
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            return Holder(tv)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val number = minValue + position
            holder.text.text = number.toString()
            holder.text.setTextColor(if (isGreen?.invoke(number) == true) greenColor else normalColor)
        }
    }
}
