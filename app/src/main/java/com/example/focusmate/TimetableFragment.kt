package com.example.focusmate

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

// Data class and Enum remain the same
data class Event(
    val id: String,
    val date: Date,
    val name: String,
    val location: String,
    val type: EventType
)

enum class EventType {
    CLASS, STUDY, MEETING
}

class TimetableFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var fabAddEvent: FloatingActionButton
    private lateinit var eventsHeader: TextView
    private lateinit var monthYearText: TextView
    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton

    private lateinit var eventAdapter: EventAdapter
    private var allEvents = mutableListOf<Event>() // Start with an empty list for user data
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timetable, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        calendarView = view.findViewById(R.id.calendar_view)
        eventsRecyclerView = view.findViewById(R.id.recycler_view_events)
        fabAddEvent = view.findViewById(R.id.fab_add_event)
        eventsHeader = view.findViewById(R.id.header_events_on_date)
        monthYearText = view.findViewById(R.id.text_month_year)
        prevMonthButton = view.findViewById(R.id.button_prev_month)
        nextMonthButton = view.findViewById(R.id.button_next_month)

        setupRecyclerView()
        setupCalendar()
        setupClickListeners()

        updateMonthYearDisplay()
        updateEventsForSelectedDate()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(listOf())
        eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventAdapter
        }
    }

    private fun setupCalendar() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
            updateMonthYearDisplay() // Also update the month display if the user selects a date in a different month
            updateEventsForSelectedDate()
        }
    }

    private fun setupClickListeners() {
        fabAddEvent.setOnClickListener {
            showAddEventDialog() // Show the dialog to add an event
        }

        prevMonthButton.setOnClickListener {
            selectedDate.add(Calendar.MONTH, -1)
            calendarView.date = selectedDate.timeInMillis
            updateMonthYearDisplay()
        }

        nextMonthButton.setOnClickListener {
            selectedDate.add(Calendar.MONTH, 1)
            calendarView.date = selectedDate.timeInMillis
            updateMonthYearDisplay()
        }
    }

    private fun showAddEventDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_event, null)
        val eventNameEditText = dialogView.findViewById<TextInputEditText>(R.id.edit_text_event_name)
        val locationEditText = dialogView.findViewById<TextInputEditText>(R.id.edit_text_event_location)
        val dateButton = dialogView.findViewById<Button>(R.id.button_pick_date)
        val timeButton = dialogView.findViewById<Button>(R.id.button_pick_time)
        val typeRadioGroup = dialogView.findViewById<RadioGroup>(R.id.radio_group_event_type)

        // Calendar to hold the selected date and time for the new event
        val newEventDateTime = Calendar.getInstance()
        newEventDateTime.time = selectedDate.time // Pre-fill with the date selected on the calendar

        // Update button texts initially
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        dateButton.text = dateFormat.format(newEventDateTime.time)
        timeButton.text = timeFormat.format(newEventDateTime.time)

        // Date Picker Logic
        dateButton.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, day ->
                newEventDateTime.set(Calendar.YEAR, year)
                newEventDateTime.set(Calendar.MONTH, month)
                newEventDateTime.set(Calendar.DAY_OF_MONTH, day)
                dateButton.text = dateFormat.format(newEventDateTime.time)
            }, newEventDateTime.get(Calendar.YEAR), newEventDateTime.get(Calendar.MONTH), newEventDateTime.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Time Picker Logic
        timeButton.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                newEventDateTime.set(Calendar.HOUR_OF_DAY, hour)
                newEventDateTime.set(Calendar.MINUTE, minute)
                timeButton.text = timeFormat.format(newEventDateTime.time)
            }, newEventDateTime.get(Calendar.HOUR_OF_DAY), newEventDateTime.get(Calendar.MINUTE), false).show()
        }

        // Build and show the dialog
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val name = eventNameEditText.text.toString()
                val location = locationEditText.text.toString()
                val selectedTypeId = typeRadioGroup.checkedRadioButtonId

                if (name.isBlank()) {
                    Toast.makeText(context, "Event name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val type = when (selectedTypeId) {
                    R.id.radio_class -> EventType.CLASS
                    R.id.radio_study -> EventType.STUDY
                    else -> EventType.MEETING
                }

                addNewEvent(name, location, newEventDateTime.time, type)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }

    private fun addNewEvent(name: String, location: String, date: Date, type: EventType) {
        val newEvent = Event(
            id = UUID.randomUUID().toString(), // Generate a unique ID
            name = name,
            location = location,
            date = date,
            type = type
        )
        allEvents.add(newEvent)
        // Sort all events by date to keep the list chronological
        allEvents.sortBy { it.date }

        // Refresh the calendar view to show the newly selected date
        selectedDate.time = date
        calendarView.date = selectedDate.timeInMillis
        updateEventsForSelectedDate()
    }

    private fun updateEventsForSelectedDate() {
        val headerFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        eventsHeader.text = "Events on ${headerFormat.format(selectedDate.time)}"

        val eventsOnDate = allEvents.filter {
            val eventCal = Calendar.getInstance().apply { time = it.date }
            eventCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    eventCal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
        }
        eventAdapter.updateEvents(eventsOnDate)
    }

    private fun updateMonthYearDisplay() {
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearText.text = monthYearFormat.format(selectedDate.time)
    }
}
