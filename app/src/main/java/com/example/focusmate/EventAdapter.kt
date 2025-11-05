package com.example.focusmate

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class EventAdapter(
    private var events: List<Event>
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    // Describes an item view and metadata about its place within the RecyclerView.
    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val time: TextView = itemView.findViewById(R.id.event_time)
        val name: TextView = itemView.findViewById(R.id.event_name)
        val location: TextView = itemView.findViewById(R.id.event_location)
        val tag: TextView = itemView.findViewById(R.id.event_tag)
    }

    // Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_card, parent, false)
        return EventViewHolder(view)
    }

    // Returns the total number of items in the data set held by the adapter.
    override fun getItemCount(): Int = events.size

    // Called by RecyclerView to display the data at the specified position.
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        val context = holder.itemView.context

        // Set the data for each view
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.time.text = timeFormat.format(event.date)
        holder.name.text = event.name
        holder.location.text = event.location

        // Set the tag text and background color based on the event type
        when (event.type) {
            EventType.CLASS -> {
                holder.tag.text = "Class"
                holder.tag.background = ContextCompat.getDrawable(context, R.drawable.pill_background_green)
            }
            EventType.STUDY -> {
                holder.tag.text = "Study"
                holder.tag.background = ContextCompat.getDrawable(context, R.drawable.pill_background_blue)
            }
            EventType.MEETING -> {
                holder.tag.text = "Meeting"
                holder.tag.background = ContextCompat.getDrawable(context, R.drawable.pill_background_teal)
            }
        }
    }

    // Public function to update the list of events from the fragment
    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged() // Reload the list
    }
}
