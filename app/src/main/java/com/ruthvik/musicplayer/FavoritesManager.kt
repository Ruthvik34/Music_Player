package com.ruthvik.musicplayer

import com.ruthvik.musicplayer.entities.Music



import android.content.Context
import android.content.SharedPreferences

object FavoritesManager {

    private const val PREFS_NAME = "music_player_favorites"
    private const val KEY_LIKED_IDS = "liked_song_ids"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
        }
    }

    fun isLiked(songId: String?): Boolean {
        if (songId.isNullOrBlank()) return false

        return getLikedIds().contains(songId)
    }

    fun toggleLike(songId: String?): Boolean {

        if (songId.isNullOrBlank()) {
            return false
        }

        val liked =
            getLikedIds().toMutableSet()

        val nowLiked =
            if (liked.contains(songId)) {

                liked.remove(songId)
                false

            } else {

                liked.add(songId)
                true
            }

        prefs.edit()
            .putStringSet(
                KEY_LIKED_IDS,
                liked
            )
            .apply()

        return nowLiked
    }

    fun sortSongsLikedFirst(
        songs: List<Music>
    ): List<Music> {

        val liked =
            getLikedIds()

        return songs.sortedWith(

            compareByDescending<Music> {
                liked.contains(it.id)
            }.thenBy {

                it.song
                    .orEmpty()
                    .lowercase()
            }
        )
    }

    private fun getLikedIds(): Set<String> {

        if (!::prefs.isInitialized) {
            return emptySet()
        }

        return prefs.getStringSet(
            KEY_LIKED_IDS,
            emptySet()
        ) ?: emptySet()
    }
}
