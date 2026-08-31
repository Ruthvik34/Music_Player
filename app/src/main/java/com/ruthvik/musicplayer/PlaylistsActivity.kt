
package com.ruthvik.musicplayer

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.ruthvik.musicplayer.databinding.ActivityPlaylistsBinding
import com.ruthvik.musicplayer.entities.Music

class PlaylistsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistsBinding

    private lateinit var playlistsAdapter: PlaylistsDetailAdapter

    private lateinit var songsAdapter: SongAdapter

    private var currentPlaylistId: String? = null

    private val currentPlaylistSongs =
        ArrayList<Music>()

    private val allSongsCache =
        mutableMapOf<String, Music>()

    private val playbackUiListener: () -> Unit = {

        if (::songsAdapter.isInitialized) {
            songsAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setupDarkEdgeToEdge()

        PlaybackManager.init(this)
        FavoritesManager.init(this)
        PlaylistManager.init(this)

        binding =
            ActivityPlaylistsBinding.inflate(
                layoutInflater
            )

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

        PlaybackManager.addUiListener(
            playbackUiListener
        )

        if (::songsAdapter.isInitialized) {
            songsAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {

        PlaybackManager.removeUiListener(
            playbackUiListener
        )

        PlaybackManager.ensureNotificationService(
            this
        )

        super.onStop()
    }

    /**
     * Loads local MediaStore songs into a cache.
     *
     * Music.id is String now, so the cache
     * also uses String as the key.
     */
    private fun loadAllSongsToCache() {

        val uri =
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val selection =
            MediaStore.Audio.Media.IS_MUSIC + "!=0"

        val sortOrder =
            MediaStore.Audio.Media.TITLE + " ASC"

        contentResolver.query(
            uri,
            null,
            selection,
            null,
            sortOrder
        )?.use { cursor ->

            while (cursor.moveToNext()) {

                val id =
                    cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media._ID
                        )
                    )

                val title =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.TITLE
                        )
                    )

                val artist =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ARTIST
                        )
                    )

                val data =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DATA
                        )
                    )

                val albumId =
                    cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ALBUM_ID
                        )
                    )

                val albumImageUri =
                    ContentUris.withAppendedId(
                        Uri.parse(
                            "content://media/external/audio/albumart"
                        ),
                        albumId
                    )

                val music =
                    Music(

                        `320kbps` = "false",

                        album = "",

                        album_url = "",

                        albumid =
                            albumId.toString(),

                        artistMap =
                            emptyMap(),

                        cache_state = "",

                        copyright_text = "",

                        disabled = "",

                        disabled_text = "",

                        duration = "0",

                        encrypted_drm_media_url = "",

                        encrypted_media_path = "",

                        encrypted_media_url = "",

                        explicit_content = 0,

                        featured_artists = "",

                        featured_artists_id = "",

                        has_lyrics = "false",

                        has_trivia = false,

                        id =
                            id.toString(),

                        image =
                            albumImageUri.toString(),

                        is_dolby_content = false,

                        is_drm = 0,

                        label = "",

                        label_id = "",

                        label_url = "",

                        language = "",

                        lyrics_snippet = "",

                        media_preview_url = "",

                        media_url =
                            data ?: "",

                        music = "",

                        music_id = "",

                        origin = "local",

                        perma_url = "",

                        play_count = 0,

                        primary_artists =
                            artist ?: "",

                        primary_artists_id = "",

                        release_date = "",


                        singers =
                            artist ?: "",

                        song =
                            title ?: "",

                        starred = "",

                        starring = "",

                        triller_available = false,



                        type = "local",


                        webp = false,

                        year = ""
                    )

                allSongsCache[
                    music.id
                ] = music
            }
        }
    }

    private fun setupRecyclerView() {

        playlistsAdapter =
            PlaylistsDetailAdapter(
                object :
                    PlaylistsDetailAdapter.OnPlaylistItemClick {

                    override fun onPlaylistClick(
                        playlist: Playlist
                    ) {
                        viewPlaylistSongs(
                            playlist
                        )
                    }

                    override fun onDeletePlaylist(
                        playlist: Playlist
                    ) {
                        deletePlaylist(
                            playlist
                        )
                    }
                }
            )

        binding.rvPlaylists.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
            )

        binding.rvPlaylists.adapter =
            playlistsAdapter

        songsAdapter =
            SongAdapter(
                object :
                    SongAdapter.OnItemClickListener {

                    override fun onPlayClick(
                        position: Int
                    ) {
                        handlePlayClick(
                            position
                        )
                    }

                    override fun onItemClick(
                        position: Int
                    ) {
                        openPlayerActivity(
                            position
                        )
                    }

                    override fun onLikeClick(
                        position: Int,
                        song: Music
                    ) {
                        toggleLike(
                            song
                        )
                    }

                    override fun onRemoveClick(
                        position: Int,
                        song: Music
                    ) {
                        removeSongFromCurrentPlaylist(
                            position,
                            song
                        )
                    }
                }
            )

        songsAdapter.showRemoveButton =
            true

        binding.rvPlaylistSongs.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
            )

        binding.rvPlaylistSongs.adapter =
            songsAdapter
    }

    private fun setupBackButton() {

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnBackFromSongs.setOnClickListener {
            goBackToPlaylistsList()
        }
    }

    private fun loadPlaylists() {

        val playlists =
            PlaylistManager.getAllPlaylists()

        playlistsAdapter.submitList(
            playlists.toList()
        )

        updateEmptyState(
            playlists.isEmpty()
        )
    }

    private fun viewPlaylistSongs(
        playlist: Playlist
    ) {

        currentPlaylistId =
            playlist.id

        currentPlaylistSongs.clear()

        currentPlaylistSongs.addAll(

            FavoritesManager.sortSongsLikedFirst(
                getSavedSongsForPlaylist(
                    playlist
                )
            )
        )

        binding.tvPlaylistTitle.text =
            playlist.name

        updatePlaylistSongCount()

        binding.rvPlaylists.isVisible =
            false

        binding.songDetailsContainer.isVisible =
            true

        submitPlaylistSongs()
    }

    private fun handlePlayClick(
        position: Int
    ) {

        if (
            position !in
            currentPlaylistSongs.indices
        ) {
            return
        }

        val song =
            currentPlaylistSongs[position]

        when {

            PlaybackManager.isPlayingSong(
                song.id
            ) -> {

                PlaybackManager.pause()
            }

            PlaybackManager.isCurrentSong(
                song.id
            ) -> {

                PlaybackManager.play()
            }

            else -> {

                PlaybackManager.playAt(
                    this,
                    position,
                    currentPlaylistSongs,
                    restart = true
                )
            }
        }
    }

    private fun toggleLike(
        song: Music
    ) {

        val nowLiked =
            FavoritesManager.toggleLike(
                song.id
            )

        Toast.makeText(
            this,
            if (nowLiked) {
                R.string.added_to_likes
            } else {
                R.string.removed_from_likes
            },
            Toast.LENGTH_SHORT
        ).show()

        val sorted =
            FavoritesManager.sortSongsLikedFirst(
                currentPlaylistSongs
            )

        currentPlaylistSongs.clear()

        currentPlaylistSongs.addAll(
            sorted
        )

        PlaybackManager.syncCurrentIndex(
            currentPlaylistSongs
        )

        submitPlaylistSongs()
    }

    private fun removeSongFromCurrentPlaylist(
        position: Int,
        song: Music
    ) {

        val playlistId =
            currentPlaylistId
                ?: return

        PlaylistManager.removeSongFromPlaylist(
            playlistId,
            song.id
        )

        if (
            position in
            currentPlaylistSongs.indices
        ) {

            currentPlaylistSongs.removeAt(
                position
            )

        } else {

            currentPlaylistSongs.removeAll {
                it.id == song.id
            }
        }

        updatePlaylistSongCount()

        PlaybackManager.syncCurrentIndex(
            currentPlaylistSongs
        )

        submitPlaylistSongs()

        Toast.makeText(
            this,
            R.string.removed_from_playlist,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updatePlaylistSongCount() {

        binding.tvSongCountInPlaylist.text =
            getString(
                R.string.song_count,
                currentPlaylistSongs.size
            )
    }

    private fun submitPlaylistSongs() {

        songsAdapter.submitList(
            currentPlaylistSongs.toList()
        ) {

            songsAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Gets playlist songs from Room.
     *
     * The PlaylistManager now returns
     * PrivatePlayListSongs where songId
     * is String.
     */
    private fun getSavedSongsForPlaylist(
        playlist: Playlist
    ): List<Music> {

        val savedSongs =
            PlaylistManager.getPlaylistSongDetails(
                playlist.id
            )

        if (savedSongs.isEmpty()) {

            return playlist.songs.mapNotNull { songId ->

                allSongsCache[
                    songId.toString()
                ]
            }
        }

        return savedSongs.mapNotNull { savedSong ->

            val hasSavedDetails =
                savedSong.song.isNotBlank() &&
                        savedSong.media_url.isNotBlank()

            if (hasSavedDetails) {

                Music(

                    `320kbps` =
                        "false",

                    album = "",

                    album_url = "",

                    albumid =
                        savedSong.albumid,

                    artistMap =
                        emptyMap(),

                    cache_state = "",

                    copyright_text = "",

                    disabled = "",

                    disabled_text = "",

                    duration = "0",

                    encrypted_drm_media_url = "",

                    encrypted_media_path = "",

                    encrypted_media_url = "",

                    explicit_content = 0,

                    featured_artists = "",

                    featured_artists_id = "",

                    has_lyrics = "false",

                    has_trivia = false,

                    id =
                        savedSong.songId,

                    image =
                        savedSong.image,

                    is_dolby_content = false,

                    is_drm = 0,

                    label = "",

                    label_id = "",

                    label_url = "",

                    language = "",

                    lyrics_snippet = "",

                    media_preview_url = "",

                    media_url =
                        savedSong.media_url,

                    music = "",

                    music_id = "",

                    origin = "playlist",

                    perma_url = "",

                    play_count = 0,

                    primary_artists =
                        savedSong.primary_artists,

                    primary_artists_id = "",

                    release_date = "",



                    singers =
                        savedSong.primary_artists,

                    song =
                        savedSong.song,

                    starred = "",

                    starring = "",

                    triller_available = false,



                    type = "playlist",



                    webp = false,

                    year = ""
                )

            } else {

                allSongsCache[
                    savedSong.songId
                ]
            }
        }
    }

    private fun openPlayerActivity(
        position: Int
    ) {

        if (
            position !in
            currentPlaylistSongs.indices
        ) {
            return
        }

        val song =
            currentPlaylistSongs[position]

        if (
            !PlaybackManager.isCurrentSong(
                song.id
            )
        ) {

            PlaybackManager.playAt(
                this,
                position,
                currentPlaylistSongs,
                restart = true
            )
        }

        startActivity(

            Intent(
                this,
                PlayerActivity::class.java
            ).apply {

                putParcelableArrayListExtra(
                    "songList",
                    ArrayList(
                        currentPlaylistSongs
                    )
                )

                putExtra(
                    "position",
                    position
                )
            }
        )
    }

    private fun deletePlaylist(
        playlist: Playlist
    ) {

        PlaylistManager.deletePlaylist(
            playlist.id
        )

        loadPlaylists()

        if (
            currentPlaylistId ==
            playlist.id
        ) {

            binding.rvPlaylists.isVisible =
                true

            binding.songDetailsContainer.isVisible =
                false

            currentPlaylistId =
                null
        }

        Toast.makeText(
            this,
            R.string.playlist_deleted,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun goBackToPlaylistsList() {

        stopCurrentPlaylistPlayback()

        binding.rvPlaylists.isVisible =
            true

        binding.songDetailsContainer.isVisible =
            false

        currentPlaylistId =
            null

        loadPlaylists()
    }

    private fun stopCurrentPlaylistPlayback() {

        val currentSong =
            PlaybackManager.currentSong()
                ?: return

        val isPlaylistSong =
            currentPlaylistSongs.any { playlistSong ->

                playlistSong.id ==
                        currentSong.id &&

                        playlistSong.media_url ==
                        currentSong.media_url
            }

        if (isPlaylistSong) {
            PlaybackManager.stop()
        }
    }

    private fun updateEmptyState(
        isEmpty: Boolean
    ) {

        binding.emptyState.isVisible =
            isEmpty

        binding.rvPlaylists.isVisible =
            !isEmpty
    }

    private fun setupBackHandler() {

        onBackPressedDispatcher.addCallback(

            this,

            object :
                OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    if (
                        binding.songDetailsContainer
                            .isVisible
                    ) {

                        goBackToPlaylistsList()

                    } else {

                        finish()
                    }
                }
            }
        )
    }
}

