import com.ruthvik.musicplayer.R


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ruthvik.musicplayer.databinding.ItemSongsBinding
import com.ruthvik.musicplayer.entities.Music

class SearchSongAdapter(
    private val listener: OnItemClickListener
) : ListAdapter<Music, SearchSongAdapter.ViewHolder>(DiffCallBack()) {

    class ViewHolder(
        val binding: ItemSongsBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ItemSongsBinding.inflate(
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

        val music = getItem(position)

        holder.binding.ivLike.visibility =
            View.GONE

        holder.binding.btnAdd.visibility =
            View.VISIBLE

        // Song name
        holder.binding.tvSongTitle.text =
            music.song.orEmpty()

        // Artist name
        holder.binding.tvArtist.text =
            music.primary_artists
                .orEmpty()
                .ifBlank {
                    music.singers.orEmpty()
                }

        updatePlayButton(
            holder,
            music
        )

        holder.binding.ivPlayHint.setOnClickListener {

            val pos =
                holder.bindingAdapterPosition

            if (
                pos != RecyclerView.NO_POSITION
            ) {
                listener.onPlayClick(pos)
            }
        }

        val openPlayer = {

            val pos =
                holder.bindingAdapterPosition

            if (
                pos != RecyclerView.NO_POSITION
            ) {
                listener.onItemClick(pos)
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

        holder.binding.btnAdd.setOnClickListener {

            val pos =
                holder.bindingAdapterPosition

            if (
                pos != RecyclerView.NO_POSITION
            ) {
                listener.onAddToPlayListClick(
                    pos,
                    music
                )
            }
        }

        // JioSaavn image URL
        Glide.with(
            holder.binding.root.context
        )
            .load(
                music.image
                    ?.takeIf { it.isNotBlank() }
            )
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

    private fun updatePlayButton(
        holder: ViewHolder,
        music: Music
    ) {

        holder.binding.ivPlayHint.setImageResource(

            if (
                PlaybackManager.isPlayingSong(
                    music.id
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

            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Music,
            newItem: Music
        ): Boolean {

            return oldItem == newItem
        }
    }

    interface OnItemClickListener {

        fun onPlayClick(
            position: Int
        )

        fun onItemClick(
            position: Int
        )

        fun onAddToPlayListClick(
            position: Int,
            song: Music
        )
    }
}

