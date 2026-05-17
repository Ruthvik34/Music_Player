package com.ruthvik.musicplayer.Models

import androidx.room.Entity

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PrivatePlayListSongs(
    val playlistId: String,
    val songId: Long,
    val title: String,
    val artist: String,
    val data: String,
    val albumId: Long,
    val imageUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
