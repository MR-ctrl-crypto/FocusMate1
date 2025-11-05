package com.example.focusmate

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.widget.Button
import android.widget.TextView

class AnalyticsFragment : Fragment() {

    // Declaring UI components
    private lateinit var barChart: BarChart
    private lateinit var btnDaily: Button
    private lateinit var btnWeekly: Button
    private lateinit var btnMonthly: Button
    private lateinit var TimeBreakdown: TextView
    private lateinit var FocusTimer: TextView
    private lateinit var AvgTimer: TextView
    private lateinit var SessionsCompleted: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        // Assigning variables
        barChart = view.findViewById(R.id.barChart)
        btnDaily = view.findViewById(R.id.btnDaily)
        btnWeekly = view.findViewById(R.id.btnWeekly)
        btnMonthly = view.findViewById(R.id.btnMonthly)
        TimeBreakdown = view.findViewById(R.id.label_time_breakdown)
        FocusTimer = view.findViewById(R.id.focus_timer)
        AvgTimer = view.findViewById(R.id.avg_timer)
        SessionsCompleted = view.findViewById(R.id.sessions_completed)

        // the daily chat displays when on analytics screen first
        loadChart("Daily")

        // sets the chart to its respective button when clicked and changes the label
        btnDaily.setOnClickListener {
            loadChart("Daily")
            TimeBreakdown.text = "Time Breakdown - Daily"}
        btnWeekly.setOnClickListener {
            loadChart("Weekly")
            TimeBreakdown.text = "Time Breakdown - Weekly"
        }
        btnMonthly.setOnClickListener {
            loadChart("Monthly")
            TimeBreakdown.text = "Time Breakdown - Monthly"
        }

        return view
    }

    private fun loadChart(period: String) {

        // data samples
        val (values, labels) = when (period) {
            "Daily" -> Pair(
                listOf(60f, 40f, 90f, 80f, 50f, 90f, 20f),
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            )
            "Weekly" -> Pair(
                listOf(350f, 400f, 450f, 500f),
                listOf("Week 1", "Week 2", "Week 3", "Week 4")
            )
            "Monthly" -> Pair(
                listOf(1200f, 1250f, 1300f, 1400f, 2000f, 1500f, 1200f, 1250f, 1300f, 1400f, 2000f, 1500f),
                listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            )
            else -> Pair(emptyList<Float>(), emptyList<String>())
        }

        // Update the "Time Breakdown" label dynamically
        TimeBreakdown.text = "Time Breakdown - $period"

        // Calculate total focus time
        val totalMinutes = values.sum().toInt()

        // Convert totalMinutes to hours and minutes for focus timer
        val totalHours = totalMinutes / 60
        val totalRemainingMinutes = totalMinutes % 60
        FocusTimer.text = if (totalHours > 0) "$totalHours h $totalRemainingMinutes m" else "$totalRemainingMinutes m"

        // Calculate average session length
        val avgMinutes = if (values.isNotEmpty()) totalMinutes / values.size else 0
        val avgHours = avgMinutes / 60
        val avgRemainingMinutes = avgMinutes % 60

        // Displays average session length in "h  m" format
        AvgTimer.text = if (avgHours > 0) "$avgHours h $avgRemainingMinutes m" else "$avgRemainingMinutes m"

        // Sessions completed
        val sessionsCompleted = values.size
        SessionsCompleted.text = sessionsCompleted.toString()

        // data is passed to the showBarChart function
        showBarChart(values, labels)
    }

    private fun showBarChart(values: List<Float>, labels: List<String>) {
        // Creates a list of BarEntry objects for the chart
        val entries = ArrayList<BarEntry>()
        for (i in values.indices) entries.add(BarEntry(i.toFloat(), values[i]))

        // Creates a dataset with the entries
        val dataSet = BarDataSet(entries, "Focus Time (minutes)").apply {
            color = Color.parseColor("black")
            valueTextColor = Color.BLACK
            valueTextSize = 10f
        }

        // Creates BarData object and setting bar width
        val barData = BarData(dataSet)
        barData.barWidth = 0.4f
        barChart.data = barData

        //X-axis of bar graph
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels) // Set labels
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f // One label per bar
        xAxis.textSize = 10f
        xAxis.setDrawGridLines(false)

        // Configure Y-axis
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false

        // Removes unnecessary chart decorations
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false

        // bar animation
        barChart.animateY(1000)

        // Refreshes the chart with new data
        barChart.invalidate()
    }
}