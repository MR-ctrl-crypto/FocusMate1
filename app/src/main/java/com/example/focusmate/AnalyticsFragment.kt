package com.example.focusmate

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class AnalyticsFragment : Fragment() {

    // UI components
    private lateinit var barChart: BarChart
    private lateinit var btnDaily: Button
    private lateinit var btnWeekly: Button
    private lateinit var btnMonthly: Button
    private lateinit var timeBreakdown: TextView
    private lateinit var focusTimer: TextView
    private lateinit var avgTimer: TextView
    private lateinit var sessionsCompleted: TextView

    private var currentPeriod = "Daily"
    private var allSessions = listOf<FocusSession>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        // Assigning variables to UI elements from the layout
        barChart = view.findViewById(R.id.barChart)
        btnDaily = view.findViewById(R.id.btnDaily)
        btnWeekly = view.findViewById(R.id.btnWeekly)
        btnMonthly = view.findViewById(R.id.btnMonthly)
        timeBreakdown = view.findViewById(R.id.label_time_breakdown)
        focusTimer = view.findViewById(R.id.focus_timer)
        avgTimer = view.findViewById(R.id.avg_timer)
        sessionsCompleted = view.findViewById(R.id.sessions_completed)

        setupButtonListeners()
        return view
    }

    override fun onResume() {
        super.onResume()
        // Every time the fragment is shown, fetch the latest data from Firebase
        loadDataFromFirebase()
    }

    private fun setupButtonListeners() {
        btnDaily.setOnClickListener {
            currentPeriod = "Daily"
            processDataForPeriod() // Process the already fetched data
        }
        btnWeekly.setOnClickListener {
            currentPeriod = "Weekly"
            processDataForPeriod()
        }
        btnMonthly.setOnClickListener {
            currentPeriod = "Monthly"
            processDataForPeriod()
        }
    }

    private fun loadDataFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "Please log in to see analytics.", Toast.LENGTH_SHORT).show()
            // Clear previous data if user logs out
            allSessions = emptyList()
            processDataForPeriod()
            return
        }

        // --- THIS IS THE CRITICAL FIX ---
        // Specify the correct database URL when getting the instance.
        val databaseUrl = "https://focusmate-51ac3-default-rtdb.europe-west1.firebasedatabase.app"
        val databaseRef = FirebaseDatabase.getInstance(databaseUrl).getReference("sessions").child(userId)
        // --------------------------------

        // Attach a listener to fetch the data once
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessionsList = mutableListOf<FocusSession>()
                snapshot.children.forEach { data ->
                    val session = data.getValue(FocusSession::class.java)
                    if (session != null) {
                        sessionsList.add(session)
                    }
                }
                allSessions = sessionsList
                // After fetching, process the data for the currently selected period
                processDataForPeriod()
            }

            override fun onCancelled(error: DatabaseError) {
                // Show a detailed error message to help with debugging
                Toast.makeText(context, "Failed to load analytics: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun processDataForPeriod() {
        val (values, labels) = when (currentPeriod) {
            "Daily" -> processDailyData(allSessions)
            "Weekly" -> processWeeklyData(allSessions)
            "Monthly" -> processMonthlyData(allSessions)
            else -> Pair(emptyList(), emptyList())
        }

        updateSummary(values)
        showBarChart(values, labels)
        timeBreakdown.text = "Time Breakdown - $currentPeriod"
    }

    private fun updateSummary(values: List<Float>) {
        val totalMinutes = values.sum().toInt()
        val totalHours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60
        focusTimer.text = if (totalHours > 0) "${totalHours}h ${remainingMinutes}m" else "${totalMinutes}m"

        // Filter out days/weeks/months with 0 minutes to calculate a meaningful average
        val validPeriods = values.filter { it > 0 }
        val avgMinutes = if (validPeriods.isNotEmpty()) totalMinutes / validPeriods.size else 0
        val avgHours = avgMinutes / 60
        val avgRemainingMinutes = avgMinutes % 60
        avgTimer.text = if (avgHours > 0) "${avgHours}h ${avgRemainingMinutes}m" else "${avgMinutes}m"

        // Count only the periods where focus time was logged
        sessionsCompleted.text = validPeriods.size.toString()
    }

    private fun showBarChart(values: List<Float>, labels: List<String>) {
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }

        val dataSet = BarDataSet(entries, "Focus Time (minutes)").apply {
            color = Color.parseColor("#333333") // Using a dark grey color
            valueTextColor = Color.BLACK
            valueTextSize = 10f
            setDrawValues(false) // Hiding values on top of bars for a cleaner look
        }

        barChart.data = BarData(dataSet).apply { barWidth = 0.4f }

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            textColor = Color.BLACK
        }

        barChart.axisLeft.apply {
            axisMinimum = 0f
            textColor = Color.BLACK
        }
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }

    // --- Data Processing Functions ---

    private fun processDailyData(sessions: List<FocusSession>): Pair<List<Float>, List<String>> {
        val dailyData = FloatArray(7) { 0f }
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.clear(Calendar.MINUTE)
        cal.clear(Calendar.SECOND)
        cal.clear(Calendar.MILLISECOND)
        val startOfWeekMs = cal.timeInMillis

        sessions.filter { it.endTimestamp >= startOfWeekMs }.forEach { session ->
            cal.timeInMillis = session.endTimestamp
            val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0, Sunday = 6
            if (dayOfWeek in 0..6) {
                dailyData[dayOfWeek] += session.durationMinutes
            }
        }
        return Pair(dailyData.toList(), labels)
    }

    private fun processWeeklyData(sessions: List<FocusSession>): Pair<List<Float>, List<String>> {
        val weeklyData = FloatArray(4) { 0f }
        val labels = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)

        for (i in 3 downTo 0) {
            val week = currentWeek - i
            labels.add("Wk $week")
            sessions.forEach { session ->
                cal.timeInMillis = session.endTimestamp
                if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.WEEK_OF_YEAR) == week) {
                    weeklyData[3 - i] += session.durationMinutes
                }
            }
        }
        return Pair(weeklyData.toList(), labels)
    }

    private fun processMonthlyData(sessions: List<FocusSession>): Pair<List<Float>, List<String>> {
        val monthlyData = FloatArray(12) { 0f }
        val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)

        sessions.filter {
            cal.timeInMillis = it.endTimestamp
            cal.get(Calendar.YEAR) == currentYear
        }.forEach { session ->
            cal.timeInMillis = session.endTimestamp
            val month = cal.get(Calendar.MONTH) // Jan = 0, Dec = 11
            monthlyData[month] += session.durationMinutes
        }
        return Pair(monthlyData.toList(), labels)
    }
}
