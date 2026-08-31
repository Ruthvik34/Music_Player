package com.ruthvik.musicplayer.entities

import androidx.room.Entity

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PrivatePlayListSongs(

    val playlistId: String,

    val songId: String,

    val song: String,

    val primary_artists: String,

    val media_url: String,

    val albumid: String,

    val image: String,

    val addedAt: Long = System.currentTimeMillis()
)