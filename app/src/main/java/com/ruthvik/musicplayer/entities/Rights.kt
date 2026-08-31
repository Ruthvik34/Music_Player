package com.ruthvik.musicplayer.entities

data class Rights(
    val cacheable: Boolean,
    val code: Int,
    val delete_cached_object: Boolean,
    val reason: String
)