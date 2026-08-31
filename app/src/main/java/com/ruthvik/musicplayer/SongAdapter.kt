
package com.ruthvik.musicplayer

import android.content.ContentUris
import android.net.Uri
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ruthvik.musicplayer.databinding.ItemSongsBinding
import com.ruthvik.musicplayer.entities.Music

class SongAdapter(
    private val listener: OnItemClickListener
) : ListAdapter<Music, SongAdapter.ViewHolder>(
    DiffCallBack()
) {

    var showRemoveButton: Boolean = false

    class ViewHolder(
        val binding: ItemSongsBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemSongsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val song =
            getItem(position)

        /*
         * Local MediaStore album artwork.
         *
         * For online songs, Music.image contains
         * the remote artwork URL.
         */
        val albumUri =
            song.albumid.toLongOrNull()?.let { albumId ->

                ContentUris.withAppendedId(
                    Uri.parse(
                        "content://media/external/audio/albumart"
                    ),
                    albumId
                )

            }

        holder.binding.tvSongTitle.text =
            song.song.orEmpty()

        holder.binding.tvArtist.text =
            song.primary_artists
                .orEmpty()
                .ifBlank {
                    song.singers.orEmpty()
                }

        updateLikeButton(
            holder,
            song
        )

        updateRemoveButton(
            holder,
            song
        )

        updatePlayButton(
            holder,
            song
        )

        holder.binding.ivLike.setOnClickListener {

            val pos =
                holder.bindingAdapterPosition

            if (
                pos != RecyclerView.NO_POSITION
            ) {

                listener.onLikeClick(
                    pos,
                    song
                )
            }
        }

        holder.binding.btnAdd.setOnClickListener {

            val pos =
                holder.bindingAdapterPosition

            if (
                pos != RecyclerView.NO_POSITION
            ) {

                listener.onRemoveClick(
                    pos,
                    song
                )
            }
        }

        holder.binding.ivPlayHint.setOnClickListener {

            val pos =
                holder.bindingAdapterPosition

            if (
                pos != RecyclerView.NO_POSITION
            ) {

                listener.onPlayClick(
                    pos
                )
            }
        }

        val openPlayer = {

            val pos =
                holder.bindingAdapterPosition

            if (
                pos != RecyclerView.NO_POSITION
            ) {

                listener.onItemClick(
                    pos
                )
            }
        }

        holder.binding.root.setOnClickListener {
            openPlayer()
        }

        holder.binding.ivArtist.setOnClickListener {
            openPlayer()
        }

        holder.binding.tvSongTitle.setOnClickListener {
            openPlayer()
        }

        holder.binding.tvArtist.setOnClickListener {
            openPlayer()
        }

        /*
         * Online Music:
         *     song.image
         *
         * Local Music:
         *     albumUri
         */
        val imageModel =
            song.image
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: albumUri

        Glide.with(
            holder.binding.root.context
        )
            .load(imageModel)
            .centerCrop()
            .placeholder(
                R.drawable.music
            )
            .error(
                R.drawable.music
            )
            .into(
                holder.binding.ivArtist
            )
    }

    private fun updateLikeButton(
        holder: ViewHolder,
        song: Music
    ) {

        val liked =
            FavoritesManager.isLiked(
                song.id
            )

        val context =
            holder.binding.root.context

        holder.binding.ivLike.visibility =
            View.VISIBLE

        holder.binding.ivLike.setImageResource(

            if (liked) {

                R.drawable.ic_favorite_filled_24

            } else {

                R.drawable.outline_favorite_24
            }
        )

        holder.binding.ivLike.imageTintList =
            ColorStateList.valueOf(

                ContextCompat.getColor(

                    context,

                    if (liked) {

                        R.color.accent_secondary

                    } else {

                        R.color.text_hint
                    }
                )
            )

        holder.binding.ivLike.contentDescription =
            context.getString(

                if (liked) {

                    R.string.unlike_song

                } else {

                    R.string.like_song
                }
            )
    }

    private fun updateRemoveButton(
        holder: ViewHolder,
        song: Music
    ) {

        val context =
            holder.binding.root.context

        holder.binding.btnAdd.visibility =

            if (showRemoveButton) {

                View.VISIBLE

            } else {

                View.GONE
            }

        holder.binding.btnAdd.setImageResource(
            R.drawable.ic_cancel_24
        )

        holder.binding.btnAdd.imageTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.text_hint
                )
            )

        holder.binding.btnAdd.contentDescription =
            context.getString(
                R.string.remove_from_playlist
            )
    }

    private fun updatePlayButton(
        holder: ViewHolder,
        song: Music
    ) {

        holder.binding.ivPlayHint.setImageResource(

            if (
                PlaybackManager.isPlayingSong(
                    song.id
                )
            ) {

                R.drawable.ic_pause_24

            } else {

                R.drawable.ic_play_arrow_24
            }
        )
    }

    class DiffCallBack :
        DiffUtil.ItemCallback<Music>() {

        override fun areItemsTheSame(
            oldItem: Music,
            newItem: Music
        ): Boolean {

            return oldItem.id ==
                    newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Music,
            newItem: Music
        ): Boolean {

            return oldItem ==
                    newItem
        }
    }

    interface OnItemClickListener {

        fun onPlayClick(
            position: Int
        )

        fun onItemClick(
            position: Int
        )

        fun onLikeClick(
            position: Int,
            song: Music
        )

        fun onRemoveClick(
            position: Int,
            song: Music
        ) {
        }
    }
}

