package com.maxrave.data.echo

import club.vibescape.echo.contracts.*
import com.maxrave.domain.data.entities.*
import com.maxrave.domain.data.entities.analytics.PlaybackEventEntity
import kotlinx.serialization.json.*

fun SongEntity.echoTrack(): JsonObject = buildJsonObject {
    put("videoId",videoId);put("title",title);put("durationSeconds",durationSeconds);put("thumbnail",thumbnails)
    put("albumId",albumId);put("albumName",albumName);put("explicit",isExplicit);put("videoType",videoType)
    putJsonArray("artists") { artistName.orEmpty().forEachIndexed { i,name -> add(buildJsonObject { put("id",artistId?.getOrNull(i));put("name",name) }) } }
}
fun SongEntity.echoLike(deleted:Boolean=false) = LibraryRecord(LibraryKind.SONG_LIKE,"youtube:$videoId",payload=buildJsonObject { put("track",echoTrack());put("favoriteAt",favoriteAt?.toString()) },deleted=deleted)
fun AlbumEntity.echoLike(deleted:Boolean=false) = LibraryRecord(LibraryKind.ALBUM_LIKE,"youtube:$browseId",payload=buildJsonObject {
    put("id",browseId);put("title",title);put("thumbnail",thumbnails);put("audioPlaylistId",audioPlaylistId);put("description",description)
    put("durationSeconds",durationSeconds);put("type",type);put("year",year);put("favoriteAt",favoriteAt?.toString())
    putJsonArray("tracks") { tracks.orEmpty().forEach{add(it)} }
    putJsonArray("artists") { artistName.orEmpty().forEachIndexed{i,name->add(buildJsonObject{put("id",artistId?.getOrNull(i));put("name",name)})} }
},deleted=deleted)
fun ArtistEntity.echoFollow(deleted:Boolean=false) = LibraryRecord(LibraryKind.ARTIST_FOLLOW,"youtube:$channelId",payload=buildJsonObject{put("id",channelId);put("name",name);put("thumbnail",thumbnails);put("followedAt",followedAt?.toString())},deleted=deleted)
fun PlaylistEntity.echoLike(deleted:Boolean=false) = LibraryRecord(LibraryKind.PLAYLIST_LIKE,"youtube:$id",payload=buildJsonObject {
    put("id",id);put("title",title);put("thumbnail",thumbnails);put("author",author);put("description",description);put("durationSeconds",durationSeconds);put("favoriteAt",favoriteAt?.toString())
    putJsonArray("tracks"){tracks.orEmpty().forEach{add(it)}}
},deleted=deleted)
fun LocalPlaylistEntity.echoPlaylist(key:String,deleted:Boolean=false) = LibraryRecord(LibraryKind.PLAYLIST,key,payload=buildJsonObject {
    put("title",title);put("thumbnail",thumbnail)
    putJsonObject("_local") {put("downloadState",downloadState);put("downloadedAt",downloadedAt?.toString());put("youtubePlaylistId",youtubePlaylistId);put("syncState",syncState)}
},deleted=deleted)
fun PairSongLocalPlaylist.echoEntry(key:String,parent:String,track:SongEntity,deleted:Boolean=false) = LibraryRecord(LibraryKind.ENTRY,key,parent,payload=buildJsonObject {put("track",track.echoTrack());put("position",position);put("addedAt",inPlaylist.toString())},deleted=deleted)
fun PlaybackEventEntity.echoHistory(key:String,channelIds:List<String>,track:SongEntity?=null) = LibraryRecord(LibraryKind.HISTORY,key,payload=buildJsonObject {
    put("videoId",videoId);put("timestamp",timestamp.toString());put("albumBrowseId",albumBrowseId);put("durationSecond",durationSecond);put("listenedSecond",listenedSecond)
    putJsonArray("channelIds"){channelIds.distinct().forEach{add(it)}}
    track?.let{put("track",it.echoTrack())}
})
suspend fun echoOrder(playlistId:Long,pairs:List<PairSongLocalPlaylist>): LibraryRecord {
    val key=EchoLibraryHooks.identity("playlist",playlistId)
    val entries = pairs.map{JsonPrimitive(EchoLibraryHooks.identity("entry",it.id.toLong()))}
    return LibraryRecord(LibraryKind.ORDER,key,key,payload=buildJsonObject {
        put("entries",JsonArray(entries))
    })
}
