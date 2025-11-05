package com.example.focusmate

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

    private lateinit var totalFocusTimeText: TextView
    private lateinit var focusStreakText: TextView
    private lateinit var motivationalQuoteText: TextView
    private lateinit var pieChart: PieChart
    private lateinit var startSessionButton: Button
    private lateinit var viewTimetableCard: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    // in HomeFragment.kt
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize all the views from the layout
        totalFocusTimeText = view.findViewById(R.id.text_total_focus_time)
        focusStreakText = view.findViewById(R.id.text_focus_streak)
        motivationalQuoteText = view.findViewById(R.id.text_motivational_quote)
        pieChart = view.findViewById(R.id.pie_chart_screen_time)
        startSessionButton = view.findViewById(R.id.button_start_new_session)
        viewTimetableCard = view.findViewById(R.id.card_view_timetable) // Correct ID

        // Call the functions to populate the UI with data
        loadFocusData()
        setupPieChart()
        loadPieChartData()
        loadMotivationalQuote()

        // Setup button click listeners for navigation
        setupClickListeners()
    }


    private fun loadFocusData() {
        // TODO: Replace with actual logic to fetch data from SharedPreferences or a database
        val totalMinutes = 95 // Example: 1h 35m
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        totalFocusTimeText.text = "${hours}h ${minutes}m"

        val streakDays = 5
        focusStreakText.text = "$streakDays Days"
    }

    private fun setupPieChart() {
        pieChart.isDrawHoleEnabled = true // Creates the donut chart style
        pieChart.holeRadius = 75f
        pieChart.setHoleColor(Color.TRANSPARENT)

        // Disable all extra chart elements for a minimal look
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.setDrawEntryLabels(false)
        pieChart.isRotationEnabled = false
        pieChart.isHighlightPerTapEnabled = false
    }

    private fun loadPieChartData() {
        // TODO: Replace with live data from screen time statistics
        val focusedPercentage = 65f
        val distractedPercentage = 35f

        val entries = ArrayList<PieEntry>().apply {
            add(PieEntry(focusedPercentage, "Focused"))
            add(PieEntry(distractedPercentage, "Distracted"))
        }

        // Get the primary color from the current theme attribute
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data

        val colors = ArrayList<Int>().apply {
            // Use the theme color we just resolved
            add(primaryColor) // Blue accent from your theme
            add(Color.parseColor("#E0E0E0")) // Light gray
        }

        val dataSet = PieDataSet(entries, "Screen Time")
        dataSet.colors = colors
        dataSet.setDrawValues(false) // Hide the percentage values on the chart slices

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate() // Refresh the chart
    }

    private fun loadMotivationalQuote() {
        val quotes = listOf(
            "\"The secret of getting ahead is getting started.\"",
            "\"The best way to predict the future is to create it.\"",
            "\"Don't watch the clock; do what it does. Keep going.\"",
            "\"Focus on being productive instead of busy.\"",
            "\"The key is not to prioritize what's on your schedule, but to schedule your priorities.\""
        )
        motivationalQuoteText.text = quotes.random()
    }

    private fun setupClickListeners() {
        startSessionButton.setOnClickListener {
            // Navigate to the TimerFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, TimerFragment())
                .addToBackStack(null)
                .commit()
        }

        viewTimetableCard.setOnClickListener {
            // Navigate to the TimetableFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, TimetableFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
