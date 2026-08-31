package com.ruthvik.musicplayer


import SearchSongAdapter
import android.content.Intent
import android.os.Bundle
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.ruthvik.musicplayer.BottomSheets.MusicPlayerCreatePlaylistBottomSheet
import com.ruthvik.musicplayer.Utilities.Urls
import com.ruthvik.musicplayer.databinding.ActivitySearchBinding
import com.ruthvik.musicplayer.entities.Music
import java.net.URLEncoder

class SearchActivity : AppCompatActivity() {

    private companion object {
        const val SEARCH_REQUEST_TAG = "song_search"
    }

    private lateinit var binding: ActivitySearchBinding

    private val onlineSongs = ArrayList<Music>()

    private lateinit var adapter: SearchSongAdapter
    private lateinit var requestQueue: RequestQueue

    private var miniPlayerStartX = 0f

    private val playbackUiListener: () -> Unit = {
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }

        updateSearchMiniPlayer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupDarkEdgeToEdge()

        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchContainer.applySystemBarInsets()

        PlaybackManager.init(this)
        PlaylistManager.init(this)

        requestQueue = Volley.newRequestQueue(this)

        requestNotificationPermissionIfNeeded()

        setupRecyclerView()
        setupSearchListener()
        setupBackButton()
        setupBackHandler()
        setupSearchMiniPlayer()

