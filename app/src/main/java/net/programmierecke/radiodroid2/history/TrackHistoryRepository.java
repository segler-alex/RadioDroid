package net.programmierecke.radiodroid2.history;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import net.programmierecke.radiodroid2.database.RadioDroidDatabase;

import java.util.Date;
import java.util.concurrent.Executor;

import static net.programmierecke.radiodroid2.history.TrackHistoryEntry.MAX_HISTORY_ITEMS_IN_TABLE;

public class TrackHistoryRepository {
    private final static int HISTORY_PAGE_SIZE = 15;

    public interface GetItemCallback {
        /**
         * It will be ran in the DB thread
         *
         * @param trackHistoryEntry fetched entry
         * @param dao               could be used to immediately do changes in the DB
         */
        void onItemFetched(@Nullable TrackHistoryEntry trackHistoryEntry, @NonNull TrackHistoryDao dao);
    }

    private final TrackHistoryDao dao;
    private final Executor queryExecutor;
    private final LiveData<PagedList<TrackHistoryEntry>> allHistoryPaged;

    // For performance reasons we don't want to enforce history limit on every insert
    private final static int TRUNCATE_FREQUENCY = 20;
    private int insertsToTruncateLeft = 0;

    public TrackHistoryRepository(Application application) {
        RadioDroidDatabase db = RadioDroidDatabase.getDatabase(application);

        dao = db.songHistoryDao();
        queryExecutor = db.getQueryExecutor();

        allHistoryPaged = new LivePagedListBuilder<>(
                dao.getAllHistoryPositional(),
                new PagedList.Config.Builder()
                        .setPageSize(HISTORY_PAGE_SIZE)
                        .setEnablePlaceholders(true)
                        .build())
                .build();
    }

    public LiveData<PagedList<TrackHistoryEntry>> getAllHistoryPaged() {
        return allHistoryPaged;
    }

    public void insert(final TrackHistoryEntry historyEntry) {
        queryExecutor.execute(() -> {
            dao.insert(historyEntry);
            if (insertsToTruncateLeft == 0) {
                insertsToTruncateLeft = TRUNCATE_FREQUENCY;
                dao.truncateHistory(MAX_HISTORY_ITEMS_IN_TABLE);
            } else {
                insertsToTruncateLeft--;
            }
        });
    }

    public void setCurrentPlayingTrackEndTime(final Date time) {
        queryExecutor.execute(() -> dao.setCurrentPlayingTrackEndTime(time));
    }

    public void setLastHistoryItemEndTimeRelative(final int deltaSeconds) {
        queryExecutor.execute(() -> dao.setLastHistoryItemEndTimeRelative(deltaSeconds));
    }

    public void setTrackArtUrl(int id, @NonNull final String artUrl) {
        queryExecutor.execute(() -> dao.setTrackArtUrl(id, artUrl));
    }

    public void getLastInsertedHistoryItem(@NonNull final GetItemCallback callback) {
        queryExecutor.execute(() -> {
            TrackHistoryEntry item = dao.getLastInsertedHistoryItem();
            callback.onItemFetched(item, dao);
        });
    }

    public void deleteHistory() {
        queryExecutor.execute(() -> {
            dao.deleteHistory();
        });
    }
}
