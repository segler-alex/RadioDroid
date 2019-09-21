package net.programmierecke.radiodroid2.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.programmierecke.radiodroid2.history.TrackHistoryDao;
import net.programmierecke.radiodroid2.history.TrackHistoryEntry;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static net.programmierecke.radiodroid2.history.TrackHistoryEntry.MAX_UNKNOWN_TRACK_DURATION;

@Database(entities = {TrackHistoryEntry.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class RadioDroidDatabase extends RoomDatabase {
    public abstract TrackHistoryDao songHistoryDao();

    private static volatile RadioDroidDatabase INSTANCE;

    private Executor queryExecutor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "RadioDroidDatabase Executor"));

    public static RadioDroidDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (RadioDroidDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            RadioDroidDatabase.class, "radio_droid_database")
                            .addCallback(CALLBACK)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public Executor getQueryExecutor() {
        return queryExecutor;
    }

    private static RoomDatabase.Callback CALLBACK = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);

            INSTANCE.queryExecutor.execute(() -> {
                // App may have been terminated without notice so we should set last track history entry's
                // end time to something reasonable.
                INSTANCE.songHistoryDao().setLastHistoryItemEndTimeRelative(MAX_UNKNOWN_TRACK_DURATION);
            });
        }
    };

}
