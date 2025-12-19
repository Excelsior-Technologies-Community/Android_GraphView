package com.ext.graphview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ScaleGestureDetector
import kotlin.math.max
import kotlin.math.min


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
    // Zoom & pan
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f

    private val minScale = 1f
    private val maxScale = 3f

    private var lastTouchX = 0f
    private var lastTouchY = 0f



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

    private val scaleDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
                invalidate()
                return true
            }
        })



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

            typedArray.recycle()
        }
    }

    // ================= DRAW =================
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()

        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)

        // 1️⃣ Grid (background)
        if (showGrid) {
            drawGrid(canvas)
        }

        // 2️⃣ Axes
        drawAxes(canvas)

        if (lines.isEmpty()) return

        val (minX, maxX, minY, maxY) = getBounds()


        // 3️⃣ Axis labels
        drawYAxisLabels(canvas, minY, maxY)
        drawXAxisLabels(canvas, minX, maxX)

        // 4️⃣ Graph line
        lines.forEach { line ->
            linePaint.color = line.color
            drawAnimatedLine(
                canvas,
                line.xValues,
                line.yValues,
                minX,
                maxX,
                minY,
                maxY
            )
        }


        // 5️⃣ TOUCH TOOLTIP (ADD HERE 👇)
        touchedPoint?.let { (lineIndex, pointIndex) ->
            val line = lines[lineIndex]

            val x = mapX(line.xValues[pointIndex], minX, maxX)
            val y = mapY(line.yValues[pointIndex], minY, maxY)

            linePaint.color = line.color
            canvas.drawCircle(x, y, 10f, linePaint)

            val text = "(${line.xValues[pointIndex]}, ${line.yValues[pointIndex]})"
            val padding = 12f
            val textWidth = tooltipPaint.measureText(text)

            canvas.drawRoundRect(
                x - textWidth / 2 - padding,
                y - 80f,
                x + textWidth / 2 + padding,
                y - 40f,
                12f,
                12f,
                tooltipBgPaint
            )

            canvas.drawText(
                text,
                x - textWidth / 2,
                y - 50f,
                tooltipPaint
            )
        }
        canvas.restore()
    }


    // ================= AXES =================
    private fun drawAxes(canvas: Canvas) {
        // X axis
        canvas.drawLine(
            100f,
            height - 100f,
            width - 50f,
            height - 100f,
            axisPaint
        )

        // Y axis
        canvas.drawLine(
            100f,
            50f,
            100f,
            height - 100f,
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
        return 100f + ((value - min) / (max - min)) * (width - 150f)
    }

    private fun mapY(value: Float, min: Float, max: Float): Float {
        return (height - 100f) -
                ((value - min) / (max - min)) * (height - 150f)
    }

    private fun drawGrid(canvas: Canvas) {
        val steps = 5
        val graphWidth = width - 150f
        val graphHeight = height - 150f

        // Horizontal grid
        for (i in 0..steps) {
            val y = 50f + (graphHeight / steps) * i
            canvas.drawLine(100f, y, width - 50f, y, gridPaint)
        }

        // Vertical grid
        for (i in 0..steps) {
            val x = 100f + (graphWidth / steps) * i
            canvas.drawLine(x, 50f, x, height - 100f, gridPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 1️⃣ Handle zoom
        scaleDetector.onTouchEvent(event)

        if (lines.isEmpty()) return false

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y

                // Tooltip touch
                touchedPoint = findClosestPoint(event.x)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector.isInProgress) {
                    // During pinch zoom → don't pan or tooltip update
                    return true
                }

                // 2️⃣ Pan
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                translateX += dx
                translateY += dy

                lastTouchX = event.x
                lastTouchY = event.y

                // 3️⃣ Tooltip update
                touchedPoint = findClosestPoint(event.x)
                invalidate()
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                touchedPoint = null
                performClick()
                invalidate()
            }
        }
        return true
    }


    private fun findClosestPoint(touchX: Float): Pair<Int, Int> {
        val (minX, maxX, _, _) = getBounds()

        var closestLine = 0
        var closestIndex = 0
        var minDistance = Float.MAX_VALUE

        lines.forEachIndexed { lineIndex, line ->
            line.xValues.forEachIndexed { pointIndex, xValue ->
                val x = mapX(xValue, minX, maxX)
                val distance = kotlin.math.abs(touchX - x)

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



}
