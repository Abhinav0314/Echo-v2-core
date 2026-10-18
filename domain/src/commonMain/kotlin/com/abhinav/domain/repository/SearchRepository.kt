package com.abhinav.domain.repository

import com.abhinav.domain.data.entities.SearchHistory
import com.abhinav.domain.data.model.searchResult.SearchSuggestions
import com.abhinav.domain.data.model.searchResult.albums.AlbumsResult
import com.abhinav.domain.data.model.searchResult.artists.ArtistsResult
import com.abhinav.domain.data.model.searchResult.playlists.PlaylistsResult
import com.abhinav.domain.data.model.searchResult.songs.SongsResult
import com.abhinav.domain.data.model.searchResult.videos.VideosResult
import com.abhinav.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun getSearchHistory(): Flow<List<SearchHistory>>

    fun insertSearchHistory(searchHistory: SearchHistory): Flow<Long>

    suspend fun deleteSearchHistory()

    fun getSearchDataSong(query: String): Flow<Resource<ArrayList<SongsResult>>>

    fun getSearchDataVideo(query: String): Flow<Resource<ArrayList<VideosResult>>>

    fun getSearchDataPodcast(query: String): Flow<Resource<ArrayList<PlaylistsResult>>>

    fun getSearchDataFeaturedPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>>

    fun getSearchDataArtist(query: String): Flow<Resource<ArrayList<ArtistsResult>>>

    fun getSearchDataAlbum(query: String): Flow<Resource<ArrayList<AlbumsResult>>>

    fun getSearchDataPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>>

    fun getSuggestQuery(query: String): Flow<Resource<SearchSuggestions>>
}