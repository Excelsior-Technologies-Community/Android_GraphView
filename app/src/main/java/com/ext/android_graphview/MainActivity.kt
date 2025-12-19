package com.ext.android_graphview

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ext.graphview.LineData
import com.ext.graphview.LineGraphView

class MainActivity : AppCompatActivity() {
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

    }
}