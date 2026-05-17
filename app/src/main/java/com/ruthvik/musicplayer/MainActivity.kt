package com.ruthvik.musicplayer

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.ruthvik.musicplayer.Models.Song
import com.ruthvik.musicplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val songsList = ArrayList<Song>()
    private lateinit var adapter: SongAdapter

    private val playbackUiListener: () -> Unit = {
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDarkEdgeToEdge()
        PlaybackManager.init(this)
        FavoritesManager.init(this)
        PlaylistManager.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.main.applySystemBarInsets()
        requestNotificationPermissionIfNeeded()
        setupSearchButton()
        setupBackHandler()
        binding.rvSongsList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        checkPermissionsAndLoadSongs()
    }

    override fun onStart() {
        super.onStart()
        PlaybackManager.addUiListener(playbackUiListener)
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
    }

    override fun onStop() {
        PlaybackManager.removeUiListener(playbackUiListener)
        PlaybackManager.ensureNotificationService(this)
        super.onStop()
    }

    private fun checkPermissionsAndLoadSongs() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun getSongs(): List<Song> {
        val songList = ArrayList<Song>()
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
                songList.add(Song(id, title, artist, data, albumId))
            }
        }
        return songList
    }

    private fun loadSongs() {
        val allSongs = getSongs()
        songsList.clear()
        songsList.addAll(FavoritesManager.sortSongsLikedFirst(allSongs))
        PlaybackManager.syncCurrentIndex(songsList)
        updateLibraryUi()

        if (!::adapter.isInitialized) {
            adapter = SongAdapter(object : SongAdapter.OnItemClickListener {
                override fun onPlayClick(position: Int) {
                    handlePlayClick(position)
                }

                override fun onItemClick(position: Int) {
                    openPlayerActivity(position)
                }

                override fun onLikeClick(position: Int, song: Song) {
                    toggleLike(song)
                }
            })
            binding.rvSongsList.adapter = adapter
        }
        submitSortedList()
    }

    private fun toggleLike(song: Song) {
        val nowLiked = FavoritesManager.toggleLike(song.id)
        Toast.makeText(
            this,
            if (nowLiked) R.string.added_to_likes else R.string.removed_from_likes,
            Toast.LENGTH_SHORT
        ).show()

        val sorted = FavoritesManager.sortSongsLikedFirst(songsList)
        songsList.clear()
        songsList.addAll(sorted)
        PlaybackManager.syncCurrentIndex(songsList)
        updateLibraryUi()
        submitSortedList()
    }

    private fun submitSortedList() {
        adapter.submitList(songsList.toList()) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun handlePlayClick(position: Int) {
        if (position !in songsList.indices) return
        val song = songsList[position]

        when {
            PlaybackManager.isPlayingSong(song.id) -> PlaybackManager.pause()
            PlaybackManager.isCurrentSong(song.id) -> PlaybackManager.play()
            else -> PlaybackManager.playAt(this, position, songsList, restart = true)
        }
    }

    private fun openPlayerActivity(position: Int) {
        if (position !in songsList.indices) return
        val song = songsList[position]

        if (!PlaybackManager.isCurrentSong(song.id)) {
            PlaybackManager.playAt(this, position, songsList, restart = true)
        }

        startActivity(
            Intent(this, PlayerActivity::class.java).apply {
                putParcelableArrayListExtra("songList", ArrayList(songsList))
                putExtra("position", position)
            }
        )
    }

    private fun updateLibraryUi() {
        val count = songsList.size
        val likedCount = songsList.count { FavoritesManager.isLiked(it.id) }
        binding.tvSubtitle.text = when {
            count == 0 -> getString(R.string.library_subtitle_empty)
            likedCount > 0 -> getString(R.string.library_subtitle_with_likes, count, likedCount)
            else -> getString(R.string.library_subtitle, count)
        }
        binding.emptyState.isVisible = count == 0
        binding.rvSongsList.isVisible = count > 0
    }

    private fun setupSearchButton() {
        binding.btnSearch.setOnClickListener {
            stopLibraryPlayback()
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.btnPlaylists.setOnClickListener {
            stopLibraryPlayback()
            startActivity(Intent(this, PlaylistsActivity::class.java))
        }
    }

    private fun stopLibraryPlayback() {
        val currentSong = PlaybackManager.currentSong() ?: return
        val isLibrarySong = songsList.any { song ->
            song.id == currentSong.id && song.data == currentSong.data
        }
        if (isLibrarySong) {
            PlaybackManager.stop()
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                PlaybackManager.stop()
                finish()
            }
        })
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                loadSongs()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
}
