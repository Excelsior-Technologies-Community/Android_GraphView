package com.ext.graphview

import android.graphics.Color

data class LineData(
    val xValues: List<Float>,
    val yValues: List<Float>,
    val color: Int = Color.BLUE,
    val label: String = ""
)

