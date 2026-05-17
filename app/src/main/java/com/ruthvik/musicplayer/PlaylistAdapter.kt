package com.ruthvik.musicplayer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ruthvik.musicplayer.databinding.ItemPlaylistBinding

class PlaylistAdapter(private val listener: OnPlaylistClickListener) :
    ListAdapter<Playlist, PlaylistAdapter.ViewHolder>(DiffCallBack()) {

    class ViewHolder(val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = getItem(position)

        holder.binding.tvPlaylistName.text = playlist.name
        holder.binding.tvSongCount.text = holder.binding.root.context.getString(
            R.string.song_count,
            playlist.songs.size
        )

        holder.binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                listener.onPlaylistClick(playlist)
            }
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean =
            oldItem == newItem
    }

    interface OnPlaylistClickListener {
        fun onPlaylistClick(playlist: Playlist)
    }
}

