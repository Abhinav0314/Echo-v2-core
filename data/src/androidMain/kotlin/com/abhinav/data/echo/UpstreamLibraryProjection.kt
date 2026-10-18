package com.abhinav.data.echo

import DatabaseDao
import android.content.Context
import club.vibescape.echo.contracts.*
import club.vibescape.echo.librarysync.*
import com.abhinav.domain.data.entities.*
import com.abhinav.domain.data.entities.analytics.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.*
import org.koin.core.module.Module
import org.koin.dsl.module

/** This is the only sync module adapter that knows the pinned upstream Room models. */
class UpstreamLibraryProjection(private val dao:DatabaseDao):LibraryProjection {
    override suspend fun snapshotGuest(store:CanonicalLibraryStore):List<LibraryRecord> {
        val owner=EchoLibrarySync.GUEST
        val records=mutableListOf<LibraryRecord>()
        records+=dao.getLikedSongs(Int.MAX_VALUE,0).map{it.echoLike()}
        records+=dao.getLikedAlbums(Int.MAX_VALUE,0).map{it.echoLike()}
        records+=dao.getLikedPlaylists(Int.MAX_VALUE,0).map{it.echoLike()}
        records+=dao.getFollowedArtists(Int.MAX_VALUE,0).map{it.echoFollow()}
        dao.echoRawGetAllLocalPlaylists(Int.MAX_VALUE,0).forEach { playlist ->
            val key=store.identity(owner,"playlist",playlist.id)
            records+=playlist.echoPlaylist(key)
            dao.echoRawGetAllPlaylistPairSongByPosition(playlist.id).forEach { pair ->
                dao.getSong(pair.songId)?.let{records+=pair.echoEntry(store.identity(owner,"entry",pair.id.toLong()),key,it)}
            }
        }
        dao.getPlaybackEventsByOffset(0,Int.MAX_VALUE).forEach { event ->
            records+=event.echoHistory(store.identity(owner,"history",event.eventId),dao.echoHistoryArtistIds(event.eventId),dao.getSong(event.videoId))
        }
        return records
    }

