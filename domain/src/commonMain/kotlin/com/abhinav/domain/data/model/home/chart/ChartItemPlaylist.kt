package com.abhinav.domain.data.model.home.chart

import com.abhinav.domain.data.model.browse.artist.ResultPlaylist

data class ChartItemPlaylist(
    val title: String,
    val playlists: List<ResultPlaylist>,
)