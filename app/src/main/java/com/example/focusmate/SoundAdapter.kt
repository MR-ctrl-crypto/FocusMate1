package com.example.focusmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Data class to hold our sound information
data class Sound(val name: String, val resourceId: Int, val iconResId: Int)

class SoundAdapter(
    private val sounds: List<Sound>,
    private val onSoundClicked: (Sound) -> Unit
) : RecyclerView.Adapter<SoundAdapter.SoundViewHolder>() {

    inner class SoundViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.sound_icon)
        val name: TextView = itemView.findViewById(R.id.sound_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sound, parent, false)
        return SoundViewHolder(view)
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        val sound = sounds[position]
        holder.name.text = sound.name
        holder.icon.setImageResource(sound.iconResId)
        holder.itemView.setOnClickListener {
            onSoundClicked(sound)
        }
    }

    override fun getItemCount(): Int = sounds.size
}