    override suspend fun apply(owner:String,records:List<LibraryRecord>,store:CanonicalLibraryStore,accountChanged:Boolean) {
        dao.echoApplyProjection {
            // This is a snapshot projection of canonical intent, never a cloud union. Cached
            // song metadata/downloads/Canvas are untouched; inactive-account playlists stay on
            // disk but are hidden by DAO identity filtering instead of being destroyed.
            dao.echoClearSongLikes();dao.echoClearAlbumLikes();dao.echoClearPlaylistLikes();dao.echoClearArtistFollows()
            records.filter{!it.deleted}.forEach { record ->
                val p=record.payload
                when(record.kind) {
                    LibraryKind.SONG_LIKE -> {
                        val song=track(p["track"] as? JsonObject ?: p["track_data"] as? JsonObject ?: p,record.key.removePrefix("youtube:"))
                        if(song.videoId.isNotBlank()) {dao.echoRawInsertSong(song);dao.echoRawUpdateLiked(1,song.videoId,p.date("favoriteAt"))}
                    }
                    LibraryKind.ARTIST_FOLLOW -> {
                        val id=p.text("id",record.key.removePrefix("youtube:"))
                        dao.echoRawInsertArtist(ArtistEntity(id,p.text("name"),p.optional("thumbnail")))
                        dao.echoRawUpdateFollowed(1,id,p.date("followedAt"))
                    }
                    LibraryKind.ALBUM_LIKE -> {
                        val id=p.text("id",record.key.removePrefix("youtube:"))
                        val artists=p.artists()
                        dao.echoRawInsertAlbum(AlbumEntity(id,artists.map{it.first},artists.map{it.second},p.text("audioPlaylistId"),p.text("description"),null,p.number("durationSeconds"),p.optional("thumbnail"),p.text("title"),p.strings("tracks").size,p.strings("tracks"),p.text("type","Album"),p.optional("year")))
                        dao.echoRawUpdateAlbumLiked(1,id,p.date("favoriteAt"))
                    }
                    LibraryKind.PLAYLIST_LIKE -> {
                        val id=p.text("id",record.key.removePrefix("youtube:"))
                        dao.echoRawInsertPlaylist(PlaylistEntity(id=id,title=p.text("title"),author=p.optional("author"),description=p.text("description"),thumbnails=p.text("thumbnail"),durationSeconds=p.number("durationSeconds"),tracks=p.strings("tracks"),trackCount=p.strings("tracks").size))
                        dao.echoRawUpdatePlaylistLiked(1,id,p.date("favoriteAt"))
                    }
                    LibraryKind.PLAYLIST -> {
                        val id=store.localId(owner,"playlist",record.key,dao.echoNextPlaylistId())
                        val old=dao.echoRawGetLocalPlaylist(id)
                        if(old==null) {
                            val local=p["_local"] as? JsonObject
                            dao.echoRawInsertLocalPlaylist(LocalPlaylistEntity(id=id,title=p.text("title",p.text("name")),thumbnail=p.optional("thumbnail") ?: p.optional("cover_image"),downloadState=local?.number("downloadState") ?: 0,downloadedAt=local?.optional("downloadedAt")?.let{runCatching{LocalDateTime.parse(it)}.getOrNull()}))
                        } else {
                            dao.echoRawUpdateLocalPlaylistTitle(p.text("title",p.text("name")),id)
                            dao.echoRawUpdateLocalPlaylistThumbnail(p.optional("thumbnail") ?: p.optional("cover_image") ?: "",id)
                        }
                    }
                    else -> Unit
                }
            }
            // Replace only each active account's membership projection; entry UUIDs preserve
            // duplicate songs. Integer positions are derived from authoritative order IDs.
            records.filter{it.kind==LibraryKind.PLAYLIST}.forEach { playlist ->
                val id=store.localId(owner,"playlist",playlist.key,dao.echoNextPlaylistId())
                if(playlist.deleted) {dao.echoRawDeleteLocalPlaylist(id);return@forEach}
                val order=records.firstOrNull{it.kind==LibraryKind.ORDER&&it.key==playlist.key&&!it.deleted}?.payload?.strings("entries").orEmpty()
                val entries=records.filter{it.kind==LibraryKind.ENTRY&&it.parentKey==playlist.key&&!it.deleted}
                    .sortedWith(compareBy<LibraryRecord> { order.indexOf(it.key).takeIf{index->index>=0} ?: Int.MAX_VALUE }.thenBy{it.payload.number("position")}.thenBy{it.key})
                dao.echoClearPlaylistEntries(id)
                val tracks=mutableListOf<String>()
                entries.forEachIndexed { index,entry ->
                    val song=track(entry.payload["track"] as? JsonObject ?: entry.payload["track_data"] as? JsonObject ?: entry.payload)
                    if(song.videoId.isBlank())return@forEachIndexed
                    dao.echoRawInsertSong(song)
                    val pairId=store.localId(owner,"entry",entry.key,dao.echoNextEntryId()).toInt()
                    dao.echoRawInsertPairSongLocalPlaylist(PairSongLocalPlaylist(pairId,id,song.videoId,index,
                        entry.payload.date("addedAt") ?: LocalDateTime(1970,1,1,0,0)))
                    tracks+=song.videoId
                }
                dao.echoRawUpdateLocalPlaylistTracks(tracks,id)
            }
            // History is canonical per account. Rebuild the projection to apply clear/pruning
            // tombstones without duplicating events or changing download/listen cache fields.
            dao.echoRawDeleteAllPlaybackEvents()
            records.filter{it.kind==LibraryKind.HISTORY&&!it.deleted}.forEach { record ->
                val p=record.payload
                (p["track"] as? JsonObject)?.let { metadata ->
                    val song=track(metadata,p.text("videoId"))
                    if(song.videoId.isNotBlank()) {
                        dao.echoRawInsertSong(song)
                        metadata.artists().filter{!it.first.isNullOrBlank()}.forEach { artist ->
                            dao.echoRawInsertArtist(ArtistEntity(artist.first!!,artist.second,null))
                        }
                    }
                }
                val id=store.localId(owner,"history",record.key,dao.echoNextHistoryId())
                val timestamp=p.date("timestamp") ?: LocalDateTime(1970,1,1,0,0)
                dao.insertPlaybackEvent(PlaybackEventEntity(id,timestamp,p.text("videoId",p.text("video_id")),p.optional("albumBrowseId") ?: p.optional("album_browse_id"),p.long("durationSecond",p.long("duration_second")),p.long("listenedSecond",p.long("listened_second"))))
                p.strings("channelIds").distinct().forEach { channel -> dao.insertEventArtist(EventArtistEntity(id,channel,timestamp)) }
            }
        }
    }
    private fun track(p:JsonObject,fallbackVideoId:String=""):SongEntity {
        val artists=p.artists().ifEmpty { listOf(p.optional("artistId") to p.text("artist","Unknown artist")) }
        val seconds=p.number("durationSeconds",p.number("duration"))
        return SongEntity(videoId=p.text("videoId",p.text("id",fallbackVideoId)),title=p.text("title",fallbackVideoId),artistName=artists.map{it.second},artistId=artists.map{it.first ?: ""},albumId=p.optional("albumId"),albumName=p.optional("albumName") ?: p.optional("album"),durationSeconds=seconds,duration="${seconds/60}:${(seconds%60).toString().padStart(2,'0')}",isAvailable=true,isExplicit=(p["explicit"] as? JsonPrimitive)?.booleanOrNull ?: false,likeStatus="INDIFFERENT",thumbnails=p.optional("thumbnail") ?: p.optional("coverArt"),videoType=p.text("videoType","MUSIC_VIDEO_TYPE_ATV"),category=null,resultType=null)
    }
}

private fun JsonObject.text(key:String,fallback:String="")=(this[key] as? JsonPrimitive)?.contentOrNull ?: fallback
private fun JsonObject.optional(key:String)=(this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.date(key:String)=optional(key)?.let { value ->
    runCatching { LocalDateTime.parse(value) }.getOrNull()
        ?: runCatching { kotlin.time.Instant.parse(value).toLocalDateTime(TimeZone.currentSystemDefault()) }.getOrNull()
}
private fun JsonObject.number(key:String,fallback:Int=0)=(this[key] as? JsonPrimitive)?.intOrNull ?: fallback
private fun JsonObject.long(key:String,fallback:Long=0)=(this[key] as? JsonPrimitive)?.longOrNull ?: fallback
private fun JsonObject.strings(key:String)=(this[key] as? JsonArray)?.mapNotNull{(it as? JsonPrimitive)?.contentOrNull}.orEmpty()
private fun JsonObject.artists()=(this["artists"] as? JsonArray)?.mapNotNull{ item -> (item as? JsonObject)?.let{it.optional("id") to it.text("name")} }.orEmpty()

fun echoLibrarySyncModule(context:Context):Module=module {
    single(createdAtStart=true) { EchoLibrarySync(context.applicationContext,get(),get(),UpstreamLibraryProjection(get())) }
    single<EchoLibrarySyncControl> { get<EchoLibrarySync>() }
}
