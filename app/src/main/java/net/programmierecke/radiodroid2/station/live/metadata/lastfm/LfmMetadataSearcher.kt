package net.programmierecke.radiodroid2.station.live.metadata.lastfm

import com.google.gson.Gson
import net.programmierecke.radiodroid2.BuildConfig
import net.programmierecke.radiodroid2.station.live.metadata.TrackMetadata
import net.programmierecke.radiodroid2.station.live.metadata.TrackMetadata.AlbumArt
import net.programmierecke.radiodroid2.station.live.metadata.TrackMetadata.AlbumArtSize
import net.programmierecke.radiodroid2.station.live.metadata.TrackMetadataCallback
import net.programmierecke.radiodroid2.station.live.metadata.lastfm.data.LfmTrackMetadata
import net.programmierecke.radiodroid2.utils.RateLimiter
import okhttp3.*
import java.io.IOException
import java.util.*

class LfmMetadataSearcher(private val httpClient: OkHttpClient) {
    private val gson = Gson()
    private val rateLimiter = RateLimiter(4, 60 * 1000)

    /**
     * Some station add to track suffix or prefix, like:
     * "Cool track [Bad station name]"
     * "Cool track (Bad station name)"
     * This makes such track unsearchable, try to remove these modifications and search track without them.
     * It has obvious drawback - some tracks have brackets in their name and such operation could
     * rarely lead to displaying wrong info if original name was unsearchable.
     *
     *
     * Also some stations have different suffix/prefix for different tracks so we cannot be smart and
     * devise from several tracks' names the scheme.
     *
     * @param track Track name as station sent to us
     * @return - null if nothing changed
     * - track name without additional station's suffix/prefix
     */
    private fun tryNormalizeTrack(track: String): String? {
        val normalizedTrack = track
                .replace("\\(.*\\)".toRegex(), "")
                .replace("\\[.*\\]".toRegex(), "")
                .replace("\\*.*\\*".toRegex(), "")
                .trim()
        return if (normalizedTrack == track) null else normalizedTrack
    }

    fun fetchTrackMetadata(artist: String, track: String, trackMetadataCallback: TrackMetadataCallback) {
        if (BuildConfig.LastFMAPIKey.isEmpty() || track.isEmpty()) {
            trackMetadataCallback.onFailure(TrackMetadataCallback.FailureType.UNRECOVERABLE)
            return
        }
        val trimmedArtist = artist.trim()
        val trimmedTrack = track.trim()

        // We want to rate limit calls to Last.fm API to prevent exceeding unknown limits.
        if (rateLimiter.allowed()) {
            httpClient.newCall(buildRequest(trimmedArtist, trimmedTrack))
                    .enqueue(MetadataCallback(trackMetadataCallback, trimmedArtist, trimmedTrack))
        } else {
            trackMetadataCallback.onFailure(TrackMetadataCallback.FailureType.RECOVERABLE)
        }
    }

    private fun buildRequest(artist: String, track: String): Request {
        val url = HttpUrl.Builder().run {
            scheme("https")
            host("ws.audioscrobbler.com")
            addPathSegment("2.0")
            addQueryParameter("method", "track.getInfo")
            addQueryParameter("api_key", BuildConfig.LastFMAPIKey)
            addQueryParameter("artist", artist)
            addQueryParameter("track", track)
            addQueryParameter("format", "json")
            build()
        }
        return Request.Builder().run {
            url(url)
            get()
            build()
        }
    }

    private inner class MetadataCallback(private val trackMetadataCallback: TrackMetadataCallback, private val artist: String, private val track: String) : Callback {
        override fun onFailure(call: Call, e: IOException) =
                trackMetadataCallback.onFailure(TrackMetadataCallback.FailureType.RECOVERABLE)

        @Throws(IOException::class)
        override fun onResponse(call: Call, response: Response) {
            try {
                val lfmTrackMetadata = gson.fromJson(response.body()!!.charStream(), LfmTrackMetadata::class.java)
                val trackMetadata = TrackMetadata()
                val trackData = lfmTrackMetadata.track
                if (trackData == null) {
                    val normalizedTrack = tryNormalizeTrack(track)
                    if (normalizedTrack != null && normalizedTrack.length > 3) {
                        httpClient.newCall(buildRequest(artist, normalizedTrack))
                                .enqueue(MetadataCallback(trackMetadataCallback, artist, normalizedTrack))
                    } else {
                        trackMetadataCallback.onFailure(TrackMetadataCallback.FailureType.UNRECOVERABLE)
                    }
                    return
                }
                if (trackData.artist?.name != null) {
                    trackMetadata.artist = trackData.artist.name
                }
                val albumArts: MutableList<AlbumArt> = ArrayList()
                trackMetadata.albumArts = albumArts
                if (trackData.album != null) {
                    trackMetadata.album = trackData.album.title ?: ""
                    val images = trackData.album.image ?: emptyList()
                    for (img in images) {
                        var artSize = AlbumArtSize.SMALL
                        when (img.size) {
                            "small" -> artSize = AlbumArtSize.SMALL
                            "medium" -> artSize = AlbumArtSize.MEDIUM
                            "large" -> artSize = AlbumArtSize.LARGE
                            "extralarge" -> artSize = AlbumArtSize.EXTRA_LARGE
                        }
                        albumArts.add(AlbumArt(artSize, img.text ?: ""))
                    }
                    albumArts.sortBy { it.size }
                }
                trackMetadata.track = trackData.name ?: ""
                trackMetadataCallback.onSuccess(trackMetadata)
            } catch (ex: Exception) {
                trackMetadataCallback.onFailure(TrackMetadataCallback.FailureType.UNRECOVERABLE)
            }
        }

    }
}
