package com.abhinav.kotlinytmusicscraper.pages

import com.abhinav.kotlinytmusicscraper.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)