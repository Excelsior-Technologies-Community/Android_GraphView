package com.ext.graphview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


class LineGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ================= DATA =================
    private val lines = mutableListOf<LineData>()
    private var animationProgress = 1f
    private var showGrid = false
    private var touchedPoint: Pair<Int, Int>? = null
    // Pair<lineIndex, pointIndex>
    private var showLegend = true
    private var activeLineIndex: Int? = null
    private var defaultLineColor: Int = Color.BLUE
    private val leftPadding = 100f
    private val rightPadding = 80f   // increased
    private val topPadding = 80f     // increased
    private val bottomPadding = 120f // increased (labels + tooltip)




    // ================= PAINTS =================
    private val axisPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 28f
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
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



    // ================= XML ATTRIBUTES =================
    init {
        attrs?.let {
            val typedArray =
                context.obtainStyledAttributes(it, R.styleable.LineGraphView)

            linePaint.color = typedArray.getColor(
                R.styleable.LineGraphView_lineColor,
                Color.BLUE
            )

            axisPaint.color = typedArray.getColor(
                R.styleable.LineGraphView_axisColor,
                Color.BLACK
            )

            textPaint.color = typedArray.getColor(
                R.styleable.LineGraphView_textColor,
                Color.BLACK
            )

            showGrid = typedArray.getBoolean(
                R.styleable.LineGraphView_showGrid,
                false
            )

            showLegend = typedArray.getBoolean(
                R.styleable.LineGraphView_showLegend,
                true
            )

            defaultLineColor = typedArray.getColor(
                R.styleable.LineGraphView_lineColor,
                Color.BLUE
            )



            typedArray.recycle()
        }
    }

    // ================= DRAW =================
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1️⃣ Grid (background)
        if (showGrid) {
            drawGrid(canvas)
        }

        // 2️⃣ Axes
        drawAxes(canvas)

        if (lines.isEmpty()) return

        // Global bounds for all lines
        val (minX, maxX, minY, maxY) = getBounds()

        // 3️⃣ Axis labels
        drawYAxisLabels(canvas, minY, maxY)
        drawXAxisLabels(canvas, minX, maxX)

        // 4️⃣ Draw lines + points
        lines.forEachIndexed { index, line ->
            // Draw line
            linePaint.color =
                if (line.color != Color.TRANSPARENT) line.color
                else defaultLineColor

            drawAnimatedLine(
                canvas,
                line.xValues,
                line.yValues,
                minX,
                maxX,
                minY,
                maxY
            )

            // Draw points (filtered by activeLineIndex inside drawPoints)
            drawPoints(
                canvas,
                line,
                index,
                minX,
                maxX,
                minY,
                maxY
            )
        }

        // 5️⃣ Tooltip for touched point
        touchedPoint?.let { (lineIndex, pointIndex) ->
            val line = lines[lineIndex]

            val x = mapX(line.xValues[pointIndex], minX, maxX)
            val y = mapY(line.yValues[pointIndex], minY, maxY)

            // Highlight point
            linePaint.color =
                if (line.color != Color.TRANSPARENT) line.color
                else defaultLineColor

            canvas.drawCircle(x, y, 10f, linePaint)

            // Tooltip text
            val text = "(${line.xValues[pointIndex]}, ${line.yValues[pointIndex]})"
            val padding = 12f
            val textWidth = tooltipPaint.measureText(text)

            // Tooltip background
            val tooltipLeft = (x - textWidth / 2 - padding)
                .coerceAtLeast(leftPadding)

            val tooltipRight = (x + textWidth / 2 + padding)
                .coerceAtMost(width - rightPadding)

            val tooltipTop = (y - 80f)
                .coerceAtLeast(topPadding)

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


            // Tooltip text
            canvas.drawText(
                text,
                x - textWidth / 2,
                y - 50f,
                tooltipPaint
            )
        }

        // 6️⃣ Legend (top layer)
        if (showLegend && lines.isNotEmpty()) {
            drawLegend(canvas)
        }
    }



    // ================= AXES =================
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

    // ================= LINE DRAW =================
    private fun drawAnimatedLine(
        canvas: Canvas,
        xVals: List<Float>,
        yVals: List<Float>,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float
    ) {
        val maxIndex = ((xVals.size - 1) * animationProgress).toInt()
            .coerceAtLeast(0)

        for (i in 0 until maxIndex) {
            val startX = mapX(xVals[i], minX, maxX)
            val startY = mapY(yVals[i], minY, maxY)

            val endX = mapX(xVals[i + 1], minX, maxX)
            val endY = mapY(yVals[i + 1], minY, maxY)

            canvas.drawLine(startX, startY, endX, endY, linePaint)
        }
    }


    // ================= LABELS =================
    private fun drawYAxisLabels(canvas: Canvas, minY: Float, maxY: Float) {
        val steps = 5
        val stepValue = (maxY - minY) / steps
        val stepHeight = (height - 150f) / steps

        for (i in 0..steps) {
            val value = minY + (stepValue * i)
            val y = (height - 100f) - (stepHeight * i)

            canvas.drawText(
                String.format("%.1f", value),
                20f,
                y + 10f,
                textPaint
            )
        }
    }

    private fun drawXAxisLabels(canvas: Canvas, minX: Float, maxX: Float) {
        val steps = 6
        val step = (maxX - minX) / (steps - 1)

        for (i in 0 until steps) {
            val value = minX + step * i
            val x = mapX(value, minX, maxX)

            canvas.drawText(
                String.format("%.1f", value),
                x - 15f,
                height - 50f,
                textPaint
            )
        }
    }


    // ================= PUBLIC API =================
    fun setLines(data: List<LineData>) {
        lines.clear()
        lines.addAll(data)
        startAnimation()
    }

    fun setLine(data: LineData) {
        setLines(listOf(data))
    }



    // ================= ANIMATION =================
    private fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 1000
        animator.addUpdateListener {
            animationProgress = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    // ================= MAPPING =================
    private fun mapX(value: Float, min: Float, max: Float): Float {
        return leftPadding +
                ((value - min) / (max - min)) *
                (width - leftPadding - rightPadding)
    }

    private fun mapY(value: Float, min: Float, max: Float): Float {
        return (height - bottomPadding) -
                ((value - min) / (max - min)) *
                (height - topPadding - bottomPadding)
    }



    private fun drawGrid(canvas: Canvas) {
        val steps = 5
        val graphWidth = width - leftPadding - rightPadding
        val graphHeight = height - topPadding - bottomPadding

        // Horizontal grid lines
        for (i in 0..steps) {
            val y = topPadding + (graphHeight / steps) * i
            canvas.drawLine(
                leftPadding,
                y,
                width - rightPadding,
                y,
                gridPaint
            )
        }

        // Vertical grid lines
        for (i in 0..steps) {
            val x = leftPadding + (graphWidth / steps) * i
            canvas.drawLine(
                x,
                topPadding,
                x,
                height - bottomPadding,
                gridPaint
            )
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (lines.isEmpty()) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val point = findClosestPoint(event.x, event.y)
                touchedPoint = point
                activeLineIndex = point.first
                invalidate()
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                touchedPoint = null
                activeLineIndex = null
                performClick()
                invalidate()
            }
        }
        return true
    }

    private fun drawPoints(
        canvas: Canvas,
        line: LineData,
        lineIndex: Int,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float
    ) {
        if (activeLineIndex != null && activeLineIndex != lineIndex) return

        linePaint.color = line.color

        line.xValues.forEachIndexed { index, xVal ->
            val x = mapX(xVal, minX, maxX)
            val y = mapY(line.yValues[index], minY, maxY)
            canvas.drawCircle(x, y, 6f, linePaint)
        }
    }





    private fun findClosestPoint(
        touchX: Float,
        touchY: Float
    ): Pair<Int, Int> {

        val (minX, maxX, minY, maxY) = getBounds()

        var closestLine = 0
        var closestIndex = 0
        var minDistance = Float.MAX_VALUE

        lines.forEachIndexed { lineIndex, line ->
            line.xValues.forEachIndexed { pointIndex, xValue ->
                val px = mapX(xValue, minX, maxX)
                val py = mapY(line.yValues[pointIndex], minY, maxY)

                // Euclidean distance (2D)
                val dx = touchX - px
                val dy = touchY - py
                val distance = dx * dx + dy * dy   // squared distance (faster)

                if (distance < minDistance) {
                    minDistance = distance
                    closestLine = lineIndex
                    closestIndex = pointIndex
                }
            }
        }
        return Pair(closestLine, closestIndex)
    }



    private fun getBounds(): FloatArray {
        val allX = lines.flatMap { it.xValues }
        val allY = lines.flatMap { it.yValues }

        return floatArrayOf(
            allX.minOrNull()!!,
            allX.maxOrNull()!!,
            allY.minOrNull()!!,
            allY.maxOrNull()!!
        )
    }

    private fun drawLegend(canvas: Canvas) {
        val startX = width - 250f
        var startY = 80f

        val boxSize = 30f
        val spacing = 20f

        lines.forEach { line ->
            // ⛔ Skip legend item if label is empty or blank
            if (line.label.isBlank()) return@forEach

            // Color box
            legendBoxPaint.color = line.color
            canvas.drawRect(
                startX,
                startY,
                startX + boxSize,
                startY + boxSize,
                legendBoxPaint
            )

            // Label text
            canvas.drawText(
                line.label,
                startX + boxSize + 16f,
                startY + boxSize - 6f,
                legendTextPaint
            )

            startY += boxSize + spacing
        }
    }




}
