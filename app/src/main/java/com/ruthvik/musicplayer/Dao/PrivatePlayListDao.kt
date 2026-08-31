package com.ruthvik.musicplayer.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruthvik.musicplayer.entities.PrivatePlayListSongs

@Dao
interface PrivatePlayListDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addToPlayList(song: PrivatePlayListSongs)

    @Query("""
        SELECT *
        FROM playlist_songs
        WHERE playlistId = :playlistId
        ORDER BY addedAt
    """)
    fun getSongsFromPlayList(
        playlistId: String
    ): List<PrivatePlayListSongs>

    @Query("""
        SELECT songId
        FROM playlist_songs
        WHERE playlistId = :playlistId
        ORDER BY addedAt
    """)
    fun getSongIdsFromPlayList(
        playlistId: String
    ): List<String>

    @Query("""
        DELETE FROM playlist_songs
        WHERE playlistId = :playlistId
        AND songId = :songId
    """)
    fun deleteFromPlayList(
        playlistId: String,
        songId: String
    )

    @Query("""
        DELETE FROM playlist_songs
        WHERE playlistId = :playlistId
    """)
    fun deletePlayListSongs(
        playlistId: String
    )
}