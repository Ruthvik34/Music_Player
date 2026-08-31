import com.ruthvik.musicplayer.PlaybackNotificationService



import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ruthvik.musicplayer.entities.Music

object PlaybackManager {

    private var exoPlayer: ExoPlayer? = null
    private var appContext: Context? = null

    private var songList: ArrayList<Music> = arrayListOf()

    var currentIndex: Int = -1
        private set

    var isShuffle: Boolean = false

    var isRepeat: Boolean = false

    private var shouldStayPaused: Boolean = false

    private val uiListeners = mutableSetOf<() -> Unit>()

    private val internalListener = object : Player.Listener {

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            updateCurrentIndexFromPlayer()
            notifyUi()
        }

        override fun onPlaybackStateChanged(
            playbackState: Int
        ) {

            if (playbackState == Player.STATE_READY) {
                updateCurrentIndexFromPlayer()
            }

            notifyUi()
        }

        override fun onIsPlayingChanged(
            isPlaying: Boolean
        ) {
            notifyUi()
        }

        override fun onShuffleModeEnabledChanged(
            shuffleModeEnabled: Boolean
        ) {
            isShuffle = shuffleModeEnabled
            notifyUi()
        }
    }

    fun init(context: Context) {

        appContext = context.applicationContext

        if (exoPlayer == null) {

            exoPlayer =
                ExoPlayer.Builder(
                    context.applicationContext
                )
                    .build()
                    .also { player ->

                        player.addListener(
                            internalListener
                        )

                        player.shuffleModeEnabled =
                            isShuffle

                        applyRepeatMode(player)
                    }
        }
    }

    private fun applyRepeatMode(
        player: ExoPlayer
    ) {

        player.repeatMode =
            if (isRepeat) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_OFF
            }
    }

    private fun updateCurrentIndexFromPlayer() {

        val index =
            exoPlayer?.currentMediaItemIndex
                ?: -1

        if (index >= 0) {
            currentIndex = index
        }
    }

    private fun mediaItemsFrom(
        songs: List<Music>
    ): List<MediaItem> {

        return songs.map { song ->
            MediaItem.fromUri(
                songContentUri(song)
            )
        }
    }

    private fun setPlaylistOnPlayer(
        startIndex: Int,
        startPositionMs: Long = 0L,
        playWhenReady: Boolean = true
    ) {

        val player =
            exoPlayer ?: return

        if (songList.isEmpty()) {
            return
        }

        val index =
            startIndex.coerceIn(
                0,
                songList.lastIndex
            )

        player.shuffleModeEnabled =
            isShuffle

        applyRepeatMode(player)

        player.setMediaItems(
            mediaItemsFrom(songList),
            index,
            startPositionMs
        )

        player.prepare()

        if (
            playWhenReady &&
            !shouldStayPaused
        ) {
            player.play()
        }

        currentIndex = index

        notifyUi()
    }

    fun getPlayer(): ExoPlayer? {
        return exoPlayer
    }

    fun getSongList(): ArrayList<Music> {
        return ArrayList(songList)
    }

    fun currentSong(): Music? {

        val index =
            exoPlayer?.currentMediaItemIndex
                ?: currentIndex

        return songList.getOrNull(index)
    }

    fun hasActiveMedia(): Boolean {

        return songList.isNotEmpty() &&
                (
                        exoPlayer?.playbackState
                            ?: Player.STATE_IDLE
                        ) != Player.STATE_IDLE
    }

    fun shouldShowNotification(): Boolean {

        return hasActiveMedia() &&
                !shouldStayPaused
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying == true
    }

    fun isPlayingSong(
        songId: String?
    ): Boolean {

        return songId != null &&
                currentSong()?.id == songId &&
                isPlaying()
    }

    fun isCurrentSong(
        songId: String?
    ): Boolean {

        return songId != null &&
                currentSong()?.id == songId &&
                hasActiveMedia()
    }

    fun syncCurrentIndex(
        sortedSongs: List<Music>
    ) {

        val playing =
            currentSong()

        val positionMs =
            exoPlayer?.currentPosition
                ?: 0L

        val wasPlaying =
            isPlaying()

        songList =
            ArrayList(sortedSongs)

        val newIndex =
            playing?.let { current ->

                songList.indexOfFirst {
                    it.id == current.id
                }

            } ?: -1

        if (newIndex >= 0) {

            setPlaylistOnPlayer(
                newIndex,
                positionMs,
                playWhenReady = wasPlaying
            )
        }
    }

    fun setPlaylist(
        songs: List<Music>
    ) {

        songList =
            ArrayList(songs)
    }

    /**
     * Updates queue order while keeping
     * the current song and playback position.
     */
    fun updatePlaylistKeepingPlayback(
        songs: List<Music>
    ) {

        val current =
            currentSong()

        val positionMs =
            exoPlayer?.currentPosition
                ?: 0L

        val wasPlaying =
            isPlaying()

        songList =
            ArrayList(songs)

        val index =
            current?.let { currentSong ->

                songList.indexOfFirst {
                    it.id == currentSong.id
                }

            }
                ?: exoPlayer?.currentMediaItemIndex
                ?: 0

        if (
            index >= 0 &&
            songList.isNotEmpty()
        ) {

            setPlaylistOnPlayer(
                index.coerceIn(
                    0,
                    songList.lastIndex
                ),
                positionMs,
                wasPlaying
            )
        }
    }

    fun addUiListener(
        listener: () -> Unit
    ) {

        uiListeners.add(listener)
    }

    fun removeUiListener(
        listener: () -> Unit
    ) {

        uiListeners.remove(listener)
    }

    private fun notifyUi() {

        uiListeners.forEach {
            it.invoke()
        }
    }

    fun play() {

        shouldStayPaused = false

        exoPlayer?.play()

        appContext?.let {
            ensureNotificationService(it)
        }

        notifyUi()
    }

    fun pause() {

        exoPlayer?.pause()

        notifyUi()
    }

    fun stop() {

        exoPlayer?.pause()

        shouldStayPaused = true

        notifyUi()
    }

    fun togglePlayPause() {

        exoPlayer?.let { player ->

            if (player.isPlaying) {

                player.pause()

            } else {

                shouldStayPaused = false

                player.play()

                appContext?.let { context ->
                    ensureNotificationService(
                        context
                    )
                }
            }
        }

        notifyUi()
    }

    fun playAt(
        context: Context,
        listIndex: Int,
        songs: List<Music>,
        restart: Boolean = true
    ) {

        init(context)

        if (listIndex !in songs.indices) {
            return
        }

        songList =
            ArrayList(songs)

        val player =
            exoPlayer ?: return

        val targetSong =
            songs[listIndex]

        val targetIndex =
            songList.indexOfFirst {
                it.id == targetSong.id
            }

        if (targetIndex < 0) {
            return
        }

        val sameSong =
            currentSong()?.id == targetSong.id &&
                    hasActiveMedia()

        if (
            sameSong &&
            !restart
        ) {

            if (!player.isPlaying) {

                shouldStayPaused = false

                player.play()
            }

            ensureNotificationService(context)

            notifyUi()

            return
        }

        shouldStayPaused = false

        val startPosition =
            if (
                sameSong &&
                !restart
            ) {
                player.currentPosition
            } else {
                0L
            }

        setPlaylistOnPlayer(
            targetIndex,
            startPosition,
            playWhenReady = true
        )

        ensureNotificationService(context)
    }

    fun ensureNotificationService(
        context: Context
    ) {

        if (!shouldShowNotification()) {
            return
        }

        val intent =
            Intent(
                context.applicationContext,
                PlaybackNotificationService::class.java
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            ContextCompat.startForegroundService(
                context.applicationContext,
                intent
            )

        } else {

            context.applicationContext.startService(
                intent
            )
        }
    }

    fun playNext() {

        val player =
            exoPlayer ?: return

        if (
            player.hasNextMediaItem()
        ) {

            player.seekToNextMediaItem()

        } else if (
            songList.size > 1
        ) {

            setPlaylistOnPlayer(
                0,
                playWhenReady = true
            )
        }

        updateCurrentIndexFromPlayer()

        notifyUi()
    }

    fun playPrevious() {

        val player =
            exoPlayer ?: return

        if (
            player.currentPosition > 3000
        ) {

            player.seekTo(0)

        } else if (
            player.hasPreviousMediaItem()
        ) {

            player.seekToPreviousMediaItem()

        } else if (
            songList.isNotEmpty()
        ) {

            setPlaylistOnPlayer(
                songList.lastIndex,
                playWhenReady = true
            )
        }

        updateCurrentIndexFromPlayer()

        notifyUi()
    }

    fun toggleShuffle() {

        val player =
            exoPlayer ?: return

        if (songList.isEmpty()) {
            return
        }

        val randomIndex =
            songList.indices.random()

        setPlaylistOnPlayer(
            randomIndex,
            playWhenReady = true
        )

        isShuffle = true

        player.shuffleModeEnabled =
            isShuffle

        notifyUi()
    }

    fun toggleRepeat() {

        isRepeat =
            !isRepeat

        exoPlayer?.let {
            applyRepeatMode(it)
        }

        notifyUi()
    }

    fun seekToProgress(
        progress: Float
    ) {

        val player =
            exoPlayer ?: return

        val duration =
            usableDuration()

        if (duration > 0) {

            player.seekTo(
                (
                        duration *
                                progress /
                                100f
                        ).toLong()
            )

            notifyUi()
        }
    }

    fun usableDuration(): Long {

        val duration =
            exoPlayer?.duration
                ?: return 0L

        return if (
            duration > 0 &&
            duration != C.TIME_UNSET
        ) {
            duration
        } else {
            0L
        }
    }

    private fun songContentUri(
        song: Music
    ): Uri {

        val mediaUrl =
            song.media_url

        if (
            mediaUrl.startsWith("http://") ||
            mediaUrl.startsWith("https://") ||
            mediaUrl.startsWith("content://") ||
            mediaUrl.startsWith("file://")
        ) {

            return Uri.parse(
                mediaUrl
            )
        }

        val localId =
            song.id.toLongOrNull()

        if (localId != null) {

            return ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                localId
            )
        }

        return Uri.parse(
            mediaUrl
        )
    }
}

