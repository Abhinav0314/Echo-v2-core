package com.abhinav.domain.data.model.browse.artist

import com.abhinav.domain.data.model.searchResult.songs.Thumbnail
import com.abhinav.domain.data.type.HomeContentType

data class ResultPlaylist(
    val id: String,
    val author: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
) : HomeContentType