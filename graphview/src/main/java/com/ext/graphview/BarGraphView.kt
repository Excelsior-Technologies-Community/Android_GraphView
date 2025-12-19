package com.ext.graphview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class BarGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ================= DATA =================
    private val bars = mutableListOf<LineData>()
    private var showGrid = false
    private var showLegend = true
    private var touchedBar: Pair<Int, Int>? = null
    // Pair<datasetIndex, barIndex>
    private var defaultBarColor: Int = Color.BLUE
    private var axisColor: Int = Color.BLACK
    private var textColor: Int = Color.BLACK
    private val leftPadding = 100f
    private val rightPadding = 80f
    private val topPadding = 80f
    private val bottomPadding = 120f



    // ================= PAINTS =================
    private val axisPaint = Paint().apply {
        color = axisColor
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val barPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = textColor
        textSize = 28f
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
    }

    private val tooltipPaint = Paint().apply {
        color = Color.BLACK
        textSize = 32f
        isAntiAlias = true
    }

    private val tooltipBgPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }

    private val legendTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        isAntiAlias = true
    }

    private val legendBoxPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }



    // ================= XML =================
    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.LineGraphView)

            showGrid = ta.getBoolean(R.styleable.LineGraphView_showGrid, false)
            showLegend = ta.getBoolean(R.styleable.LineGraphView_showLegend, true)

            // ✅ READ BAR COLOR FROM XML
            defaultBarColor = ta.getColor(
                R.styleable.LineGraphView_barColor,
                Color.BLUE
            )

            axisColor = ta.getColor(
                R.styleable.LineGraphView_axisColor,
                Color.BLACK
            )

            // ✅ TEXT COLOR FROM XML
            textColor = ta.getColor(
                R.styleable.LineGraphView_textColor,
                Color.BLACK
            )

            ta.recycle()

            axisPaint.color = axisColor
            textPaint.color = textColor
        }
    }


    // ================= DRAW =================
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (showGrid) drawGrid(canvas)
        drawAxes(canvas)

        if (bars.isEmpty()) return

        val (minX, maxX, minY, maxY) = getBounds()

        drawYAxisLabels(canvas, minY, maxY)
        drawXAxisLabels(canvas, minX, maxX)

        drawBars(canvas, minX, maxX, minY, maxY)

        touchedBar?.let { (setIndex, barIndex) ->
            drawBarTooltip(canvas, setIndex, barIndex, minX, maxX, minY, maxY)
        }

        if (showLegend) drawLegend(canvas)
    }

    // ================= BAR DRAW =================
    private fun drawBars(
        canvas: Canvas,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float
    ) {
        val barCount = bars[0].xValues.size
        val graphWidth = width - leftPadding - rightPadding
        val groupWidth = graphWidth / barCount
        val barWidth = groupWidth / bars.size * 0.7f

        bars.forEachIndexed { dataIndex, data ->
            barPaint.color =
                if (data.color != Color.TRANSPARENT) data.color
                else defaultBarColor

            data.yValues.forEachIndexed { index, value ->
                val left =
                    leftPadding + index * groupWidth + dataIndex * barWidth
                val right = left + barWidth

                val top = mapY(value, minY, maxY)
                val bottom = height - bottomPadding

                canvas.drawRect(left, top, right, bottom, barPaint)
            }
        }
    }


    // ================= TOOLTIP =================
    private fun drawBarTooltip(
        canvas: Canvas,
        setIndex: Int,
        barIndex: Int,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float
    ) {
        val groupWidth = (width - 150f) / bars[0].xValues.size
        val barWidth = groupWidth / bars.size * 0.7f

        val left =
            100f + barIndex * groupWidth + setIndex * barWidth
        val right = left + barWidth
        val top = mapY(bars[setIndex].yValues[barIndex], minY, maxY)

        val cx = (left + right) / 2
        val cy = top - 20f

        val text = bars[setIndex].yValues[barIndex].toString()
        val textWidth = tooltipPaint.measureText(text)
        val padding = 12f

        val rawLeft = cx - textWidth / 2 - padding
        val rawRight = cx + textWidth / 2 + padding
        val rawTop = top - 60f

        val tooltipLeft = rawLeft.coerceAtLeast(leftPadding)
        val tooltipRight = rawRight.coerceAtMost(width - rightPadding)
        val tooltipTop = rawTop.coerceAtLeast(topPadding)
        val tooltipBottom = tooltipTop + 40f

        canvas.drawRoundRect(
            tooltipLeft,
            tooltipTop,
            tooltipRight,
            tooltipBottom,
            12f,
            12f,
            tooltipBgPaint
        )

        canvas.drawText(
            text,
            tooltipLeft + padding,
            tooltipBottom - 12f,
            tooltipPaint
        )

    }

    // ================= TOUCH =================
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bars.isEmpty()) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                touchedBar = findTouchedBar(event.x, event.y)
                invalidate()
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                touchedBar = null
                invalidate()
            }
        }
        return true
    }

    private fun findTouchedBar(x: Float, y: Float): Pair<Int, Int>? {
        val (minX, maxX, minY, maxY) = getBounds()

        val groupWidth = (width - 150f) / bars[0].xValues.size
        val barWidth = groupWidth / bars.size * 0.7f

        bars.forEachIndexed { setIndex, data ->
            data.xValues.forEachIndexed { barIndex, _ ->
                val left =
                    100f + barIndex * groupWidth + setIndex * barWidth
                val right = left + barWidth
                val top = mapY(data.yValues[barIndex], minY, maxY)
                val bottom = height - 100f

                if (x in left..right && y in top..bottom) {
                    return Pair(setIndex, barIndex)
                }
            }
        }
        return null
    }

    // ================= AXES & UTILS =================
    private fun drawAxes(canvas: Canvas) {
        // X axis
        canvas.drawLine(
            leftPadding,
            height - bottomPadding,
            width - rightPadding,
            height - bottomPadding,
            axisPaint
        )

        // Y axis
        canvas.drawLine(
            leftPadding,
            topPadding,
            leftPadding,
            height - bottomPadding,
            axisPaint
        )
    }


    private fun drawGrid(canvas: Canvas) {
        val steps = 5
        val h = height - 150f
        for (i in 0..steps) {
            val y = 50f + h / steps * i
            canvas.drawLine(100f, y, width - 50f, y, gridPaint)
        }
    }

    private fun drawYAxisLabels(canvas: Canvas, minY: Float, maxY: Float) {
        val steps = 5
        val step = (maxY - minY) / steps
        val h = height - 150f

        for (i in 0..steps) {
            val value = minY + step * i
            val y = height - 100f - h / steps * i
            canvas.drawText(String.format("%.1f", value), 20f, y + 10f, textPaint)
        }
    }

    private fun drawXAxisLabels(canvas: Canvas, minX: Float, maxX: Float) {
        val steps = bars[0].xValues.size
        val w = width - 150f

        for (i in 0 until steps) {
            val x = 100f + w / steps * (i + 0.5f)
            canvas.drawText(i.toString(), x - 10f, height - 50f, textPaint)
        }
    }

    private fun getBounds(): FloatArray {
        val allY = bars.flatMap { it.yValues }
        return floatArrayOf(
            0f,
            bars[0].xValues.size.toFloat(),
            0f,
            allY.maxOrNull() ?: 0f
        )
    }

    private fun mapY(value: Float, min: Float, max: Float): Float {
        return (height - bottomPadding) -
                ((value - min) / (max - min)) *
                (height - topPadding - bottomPadding)
    }


    private fun drawLegend(canvas: Canvas) {
        val startX = width - 250f
        var startY = 80f

        bars.forEach { bar ->
            // ⛔ Skip if label is empty
            if (bar.label.isBlank()) return@forEach

            legendBoxPaint.color = bar.color
            canvas.drawRect(
                startX,
                startY,
                startX + 30f,
                startY + 30f,
                legendBoxPaint
            )

            canvas.drawText(
                bar.label,
                startX + 46f,
                startY + 24f,
                legendTextPaint
            )

            startY += 50f
        }
    }


    // ================= PUBLIC API =================
    fun setBars(data: List<LineData>) {
        bars.clear()
        bars.addAll(data)
        invalidate()
    }

    fun setBar(data: LineData) {
        setBars(listOf(data))
    }

}
