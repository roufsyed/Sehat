package com.rouf.saht.meditation

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rouf.saht.R
import com.rouf.saht.common.model.Sound
import com.rouf.saht.databinding.ItemSoundBinding

class MeditationSoundAdapter(
    private val soundList: List<Sound>,
    private val onItemClick: (Sound) -> Unit
) : RecyclerView.Adapter<MeditationSoundAdapter.SoundViewHolder>() {

    private var currentPlayingFile: String? = null

    /**
     * Updates the currently playing sound and refreshes only the affected items,
     * avoiding a full notifyDataSetChanged.
     */
    fun updatePlayingSound(file: String?) {
        val previousFile = currentPlayingFile
        currentPlayingFile = file

        previousFile?.let { old ->
            soundList.indexOfFirst { it.file == old }.takeIf { it >= 0 }?.let { notifyItemChanged(it) }
        }
        file?.let { new ->
            soundList.indexOfFirst { it.file == new }.takeIf { it >= 0 }?.let { notifyItemChanged(it) }
        }
    }

    inner class SoundViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemSoundBinding.bind(view)

        fun bind(sound: Sound) {
            binding.tvSoundName.text = sound.name
            binding.rlSoundCard.setBackgroundColor(Color.parseColor(sound.backgroundColor))

            val isPlaying = sound.file == currentPlayingFile
            binding.ivPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

            binding.root.setOnClickListener { onItemClick(sound) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sound, parent, false)
        return SoundViewHolder(view)
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        holder.bind(soundList[position])
    }

    override fun getItemCount(): Int = soundList.size
}
