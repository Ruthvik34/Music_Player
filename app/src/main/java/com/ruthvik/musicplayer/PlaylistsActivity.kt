package com.ruthvik.musicplayer

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.ruthvik.musicplayer.Models.Song
import com.ruthvik.musicplayer.databinding.ActivityPlaylistsBinding

class PlaylistsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistsBinding
    private lateinit var playlistsAdapter: PlaylistsDetailAdapter
    private lateinit var songsAdapter: SongAdapter
    private var currentPlaylistId: String? = null
    private val currentPlaylistSongs = ArrayList<Song>()
    private val allSongsCache = mutableMapOf<Long, Song>()

    private val playbackUiListener: () -> Unit = {
        if (::songsAdapter.isInitialized) songsAdapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDarkEdgeToEdge()
        PlaybackManager.init(this)
        FavoritesManager.init(this)
        PlaylistManager.init(this)

        binding = ActivityPlaylistsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.main.applySystemBarInsets()

        loadAllSongsToCache()
        setupRecyclerView()
        setupBackButton()
        setupBackHandler()
        loadPlaylists()
    }

    override fun onStart() {
        super.onStart()
        PlaybackManager.addUiListener(playbackUiListener)
        if (::songsAdapter.isInitialized) songsAdapter.notifyDataSetChanged()
    }

    override fun onStop() {
        PlaybackManager.removeUiListener(playbackUiListener)
        PlaybackManager.ensureNotificationService(this)
        super.onStop()
    }

    private fun loadAllSongsToCache() {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Audio.Media.IS_MUSIC + "!=0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
        contentResolver.query(uri, null, selection, null, sortOrder)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                allSongsCache[id] = Song(id, title, artist, data, albumId)
            }
        }
    }

    private fun setupRecyclerView() {
        playlistsAdapter = PlaylistsDetailAdapter(object : PlaylistsDetailAdapter.OnPlaylistItemClick {
            override fun onPlaylistClick(playlist: Playlist) {
                viewPlaylistSongs(playlist)
            }

            override fun onDeletePlaylist(playlist: Playlist) {
                deletePlaylist(playlist)
            }
        })
        binding.rvPlaylists.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvPlaylists.adapter = playlistsAdapter

        songsAdapter = SongAdapter(object : SongAdapter.OnItemClickListener {
            override fun onPlayClick(position: Int) {
                handlePlayClick(position)
            }

            override fun onItemClick(position: Int) {
                openPlayerActivity(position)
            }

            override fun onLikeClick(position: Int, song: Song) {
                toggleLike(song)
            }

            override fun onRemoveClick(position: Int, song: Song) {
                removeSongFromCurrentPlaylist(position, song)
            }
        })
        songsAdapter.showRemoveButton = true
        binding.rvPlaylistSongs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvPlaylistSongs.adapter = songsAdapter
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnBackFromSongs.setOnClickListener { goBackToPlaylistsList() }
    }

    private fun loadPlaylists() {
        val playlists = PlaylistManager.getAllPlaylists()
        playlistsAdapter.submitList(playlists.toList())
        updateEmptyState(playlists.isEmpty())
    }

    private fun viewPlaylistSongs(playlist: Playlist) {
        currentPlaylistId = playlist.id
        currentPlaylistSongs.clear()
        currentPlaylistSongs.addAll(FavoritesManager.sortSongsLikedFirst(getSavedSongsForPlaylist(playlist)))

        binding.tvPlaylistTitle.text = playlist.name
        updatePlaylistSongCount()
        binding.rvPlaylists.isVisible = false
        binding.songDetailsContainer.isVisible = true

        submitPlaylistSongs()
    }

    private fun handlePlayClick(position: Int) {
        if (position !in currentPlaylistSongs.indices) return
        val song = currentPlaylistSongs[position]

        when {
            PlaybackManager.isPlayingSong(song.id) -> PlaybackManager.pause()
            PlaybackManager.isCurrentSong(song.id) -> PlaybackManager.play()
            else -> PlaybackManager.playAt(this, position, currentPlaylistSongs, restart = true)
        }
    }

    private fun toggleLike(song: Song) {
        val nowLiked = FavoritesManager.toggleLike(song.id)
        Toast.makeText(
            this,
            if (nowLiked) R.string.added_to_likes else R.string.removed_from_likes,
            Toast.LENGTH_SHORT
        ).show()

        val sorted = FavoritesManager.sortSongsLikedFirst(currentPlaylistSongs)
        currentPlaylistSongs.clear()
        currentPlaylistSongs.addAll(sorted)
        PlaybackManager.syncCurrentIndex(currentPlaylistSongs)
        submitPlaylistSongs()
    }

    private fun removeSongFromCurrentPlaylist(position: Int, song: Song) {
        val playlistId = currentPlaylistId ?: return
        PlaylistManager.removeSongFromPlaylist(playlistId, song.id)
        if (position in currentPlaylistSongs.indices) {
            currentPlaylistSongs.removeAt(position)
        } else {
            currentPlaylistSongs.removeAll { it.id == song.id }
        }

        updatePlaylistSongCount()
        PlaybackManager.syncCurrentIndex(currentPlaylistSongs)
        submitPlaylistSongs()
        Toast.makeText(this, R.string.removed_from_playlist, Toast.LENGTH_SHORT).show()
    }

    private fun updatePlaylistSongCount() {
        binding.tvSongCountInPlaylist.text = getString(R.string.song_count, currentPlaylistSongs.size)
    }

    private fun submitPlaylistSongs() {
        songsAdapter.submitList(currentPlaylistSongs.toList()) {
            songsAdapter.notifyDataSetChanged()
        }
    }

    private fun getSavedSongsForPlaylist(playlist: Playlist): List<Song> {
        val savedSongs = PlaylistManager.getPlaylistSongDetails(playlist.id)
        if (savedSongs.isEmpty()) {
            return playlist.songs.mapNotNull { songId -> allSongsCache[songId] }
        }

        return savedSongs.mapNotNull { savedSong ->
            val hasSavedDetails = savedSong.title.isNotBlank() && savedSong.data.isNotBlank()
            if (hasSavedDetails) {
                Song(
                    id = savedSong.songId,
                    title = savedSong.title,
                    artist = savedSong.artist,
                    data = savedSong.data,
                    albumId = savedSong.albumId,
                    imageUrl = savedSong.imageUrl
                )
            } else {
                allSongsCache[savedSong.songId]
            }
        }
    }

    private fun openPlayerActivity(position: Int) {
        if (position !in currentPlaylistSongs.indices) return
        val song = currentPlaylistSongs[position]

        if (!PlaybackManager.isCurrentSong(song.id)) {
            PlaybackManager.playAt(this, position, currentPlaylistSongs, restart = true)
        }

        startActivity(
            Intent(this, PlayerActivity::class.java).apply {
                putParcelableArrayListExtra("songList", currentPlaylistSongs)
                putExtra("position", position)
            }
        )
    }

    private fun deletePlaylist(playlist: Playlist) {
        PlaylistManager.deletePlaylist(playlist.id)
        loadPlaylists()
        if (currentPlaylistId == playlist.id) {
            binding.rvPlaylists.isVisible = true
            binding.songDetailsContainer.isVisible = false
            currentPlaylistId = null
        }
        Toast.makeText(this, R.string.playlist_deleted, Toast.LENGTH_SHORT).show()
    }

    private fun goBackToPlaylistsList() {
        stopCurrentPlaylistPlayback()
        binding.rvPlaylists.isVisible = true
        binding.songDetailsContainer.isVisible = false
        currentPlaylistId = null
        loadPlaylists()
    }

    private fun stopCurrentPlaylistPlayback() {
        val currentSong = PlaybackManager.currentSong() ?: return
        val isPlaylistSong = currentPlaylistSongs.any { playlistSong ->
            playlistSong.id == currentSong.id && playlistSong.data == currentSong.data
        }
        if (isPlaylistSong) {
            PlaybackManager.stop()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.isVisible = isEmpty
        binding.rvPlaylists.isVisible = !isEmpty
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.songDetailsContainer.isVisible) {
                    goBackToPlaylistsList()
                } else {
                    finish()
                }
            }
        })
    }
}
