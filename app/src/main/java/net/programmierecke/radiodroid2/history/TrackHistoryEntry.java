package net.programmierecke.radiodroid2.history;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "track_history")
public class TrackHistoryEntry {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "station_uuid")
    @NonNull
    public String stationUuid;

    @ColumnInfo(name = "station_icon_url")
    @NonNull
    public String stationIconUrl;

    @ColumnInfo(name = "track")
    @NonNull
    public String track;

    @ColumnInfo(name = "artist")
    @NonNull
    public String artist;

    @ColumnInfo(name = "title")
    @NonNull
    public String title;

    @ColumnInfo(name = "art_url")
    @Nullable
    public String artUrl;

    @ColumnInfo(name = "start_time")
    @NonNull
    public Date startTime;

    @ColumnInfo(name = "end_time")
    @NonNull
    public Date endTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackHistoryEntry that = (TrackHistoryEntry) o;

        if (uid != that.uid) return false;
        if (!stationUuid.equals(that.stationUuid)) return false;
        if (!track.equals(that.track)) return false;
        if (!artist.equals(that.artist)) return false;
        if (!title.equals(that.title)) return false;
        if (artUrl != null ? !artUrl.equals(that.artUrl) : that.artUrl != null) return false;
        if (!startTime.equals(that.startTime)) return false;
        return endTime.equals(that.endTime);
    }

    @Override
    public int hashCode() {
        int result = uid;
        result = 31 * result + stationUuid.hashCode();
        result = 31 * result + track.hashCode();
        result = 31 * result + artist.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + (artUrl != null ? artUrl.hashCode() : 0);
        result = 31 * result + startTime.hashCode();
        result = 31 * result + endTime.hashCode();
        return result;
    }

    public final static int MAX_HISTORY_ITEMS_IN_TABLE = 1000;
    public final static int MAX_UNKNOWN_TRACK_DURATION = 3 * 60 * 1000; // 3 minutes
}
