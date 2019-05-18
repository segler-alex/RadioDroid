package net.programmierecke.radiodroid2.station.live;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.Map;

public class StreamLiveInfo implements Parcelable {

    public StreamLiveInfo(Map<String, String> rawMetadata) {
        this.rawMetadata = rawMetadata;

        if (rawMetadata != null && rawMetadata.containsKey("StreamTitle")) {
            title = rawMetadata.get("StreamTitle");

            if (!TextUtils.isEmpty(title)) {
                String[] artistAndTrack = title.split(" - ", 2);
                artist = artistAndTrack[0];
                if (artistAndTrack.length == 2) {
                    track = artistAndTrack[1];
                }
            }
        }
    }

    public @NonNull
    String getTitle() {
        return title;
    }

    public
    boolean hasArtistAndTrack() {
        return ! (artist.isEmpty() || track.isEmpty());
    }

    public @NonNull
    String getArtist() {
        return artist;
    }

    public @NonNull
    String getTrack() {
        return track;
    }

    public Map<String, String> getRawMetadata() {
        return rawMetadata;
    }

    private String title = "";
    private String artist = "";
    private String track = "";
    private Map<String, String> rawMetadata;

    protected StreamLiveInfo(Parcel in) {
        title = in.readString();
        artist = in.readString();
        track = in.readString();
        in.readMap(rawMetadata, String.class.getClassLoader());
    }

    public static final Creator<StreamLiveInfo> CREATOR = new Creator<StreamLiveInfo>() {
        @Override
        public StreamLiveInfo createFromParcel(Parcel in) {
            return new StreamLiveInfo(in);
        }

        @Override
        public StreamLiveInfo[] newArray(int size) {
            return new StreamLiveInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeString(artist);
        parcel.writeString(track);
        parcel.writeMap(rawMetadata);
    }
}
