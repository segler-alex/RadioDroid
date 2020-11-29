package net.programmierecke.radiodroid2.station.live.metadata

import net.programmierecke.radiodroid2.station.live.metadata.lastfm.LfmMetadataSearcher
import okhttp3.OkHttpClient

class TrackMetadataSearcher(httpClient: OkHttpClient) {
    private val metadataSearcher: LfmMetadataSearcher = LfmMetadataSearcher(httpClient)
    fun fetchTrackMetadata(artist: String, track: String, trackMetadataCallback: TrackMetadataCallback) =
            metadataSearcher.fetchTrackMetadata(artist, track, trackMetadataCallback)
}
