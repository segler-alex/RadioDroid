package net.programmierecke.radiodroid2.history;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

@Dao
public interface TrackHistoryDao {
    @Insert
    void insert(TrackHistoryEntry historyEntry);

    @Update
    void update(TrackHistoryEntry historyEntry);

    // Ordering by id because time is not really reliable for ordering - user can set it to whatever
    // they want.
    @Query("SELECT * from track_history ORDER BY uid DESC")
    LiveData<List<TrackHistoryEntry>> getAllHistory();

    @Query("SELECT * FROM track_history ORDER BY uid DESC")
    DataSource.Factory<Integer, TrackHistoryEntry> getAllHistoryPositional();

    @Query("SELECT * FROM track_history ORDER BY uid DESC LIMIT 1")
    TrackHistoryEntry getLastInsertedHistoryItem();

    @Query("UPDATE track_history SET end_time = :time WHERE end_time = 0")
    void setCurrentPlayingTrackEndTime(Date time);

    @Query("UPDATE track_history SET end_time = start_time + :deltaSeconds WHERE end_time = 0")
    void setLastHistoryItemEndTimeRelative(int deltaSeconds);

    @Query("UPDATE track_history SET art_url = :artUrl WHERE uid = :id")
    void setTrackArtUrl(int id, @NonNull String artUrl);

    @Query("DELETE FROM track_history WHERE uid < (SELECT MIN(uid) FROM (SELECT uid FROM track_history ORDER BY uid DESC LIMIT :limit))")
    void truncateHistory(int limit);

    @Query("DELETE FROM track_history")
    void deleteHistory();
}
