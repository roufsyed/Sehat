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
    private var soundList: List<Sound>,
    private val onSoundClick: (Sound) -> Unit,
    private val onAddClick: () -> Unit,
    private val onDeleteCustomSound: (Sound) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var currentPlayingFile: String? = null

    companion object {
        private const val VIEW_TYPE_SOUND = 0
        private const val VIEW_TYPE_ADD   = 1
        private const val ADD_CARD_COLOR  = "#546E7A" // blue-grey
    }

    fun updateSounds(sounds: List<Sound>) {
        soundList = sounds
        notifyDataSetChanged()
    }

    /**
     * Updates the currently playing sound, refreshing only the two affected items
     * (the one that stopped and the one that started).
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

    override fun getItemViewType(position: Int): Int =
        if (position < soundList.size) VIEW_TYPE_SOUND else VIEW_TYPE_ADD

    override fun getItemCount(): Int = soundList.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sound, parent, false)
        return if (viewType == VIEW_TYPE_ADD) AddSoundViewHolder(view) else SoundViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SoundViewHolder  -> holder.bind(soundList[position])
            is AddSoundViewHolder -> holder.bind()
        }
    }

    inner class SoundViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemSoundBinding.bind(view)

        fun bind(sound: Sound) {
            binding.tvSoundName.text = sound.name
            binding.rlSoundCard.setBackgroundColor(Color.parseColor(sound.backgroundColor))

            val isPlaying = sound.file == currentPlayingFile
            binding.ivPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

            binding.root.setOnClickListener { onSoundClick(sound) }

            // Long-press on custom sounds (identified by content:// URI) offers deletion
            if (sound.file.startsWith("content://")) {
                binding.root.setOnLongClickListener {
                    onDeleteCustomSound(sound)
                    true
                }
            } else {
                binding.root.setOnLongClickListener(null)
            }
        }
    }

    inner class AddSoundViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemSoundBinding.bind(view)

        fun bind() {
            binding.tvSoundName.text = "Add Sound"
            binding.rlSoundCard.setBackgroundColor(Color.parseColor(ADD_CARD_COLOR))
            binding.ivPlayPause.setImageResource(R.drawable.ic_add)
            binding.root.setOnClickListener { onAddClick() }
            binding.root.setOnLongClickListener(null)
        }
    }
}
