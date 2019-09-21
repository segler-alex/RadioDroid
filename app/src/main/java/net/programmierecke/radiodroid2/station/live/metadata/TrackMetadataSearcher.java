package net.programmierecke.radiodroid2.station.live.metadata;

import androidx.annotation.NonNull;

import net.programmierecke.radiodroid2.station.live.metadata.lastfm.LfmMetadataSearcher;

import okhttp3.OkHttpClient;

public class TrackMetadataSearcher {
    private LfmMetadataSearcher lfmMetadataSearcher;

    public TrackMetadataSearcher(OkHttpClient httpClient) {
        lfmMetadataSearcher = new LfmMetadataSearcher(httpClient);
    }


    public void fetchTrackMetadata(String artist, @NonNull String track, @NonNull TrackMetadataCallback trackMetadataCallback) {
        lfmMetadataSearcher.fetchTrackMetadata(artist, track, trackMetadataCallback);
    }
}
