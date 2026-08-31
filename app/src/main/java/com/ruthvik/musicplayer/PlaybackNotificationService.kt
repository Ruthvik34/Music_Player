
package com.ruthvik.musicplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class PlaybackNotificationService : Service() {

    private companion object {

        const val CHANNEL_ID =
            "music_playback"

        const val NOTIFICATION_ID =
            34

        const val ACTION_PLAY_PAUSE =
            "com.ruthvik.musicplayer.PLAY_PAUSE"

        const val ACTION_PREVIOUS =
            "com.ruthvik.musicplayer.PREVIOUS"

        const val ACTION_NEXT =
            "com.ruthvik.musicplayer.NEXT"

        const val ACTION_STOP =
            "com.ruthvik.musicplayer.STOP"
    }

    private val playbackListener: () -> Unit = {

        if (
            PlaybackManager.shouldShowNotification()
        ) {

            showNotification()

        } else {

            stopForeground(
                STOP_FOREGROUND_REMOVE
            )

            stopSelf()
        }
    }

    override fun onCreate() {

        super.onCreate()

        createNotificationChannel()

        PlaybackManager.addUiListener(
            playbackListener
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_PLAY_PAUSE -> {

                PlaybackManager.togglePlayPause()
            }

            ACTION_PREVIOUS -> {

                PlaybackManager.playPrevious()
            }

            ACTION_NEXT -> {

                PlaybackManager.playNext()
            }

            ACTION_STOP -> {

                PlaybackManager.stop()

                stopForeground(
                    STOP_FOREGROUND_REMOVE
                )

                stopSelf()

                return START_NOT_STICKY
            }
        }

        if (
            PlaybackManager.shouldShowNotification()
        ) {

            showNotification()

        } else {

            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {

        PlaybackManager.removeUiListener(
            playbackListener
        )

        super.onDestroy()
    }

    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {

        PlaybackManager.stop()

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )

        stopSelf()

        super.onTaskRemoved(
            rootIntent
        )
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    private fun showNotification() {

        val notification =
            buildNotification()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            startForeground(

                NOTIFICATION_ID,

                notification,

                ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun buildNotification(): Notification {

        val song =
            PlaybackManager.currentSong()

        val playPauseIcon =

            if (
                PlaybackManager.isPlaying()
            ) {

                R.drawable.ic_pause_24

            } else {

                R.drawable.ic_play_arrow_24
            }

        val playPauseText =

            if (
                PlaybackManager.isPlaying()
            ) {

                getString(
                    R.string.pause_song
                )

            } else {

                getString(
                    R.string.play_song
                )
            }

        val openPlayerIntent =
            Intent(
                this,
                PlayerActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val contentIntent =
            PendingIntent.getActivity(

                this,

                0,

                openPlayerIntent,

                pendingIntentFlags()
            )

        return NotificationCompat
            .Builder(
                this,
                CHANNEL_ID
            )

            .setSmallIcon(
                R.drawable.music
            )

            /*
             * New Music POJO
             */
            .setContentTitle(
                song?.song
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: getString(
                        R.string.app_name
                    )
            )

            /*
             * New Music POJO
             */
            .setContentText(
                song?.primary_artists
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: getString(
                        R.string.now_playing
                    )
            )

            .setContentIntent(
                contentIntent
            )

            .setVisibility(
                NotificationCompat
                    .VISIBILITY_PUBLIC
            )

            .setOnlyAlertOnce(
                true
            )

            .setOngoing(
                PlaybackManager.isPlaying()
            )

            .addAction(
                R.drawable.ic_skip_previous_24,
                getString(
                    R.string.previous_song
                ),
                serviceIntent(
                    ACTION_PREVIOUS,
                    1
                )
            )

            .addAction(
                playPauseIcon,
                playPauseText,
                serviceIntent(
                    ACTION_PLAY_PAUSE,
                    2
                )
            )

            .addAction(
                R.drawable.ic_skip_next_24,
                getString(
                    R.string.next_song
                ),
                serviceIntent(
                    ACTION_NEXT,
                    3
                )
            )

            .addAction(
                R.drawable.ic_close_24,
                getString(
                    R.string.stop_song
                ),
                serviceIntent(
                    ACTION_STOP,
                    4
                )
            )

            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )

            .build()
    }

    private fun serviceIntent(
        action: String,
        requestCode: Int
    ): PendingIntent {

        val intent =
            Intent(
                this,
                PlaybackNotificationService::class.java
            ).apply {

                this.action = action
            }

        return PendingIntent.getService(

            this,

            requestCode,

            intent,

            pendingIntentFlags()
        )
    }

    private fun pendingIntentFlags(): Int {

        return PendingIntent.FLAG_UPDATE_CURRENT or

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.M
                ) {

                    PendingIntent.FLAG_IMMUTABLE

                } else {

                    0
                }
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        val channel =
            NotificationChannel(

                CHANNEL_ID,

                getString(
                    R.string.playback_notification_channel
                ),

                NotificationManager
                    .IMPORTANCE_LOW

            ).apply {

                description =
                    getString(
                        R.string
                            .playback_notification_channel_description
                    )

                setShowBadge(
                    false
                )
            }

        getSystemService(
            NotificationManager::class.java
        ).createNotificationChannel(
            channel
        )
    }
}

