package net.programmierecke.radiodroid2;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import net.programmierecke.radiodroid2.recording.RecordingsManager;

public class RadioDroidApp extends Application {

    private HistoryManager historyManager;
    private FavouriteManager favouriteManager;
    private RecordingsManager recordingsManager;

    @Override
    public void onCreate() {
        super.onCreate();

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso picassoInstance = builder.build();
        Picasso.setSingletonInstance(picassoInstance);

        CountryFlagsLoader.getInstance().load(this);

        historyManager = new HistoryManager(this);
        favouriteManager = new FavouriteManager(this);
        recordingsManager = new RecordingsManager();
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public FavouriteManager getFavouriteManager() {
        return favouriteManager;
    }

    public RecordingsManager getRecordingsManager() {
        return recordingsManager;
    }
}
