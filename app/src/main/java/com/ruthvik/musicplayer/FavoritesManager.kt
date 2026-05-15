package com.ruthvik.musicplayer

import android.content.Context
import android.content.SharedPreferences
import com.ruthvik.musicplayer.Models.Song

object FavoritesManager {

    private const val PREFS_NAME = "music_player_favorites"
    private const val KEY_LIKED_IDS = "liked_song_ids"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun isLiked(songId: Long): Boolean = getLikedIds().contains(songId)

    fun toggleLike(songId: Long): Boolean {
        val liked = getLikedIds().toMutableSet()
        val nowLiked = if (liked.contains(songId)) {
            liked.remove(songId)
            false
        } else {
            liked.add(songId)
            true
        }
        prefs.edit().putStringSet(KEY_LIKED_IDS, liked.map { it.toString() }.toSet()).apply()
        return nowLiked
    }

    fun sortSongsLikedFirst(songs: List<Song>): List<Song> {
        val liked = getLikedIds()
        return songs.sortedWith(
            compareByDescending<Song> { liked.contains(it.id) }
                .thenBy { it.title.lowercase() }
        )
    }

    private fun getLikedIds(): Set<Long> {
        if (!::prefs.isInitialized) return emptySet()
        return prefs.getStringSet(KEY_LIKED_IDS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }
}
