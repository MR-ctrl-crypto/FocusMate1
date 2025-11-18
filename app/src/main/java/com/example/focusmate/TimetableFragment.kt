package com.example.focusmate

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

// MODIFIED DATA CLASS FOR FIREBASE
data class Event(
    var id: String = "",
    var timestamp: Long = 0, // Storing date as a Long timestamp
    var name: String = "",
    var location: String = "",
    var type: EventType = EventType.MEETING
)

enum class EventType {
    CLASS, STUDY, MEETING
}

class TimetableFragment : Fragment() {

    // --- FIREBASE PROPERTIES ---
    private val databaseUrl = "https://focusmate-51ac3-default-rtdb.europe-west1.firebasedatabase.app"
    private var eventsRef: DatabaseReference? = null

    // --- UI VIEWS ---
    private lateinit var calendarView: CalendarView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var fabAddEvent: FloatingActionButton
    private lateinit var eventsHeader: TextView
    private lateinit var monthYearText: TextView
    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton

    private lateinit var eventAdapter: EventAdapter
    private var allEvents = mutableListOf<Event>()
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
        initializeViews(view)

        // Setup Firebase reference for the current user
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            eventsRef = FirebaseDatabase.getInstance(databaseUrl).getReference("timetables").child(userId)
        } else {
            Toast.makeText(context, "Error: You must be logged in.", Toast.LENGTH_LONG).show()
            // Optionally, navigate back or disable functionality
            return
        }

        setupRecyclerView()
        setupCalendar()
        setupClickListeners()

        updateMonthYearDisplay()
        loadEventsFromFirebase() // Load data from Firebase
    }

    private fun initializeViews(view: View) {
        calendarView = view.findViewById(R.id.calendar_view)
        eventsRecyclerView = view.findViewById(R.id.recycler_view_events)
        fabAddEvent = view.findViewById(R.id.fab_add_event)
        eventsHeader = view.findViewById(R.id.header_events_on_date)
        monthYearText = view.findViewById(R.id.text_month_year)
        prevMonthButton = view.findViewById(R.id.button_prev_month)
        nextMonthButton = view.findViewById(R.id.button_next_month)
    }

    // --- NEW: Load all events from Firebase and listen for real-time updates ---
    private fun loadEventsFromFirebase() {
        eventsRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allEvents.clear()
                for (eventSnapshot in snapshot.children) {
                    val event = eventSnapshot.getValue(Event::class.java)
                    if (event != null) {
                        allEvents.add(event)
                    }
                }
                allEvents.sortBy { it.timestamp } // Sort by timestamp
                updateEventsForSelectedDate() // Refresh the UI with new data
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load timetable: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
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
            updateMonthYearDisplay()
            updateEventsForSelectedDate()
        }
    }

    private fun setupClickListeners() {
        fabAddEvent.setOnClickListener { showAddEventDialog() }

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
        // ... (find all views in dialogView as before) ...
        val eventNameEditText = dialogView.findViewById<TextInputEditText>(R.id.edit_text_event_name)
        val locationEditText = dialogView.findViewById<TextInputEditText>(R.id.edit_text_event_location)
        val dateButton = dialogView.findViewById<Button>(R.id.button_pick_date)
        val timeButton = dialogView.findViewById<Button>(R.id.button_pick_time)
        val typeRadioGroup = dialogView.findViewById<RadioGroup>(R.id.radio_group_event_type)

        val newEventDateTime = Calendar.getInstance().apply { time = selectedDate.time }

        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        dateButton.text = dateFormat.format(newEventDateTime.time)
        timeButton.text = timeFormat.format(newEventDateTime.time)

        dateButton.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, day ->
                newEventDateTime.set(year, month, day)
                dateButton.text = dateFormat.format(newEventDateTime.time)
            }, newEventDateTime.get(Calendar.YEAR), newEventDateTime.get(Calendar.MONTH), newEventDateTime.get(Calendar.DAY_OF_MONTH)).show()
        }

        timeButton.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                newEventDateTime.set(Calendar.HOUR_OF_DAY, hour)
                newEventDateTime.set(Calendar.MINUTE, minute)
                timeButton.text = timeFormat.format(newEventDateTime.time)
            }, newEventDateTime.get(Calendar.HOUR_OF_DAY), newEventDateTime.get(Calendar.MINUTE), false).show()
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = eventNameEditText.text.toString()
                val location = locationEditText.text.toString()
                val type = when (typeRadioGroup.checkedRadioButtonId) {
                    R.id.radio_class -> EventType.CLASS
                    R.id.radio_study -> EventType.STUDY
                    else -> EventType.MEETING
                }

                if (name.isBlank()) {
                    Toast.makeText(context, "Event name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Call the function to save the event to Firebase
                addNewEventToFirebase(name, location, newEventDateTime.timeInMillis, type)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    // --- MODIFIED: Save the new event directly to Firebase ---
    private fun addNewEventToFirebase(name: String, location: String, timestamp: Long, type: EventType) {
        // Generate a unique key for the new event
        val eventId = eventsRef?.push()?.key ?: return

        val newEvent = Event(
            id = eventId,
            name = name,
            location = location,
            timestamp = timestamp,
            type = type
        )

        // Save the event to Firebase under the generated key
        eventsRef?.child(eventId)?.setValue(newEvent)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Event saved!", Toast.LENGTH_SHORT).show()
                // The ValueEventListener will automatically handle updating the UI
            } else {
                Toast.makeText(context, "Failed to save event: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEventsForSelectedDate() {
        val headerFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        eventsHeader.text = "Events on ${headerFormat.format(selectedDate.time)}"

        val eventsOnDate = allEvents.filter {
            val eventCal = Calendar.getInstance().apply { timeInMillis = it.timestamp } // Use timestamp
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
