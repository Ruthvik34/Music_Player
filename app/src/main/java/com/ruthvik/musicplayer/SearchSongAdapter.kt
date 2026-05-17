package com.ruthvik.musicplayer

import android.content.ContentUris
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ruthvik.musicplayer.Models.Song
import com.ruthvik.musicplayer.databinding.ItemSongsBinding

class SearchSongAdapter(private val listener: OnItemClickListener) :
    ListAdapter<Song, SearchSongAdapter.ViewHolder>(DiffCallBack()) {

    class ViewHolder(val binding: ItemSongsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSongsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = getItem(position)
        val albumUri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            song.albumId
        )
        holder.binding.ivLike.visibility= View.GONE
        holder.binding.btnAdd.visibility= View.VISIBLE

        holder.binding.tvSongTitle.text = song.title
        holder.binding.tvArtist.text = song.artist

        updatePlayButton(holder, song)

        holder.binding.ivPlayHint.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                listener.onPlayClick(pos)
            }
        }

        val openPlayer = {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                listener.onItemClick(pos)
            }
        }
        holder.binding.root.setOnClickListener { openPlayer() }
        holder.binding.ivArtist.setOnClickListener { openPlayer() }
        holder.binding.tvSongTitle.setOnClickListener { openPlayer() }
        holder.binding.tvArtist.setOnClickListener { openPlayer() }

        holder.binding.btnAdd.setOnClickListener {

            listener.onAddToPlayListClick(position,song)
        }

        Glide.with(holder.binding.root.context)
            .load(song.imageUrl ?: albumUri)
            .centerCrop()
            .placeholder(R.drawable.music)
            .error(R.drawable.music)
            .into(holder.binding.ivArtist)
    }


    private fun updatePlayButton(holder: ViewHolder, song: Song) {
        holder.binding.ivPlayHint.setImageResource(
            if (PlaybackManager.isPlayingSong(song.id)) R.drawable.ic_pause_24
            else R.drawable.ic_play_arrow_24
        )
    }

    class DiffCallBack : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean =
            oldItem == newItem
    }

    interface OnItemClickListener {
        fun onPlayClick(position: Int)
        fun onItemClick(position: Int)
        fun onAddToPlayListClick(position: Int, song: Song)
    }
}
