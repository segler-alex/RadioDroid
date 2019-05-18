package net.programmierecke.radiodroid2.station.live.metadata;


import androidx.annotation.NonNull;

public interface TrackMetadataCallback {
    void onFailure();
    void onSuccess(@NonNull TrackMetadata trackMetadata);
}
