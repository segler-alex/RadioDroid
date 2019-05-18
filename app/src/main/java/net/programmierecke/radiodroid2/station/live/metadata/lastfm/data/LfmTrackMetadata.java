
package net.programmierecke.radiodroid2.station.live.metadata.lastfm.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LfmTrackMetadata {

    @SerializedName("track")
    @Expose
    private Track track;

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

}
