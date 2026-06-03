package com.example.bfit

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class WeightMarkerView(context: Context) : MarkerView(context, R.layout.chart_marker_weight) {

    private val markerText: TextView = findViewById(R.id.markerText)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            markerText.text = String.format("%.1f kg", e.y)
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
