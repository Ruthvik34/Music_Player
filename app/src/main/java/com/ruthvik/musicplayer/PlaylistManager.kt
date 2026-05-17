package com.ruthvik.musicplayer

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ruthvik.musicplayer.Database.AppDatabase
import com.ruthvik.musicplayer.Models.PrivatePlayListSongs
import com.ruthvik.musicplayer.Models.Song

data class Playlist(
    val id: String,
    val name: String,
    val songs: MutableList<Long> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
)

object PlaylistManager {

    private const val PREFS_NAME = "music_player_playlists"
    private const val KEY_PLAYLISTS = "playlists"

    private lateinit var prefs: SharedPreferences
    private lateinit var database: AppDatabase
    private val gson = Gson()

    private val listeners = mutableSetOf<() -> Unit>()

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        if (!::database.isInitialized) {
            database = AppDatabase.getInstance(context)
            migrateStoredPlaylistSongsToDatabase()
        }
    }

    fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name.trim()
        )
        val playlists = getAllPlaylists().toMutableList()
        playlists.add(playlist)
        savePlaylists(playlists)
        notifyListeners()
        return playlist
    }

    fun getAllPlaylists(): List<Playlist> {
        return readStoredPlaylists().withDatabaseSongs()
    }

    private fun readStoredPlaylists(): List<Playlist> {
        if (!::prefs.isInitialized) return emptyList()
        val json = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Playlist>>() {}.type
            gson.fromJson<List<Playlist>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deletePlaylist(playlistId: String) {
        val playlists = getAllPlaylists().toMutableList()
        playlists.removeAll { it.id == playlistId }
        if (::database.isInitialized) {
            database.songDao().deletePlayListSongs(playlistId)
        }
        savePlaylists(playlists)
        notifyListeners()
    }

    fun getPlaylistById(playlistId: String): Playlist? {
        return getAllPlaylists().find { it.id == playlistId }
    }

    fun addSongToPlaylist(playlistId: String, songId: Long): Boolean {
        val playlists = getAllPlaylists().toMutableList()
        val playlist = playlists.find { it.id == playlistId } ?: return false

        if (!playlist.songs.contains(songId)) {
            playlist.songs.add(songId)
            if (::database.isInitialized) {
                database.songDao().addToPlayList(
                    PrivatePlayListSongs(
                        playlistId = playlistId,
                        songId = songId,
                        title = "",
                        artist = "",
                        data = "",
                        albumId = 0L
                    )
                )
            }
            savePlaylists(playlists)
            notifyListeners()
        }
        return true
    }

    fun addSongToPlaylist(playlistId: String, song: Song): Boolean {
        val playlists = getAllPlaylists().toMutableList()
        val playlist = playlists.find { it.id == playlistId } ?: return false

        if (::database.isInitialized) {
            database.songDao().addToPlayList(
                PrivatePlayListSongs(
                    playlistId = playlistId,
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    data = song.data,
                    albumId = song.albumId,
                    imageUrl = song.imageUrl
                )
            )
        }

        if (!playlist.songs.contains(song.id)) {
            playlist.songs.add(song.id)
            savePlaylists(playlists)
            notifyListeners()
        }
        return true
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long): Boolean {
        val playlists = getAllPlaylists().toMutableList()
        val playlist = playlists.find { it.id == playlistId } ?: return false

        if (::database.isInitialized) {
            database.songDao().deleteFromPlayList(playlistId, songId)
        }

        if (playlist.songs.remove(songId)) {
            savePlaylists(playlists)
            notifyListeners()
        }
        return true
    }

    fun getSongsInPlaylist(playlistId: String): List<Long> {
        if (::database.isInitialized) {
            return database.songDao().getSongIdsFromPlayList(playlistId)
        }
        return getPlaylistById(playlistId)?.songs ?: emptyList()
    }

    fun getPlaylistSongDetails(playlistId: String): List<PrivatePlayListSongs> {
        if (!::database.isInitialized) return emptyList()
        return database.songDao().getSongsFromPlayList(playlistId)
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        if (!::prefs.isInitialized) return
        val json = gson.toJson(playlists)
        prefs.edit().putString(KEY_PLAYLISTS, json).apply()
    }

    private fun migrateStoredPlaylistSongsToDatabase() {
        readStoredPlaylists().forEach { playlist ->
            playlist.songs.forEach { songId ->
                database.songDao().addToPlayList(
                    PrivatePlayListSongs(
                        playlistId = playlist.id,
                        songId = songId,
                        title = "",
                        artist = "",
                        data = "",
                        albumId = 0L
                    )
                )
            }
        }
    }

    private fun List<Playlist>.withDatabaseSongs(): List<Playlist> {
        if (!::database.isInitialized) return this
        return map { playlist ->
            playlist.copy(
                songs = database.songDao()
                    .getSongIdsFromPlayList(playlist.id)
                    .toMutableList()
            )
        }
    }
}

