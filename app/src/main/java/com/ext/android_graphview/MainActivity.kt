package com.ext.android_graphview

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ext.graphview.BarGraphView
import com.ext.graphview.LineData
import com.ext.graphview.LineGraphView

class MainActivity : AppCompatActivity() {
    private lateinit var barGraph: BarGraphView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val graph = findViewById<LineGraphView>(R.id.graph)
        graph.setLines(
            listOf(
                LineData(
                    xValues = listOf(0f, 1f, 2f, 3f),
                    yValues = listOf(10f, 40f, 20f, 60f),
                    color = Color.RED,
                    label = "Revenue"
                ),
                LineData(
                    xValues = listOf(0f, 1f, 2f, 3f),
                    yValues = listOf(20f, 30f, 50f, 40f),
                    color = Color.BLUE,
                    label = "Expenses"
                )
            )
        )

        barGraph = findViewById(R.id.bargraph)

        barGraph.setBars(
            listOf(
                LineData(
                    xValues = listOf(0f, 1f, 2f, 3f),
                    yValues = listOf(50f, 70f, 40f, 90f),
                    color = Color.YELLOW
                ),
                LineData(
                    xValues = listOf(0f, 1f, 2f, 3f),
                    yValues = listOf(30f, 60f, 80f, 50f),
                    color = Color.BLUE
                )
            )
        )

        val lineGraph = findViewById<LineGraphView>(R.id.linegraph)

        lineGraph.setLine(
            LineData(
                xValues = listOf(0f, 1f, 2f, 3f),
                yValues = listOf(20f, 30f, 50f, 40f),
            )
        )

        val bargraph1 = findViewById<BarGraphView>(R.id.bargraph1)

        bargraph1.setBar(
            LineData(
                xValues = listOf(0f, 1f, 2f),
                yValues = listOf(50f, 70f, 40f)
            )
        )



    }
}