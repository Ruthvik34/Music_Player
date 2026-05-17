package com.ruthvik.musicplayer

import android.content.ContentUris
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.request.target.Target
import com.google.android.material.slider.Slider
import com.ruthvik.musicplayer.Models.Song
import com.ruthvik.musicplayer.databinding.ActivityPlayerBinding
import jp.wasabeef.glide.transformations.BlurTransformation

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false
    private var exoPlayer: ExoPlayer? = null
    private lateinit var gestureDetectorLeft: GestureDetector
    private lateinit var gestureDetectorRight: GestureDetector

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncUiFromManager()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlayAndPause()
            if (playbackState == Player.STATE_READY) {
                syncProgressUi()
            }
            syncUiFromManager()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayAndPause()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            updateToggleAlpha()
        }

        override fun onPlayerError(error: PlaybackException) {
            Toast.makeText(this@PlayerActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isUserSeeking) {
                syncProgressUi()
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDarkEdgeToEdge()
        PlaybackManager.init(this)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.main.applySystemBarInsets()

        val songList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("songList", Song::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("songList")
        } ?: PlaybackManager.getSongList()

        if (songList.isEmpty()) {
            Toast.makeText(this, R.string.no_songs_available, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val index = intent.getIntExtra("position", PlaybackManager.currentIndex)
            .coerceIn(0, songList.lastIndex)
        val song = songList[index]

        if (!PlaybackManager.isCurrentSong(song.id)) {
            PlaybackManager.playAt(this, index, songList, restart = true)
        } else {
            PlaybackManager.updatePlaylistKeepingPlayback(songList)
        }

        exoPlayer = PlaybackManager.getPlayer()
        exoPlayer?.addListener(playerListener)

        updateToggleAlpha()
        syncUiFromManager()
        setUpControls()
        setUpSeekBar()
        setupBackHandler()
    }

    override fun onResume() {
        super.onResume()
        syncProgressUi()
        updatePlayAndPause()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        handler.removeCallbacks(updateRunnable)
        PlaybackManager.ensureNotificationService(this)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        exoPlayer?.removeListener(playerListener)
        super.onDestroy()
    }

    private fun syncUiFromManager() {
        updateUi(PlaybackManager.currentSong())
        updatePlayAndPause()
        syncProgressUi()
    }

    private fun syncProgressUi() {
        val player = exoPlayer ?: return
        val duration = player.usableDuration()
        if (duration <= 0) return

        val currentPos = player.currentPosition.coerceAtLeast(0L)
        binding.seekBar.value = ((currentPos.toFloat() / duration) * 100f).coerceIn(0f, 100f)
        binding.tvStartTime.text = formatTime((currentPos / 1000).toInt())
        binding.tvEndTime.text = formatTime((duration / 1000).toInt())
    }

    private fun setUpSeekBar() {
        binding.seekBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                isUserSeeking = false
                PlaybackManager.seekToProgress(slider.value)
                syncProgressUi()
            }
        })

        binding.seekBar.addOnChangeListener { slider, _, fromUser ->
            if (fromUser) {
                val duration = exoPlayer?.usableDuration() ?: 0L
                if (duration > 0) {
                    binding.tvStartTime.text =
                        formatTime(((duration * slider.value / 100f) / 1000).toInt())
                }
            }
        }
    }

    private fun setUpControls() {
        binding.ivBack.setOnClickListener { finish() }

        binding.ivPlay.setOnClickListener {
            PlaybackManager.togglePlayPause()
            updatePlayAndPause()
        }

        binding.ivPrevious.setOnClickListener {
            PlaybackManager.playPrevious()
            syncUiFromManager()
        }

        binding.ivNext.setOnClickListener {
            PlaybackManager.playNext()
            syncUiFromManager()
        }

        binding.ivShuffle.setOnClickListener {
            PlaybackManager.toggleShuffle()
            updateToggleAlpha()
            syncUiFromManager()
        }

        binding.ivRepeatOne.setOnClickListener {
            PlaybackManager.toggleRepeat()
            updateToggleAlpha()
        }

        setUpGestureDetectors()
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                PlaybackManager.stop()
                finish()
            }
        })
    }

    private fun setUpGestureDetectors() {
        // Left side: rewind 10 seconds
        gestureDetectorLeft = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return true
            }
        })
        gestureDetectorLeft.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                skipBackward()
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return true
            }
        })

        // Right side: forward 10 seconds
        gestureDetectorRight = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return true
            }
        })
        gestureDetectorRight.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                skipForward()
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return true
            }
        })

        binding.doubleTapLeft.setOnTouchListener { _, event ->
            gestureDetectorLeft.onTouchEvent(event)
        }

        binding.doubleTapRight.setOnTouchListener { _, event ->
            gestureDetectorRight.onTouchEvent(event)
        }
    }

    private fun skipForward() {
        val player = exoPlayer ?: return
        val newPosition = (player.currentPosition + 10000).coerceAtMost(player.duration)
        player.seekTo(newPosition)
        syncProgressUi()
    }

    private fun skipBackward() {
        val player = exoPlayer ?: return
        val newPosition = (player.currentPosition - 10000).coerceAtLeast(0L)
        player.seekTo(newPosition)
        syncProgressUi()
    }

    private fun updateToggleAlpha() {
        val shuffleOn = PlaybackManager.isShuffle || (exoPlayer?.shuffleModeEnabled == true)
        binding.ivShuffle.alpha = if (shuffleOn) 1f else 0.45f
        binding.ivRepeatOne.alpha = if (PlaybackManager.isRepeat) 1f else 0.45f
    }

    private fun updateUi(song: Song?) {
        binding.tvTitle.text = song?.title ?: getString(R.string.unknown_title)
        binding.tvArtist.text = song?.artist ?: getString(R.string.unknown_artist)

        val remoteImageUrl = song?.imageUrl
        if (!remoteImageUrl.isNullOrBlank()) {
            loadAlbumArt(remoteImageUrl)
            return
        }

        val albumId = song?.albumId ?: -1L
        if (albumId <= 0 || !hasAlbumArt(albumId)) {
            showDefaultAlbumArt()
            return
        }

        val albumUri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )

        loadAlbumArt(albumUri)
    }

    private fun loadAlbumArt(model: Any) {
        Glide.with(this)
            .load(model)
            .centerCrop()
            .placeholder(R.drawable.album_art_placeholder)
            .error(R.drawable.album_art_placeholder)
            .fallback(R.drawable.album_art_placeholder)
            .listener(albumArtListener())
            .into(binding.ivMusicImage)

        Glide.with(this)
            .load(model)
            .apply(bitmapTransform(BlurTransformation(40, 4)))
            .placeholder(R.color.background_dark)
            .error(R.color.background_dark)
            .into(binding.bgAlbumArt)
    }

    private fun showDefaultAlbumArt() {
        Glide.with(this).clear(binding.ivMusicImage)
        Glide.with(this).clear(binding.bgAlbumArt)
        binding.ivMusicImage.setImageResource(R.drawable.album_art_placeholder)
        binding.bgAlbumArt.setImageResource(R.color.background_dark)
    }

    private fun hasAlbumArt(albumId: Long): Boolean {
        val uri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
        return try {
            contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun albumArtListener() = object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>,
            isFirstResource: Boolean
        ): Boolean {
            binding.ivMusicImage.setImageResource(R.drawable.album_art_placeholder)
            return true
        }

        override fun onResourceReady(
            resource: Drawable,
            model: Any,
            target: Target<Drawable>?,
            dataSource: DataSource,
            isFirstResource: Boolean
        ): Boolean = false
    }

    private fun formatTime(time: Int): String {
        val minutes = time / 60
        val seconds = time % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun updatePlayAndPause() {
        binding.ivPlay.setImageResource(
            if (PlaybackManager.isPlaying()) R.drawable.ic_pause_24
            else R.drawable.ic_play_arrow_24
        )
    }

    private fun Player.usableDuration(): Long {
        val duration = duration
        return if (duration > 0 && duration != C.TIME_UNSET) duration else 0L
    }
}