        updateEmptyState()
    }

    override fun onStart() {
        super.onStart()

        PlaybackManager.addUiListener(playbackUiListener)

        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }

        updateSearchMiniPlayer()
    }

    override fun onStop() {
        PlaybackManager.removeUiListener(playbackUiListener)
        PlaybackManager.ensureNotificationService(this)

        super.onStop()
    }

    private fun setupRecyclerView() {

        adapter = SearchSongAdapter(
            object : SearchSongAdapter.OnItemClickListener {

                override fun onPlayClick(position: Int) {
                    handlePlayClick(position)
                }

                override fun onItemClick(position: Int) {
                    openPlayerActivity(position)
                }

                override fun onAddToPlayListClick(
                    position: Int,
                    song: Music
                ) {
                    showBottomSheet(song)
                }
            }
        )

        binding.rvSearchResults.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
            )

        binding.rvSearchResults.adapter = adapter
    }

    private fun showBottomSheet(song: Music) {

        val dialog = BottomSheetDialog(this)

        val view = layoutInflater.inflate(
            R.layout.bottom_sheet_playlist,
            null
        )

        dialog.setContentView(view)

        val bottomSheet =
            dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )

        bottomSheet?.layoutParams?.height =
            ViewGroup.LayoutParams.MATCH_PARENT

        val rvPlaylists =
            view.findViewById<androidx.recyclerview.widget.RecyclerView>(
                R.id.rvPlaylists
            )

        rvPlaylists.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
            )

        val playlistAdapter =
            PlaylistAdapter(
                object : PlaylistAdapter.OnPlaylistClickListener {

                    override fun onPlaylistClick(
                        playlist: Playlist
                    ) {

                        PlaylistManager.addSongToPlaylist(
                            playlist.id,
                            song
                        )

                        Toast.makeText(
                            this@SearchActivity,
                            getString(
                                R.string.song_added_to_playlist,
                                song.song.orEmpty(),
                                playlist.name
                            ),
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()
                    }
                }
            )

        rvPlaylists.adapter = playlistAdapter

        updatePlaylistRecyclerView(playlistAdapter)

        val btnAdd =
            view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
                R.id.btnAdd
            )

        btnAdd.setOnClickListener {

            val createDialog =
                MusicPlayerCreatePlaylistBottomSheet(
                    this,
                    object :
                        MusicPlayerCreatePlaylistBottomSheet.OnOptionClick {

                        override fun onOptionSelect(
                            playlistName: String
                        ) {

                            val newPlaylist =
                                PlaylistManager.createPlaylist(
                                    playlistName
                                )

                            PlaylistManager.addSongToPlaylist(
                                newPlaylist.id,
                                song
                            )

                            updatePlaylistRecyclerView(
                                playlistAdapter
                            )

                            Toast.makeText(
                                this@SearchActivity,
                                getString(
                                    R.string.playlist_created_and_song_added,
                                    playlistName
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

            createDialog.show()
        }

        val behavior =
            BottomSheetBehavior.from(bottomSheet!!)

        behavior.state =
            BottomSheetBehavior.STATE_EXPANDED

        behavior.skipCollapsed = true

        dialog.show()
    }

    private fun updatePlaylistRecyclerView(
        adapter: PlaylistAdapter
    ) {

        val playlists =
            PlaylistManager.getAllPlaylists()

        adapter.submitList(
            playlists.toList()
        )
    }

    private fun setupSearchListener() {

        binding.etSearch.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    searchOnlineSongs(
                        s.toString()
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    private fun setupBackButton() {

        binding.btnBack.setOnClickListener {

            stopSearchPlaybackAndHideMiniPlayer()

            finish()
        }
    }

    private fun setupBackHandler() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    stopSearchPlaybackAndHideMiniPlayer()

                    finish()
                }
            }
        )
    }

    private fun requestNotificationPermissionIfNeeded() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU &&

            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestNotificationPermissionLauncher.launch(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private fun setupSearchMiniPlayer() {

        binding.searchMiniPlayer.setOnClickListener {
            openCurrentSearchSongPlayer()
        }

        binding.btnMiniPlayPause.setOnClickListener {
            toggleCurrentSearchSong()
        }

        binding.btnMiniNext.setOnClickListener {

            PlaybackManager.playNext()

            updateSearchMiniPlayer()
        }

        binding.btnMiniStop.setOnClickListener {

            PlaybackManager.stop()

            hideSearchMiniPlayer()
        }

        binding.searchMiniPlayer.setOnTouchListener { view, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    miniPlayerStartX =
                        event.rawX

                    false
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx =
                        (event.rawX - miniPlayerStartX)
                            .coerceAtLeast(0f)

                    view.translationX =
                        dx.coerceAtMost(
                            dpToPx(76).toFloat()
                        )

                    false
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    val dx =
                        event.rawX - miniPlayerStartX

                    if (dx > dpToPx(96)) {

                        PlaybackManager.stop()

                        hideSearchMiniPlayer()

                    } else {

                        view.animate()
                            .translationX(0f)
                            .setDuration(160)
                            .start()
                    }

                    false
                }

                else -> false
            }
        }
    }

    private fun searchOnlineSongs(query: String) {

        if (query.isBlank()) {

            requestQueue.cancelAll(
                SEARCH_REQUEST_TAG
            )

            stopSearchPlaybackAndHideMiniPlayer()

            onlineSongs.clear()

            adapter.submitList(
                emptyList()
            )

            updateEmptyState()

            return
        }

        onlineSongs.clear()

        fetchSearchedSong(
            query.trim()
        )
    }

    private fun submitOnlineSongs(
        songs: List<Music>
    ) {

        onlineSongs.clear()

        onlineSongs.addAll(songs)

        adapter.submitList(
            onlineSongs.toList()
        ) {
            adapter.notifyDataSetChanged()
        }

        updateEmptyState()

        updateSearchMiniPlayer()
    }

    private fun updateEmptyState() {

        val showEmpty =
            binding.etSearch.text
                .toString()
                .isNotBlank() &&
                    onlineSongs.isEmpty()

        binding.emptyState.isVisible =
            showEmpty

        binding.rvSearchResults.isVisible =
            !showEmpty
    }

    private fun handlePlayClick(
        position: Int
    ) {

        if (position !in onlineSongs.indices) {
            return
        }

        val song =
            onlineSongs[position]

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
                    onlineSongs,
                    restart = true
                )
            }
        }

        updateSearchMiniPlayer()
    }

    private fun openPlayerActivity(
        position: Int
    ) {

        if (position !in onlineSongs.indices) {
            return
        }

        val song =
            onlineSongs[position]

        if (
            !PlaybackManager.isCurrentSong(
                song.id
            )
        ) {

            PlaybackManager.playAt(
                this,
                position,
                onlineSongs,
                restart = true
            )
        }

        updateSearchMiniPlayer()

        startActivity(
            Intent(
                this,
                PlayerActivity::class.java
            ).apply {

                putParcelableArrayListExtra(
                    "songList",
                    ArrayList(onlineSongs)
                )

                putExtra(
                    "position",
                    position
                )
            }
        )
    }

    private fun fetchSearchedSong(
        query: String
    ) {

        requestQueue.cancelAll(
            SEARCH_REQUEST_TAG
        )

        val encodedQuery =
            URLEncoder.encode(
                query,
                "UTF-8"
            )

        val url =
            String.format(
                Urls.searchSongsUrl,
                encodedQuery
            )

        val request =
            StringRequest(
                Request.Method.GET,
                url,

                { response ->

                    try {

                        val songsResponse =
                            Gson().fromJson(
                                response,
                                Array<Music>::class.java
                            )

                        submitOnlineSongs(
                            songsResponse.toList()
                        )

                    } catch (e: Exception) {

                        e.printStackTrace()

                        submitOnlineSongs(
                            emptyList()
                        )
                    }
                },

                {
                    submitOnlineSongs(
                        emptyList()
                    )
                }
            ).apply {

                tag =
                    SEARCH_REQUEST_TAG
            }

        requestQueue.add(request)
    }

    private fun updateSearchMiniPlayer() {

        val song =
            currentSearchSong()

        if (
            song == null ||
            !PlaybackManager.hasActiveMedia()
        ) {

            hideSearchMiniPlayer()

            return
        }

        binding.searchMiniPlayerRoot.isVisible =
            true

        binding.searchMiniPlayer.translationX =
            0f

        binding.tvMiniTitle.text =
            song.song.orEmpty()

        binding.tvMiniArtist.text =
            song.primary_artists.orEmpty()

        binding.btnMiniPlayPause.setImageResource(

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

        Glide.with(this)
            .load(song.image)
            .centerCrop()
            .placeholder(R.drawable.music)
            .error(R.drawable.music)
            .into(binding.ivMiniArtwork)
    }

    private fun hideSearchMiniPlayer() {

        binding.searchMiniPlayer.translationX =
            0f

        binding.searchMiniPlayerRoot.isVisible =
            false
    }

    private fun stopSearchPlaybackAndHideMiniPlayer() {

        if (isSearchPlaybackActive()) {
            PlaybackManager.stop()
        }

        hideSearchMiniPlayer()
    }

    private fun toggleCurrentSearchSong() {

        val song =
            currentSearchSong()
                ?: return

        if (
            PlaybackManager.isPlayingSong(
                song.id
            )
        ) {

            PlaybackManager.pause()

        } else {

            PlaybackManager.play()
        }

        updateSearchMiniPlayer()
    }

    private fun openCurrentSearchSongPlayer() {

        val song =
            currentSearchSong()
                ?: return

        val position =
            onlineSongs.indexOfFirst {

                it.id == song.id &&
                        it.media_url == song.media_url
            }

        if (position != -1) {

            openPlayerActivity(
                position
            )
        }
    }

    private fun currentSearchSong(): Music? {

        val current =
            PlaybackManager.currentSong()
                ?: return null

        return onlineSongs.firstOrNull {

            it.id == current.id &&
                    it.media_url == current.media_url
        }
    }

    private fun isSearchPlaybackActive(): Boolean {

        val current =
            PlaybackManager.currentSong()
                ?: return false

        return currentSearchSong() != null ||
                current.image != null ||
                current.media_url
                    ?.startsWith("http://") == true ||
                current.media_url
                    ?.startsWith("https://") == true
    }

    private fun dpToPx(
        value: Int
    ): Int =
        (
                value *
                        resources.displayMetrics.density
                ).toInt()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}
}
