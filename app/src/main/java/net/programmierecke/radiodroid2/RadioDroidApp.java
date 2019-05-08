package net.programmierecke.radiodroid2;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import net.programmierecke.radiodroid2.proxy.ProxySettings;
import net.programmierecke.radiodroid2.recording.RecordingsManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RadioDroidApp extends Application {

    private HistoryManager historyManager;
    private FavouriteManager favouriteManager;
    private RecordingsManager recordingsManager;

    private ConnectionPool connectionPool;
    private OkHttpClient httpClient;

    public class UserAgentInterceptor implements Interceptor {

        private final String userAgent;

        public UserAgentInterceptor(String userAgent) {
            this.userAgent = userAgent;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request requestWithUserAgent = originalRequest.newBuilder()
                    .header("User-Agent", userAgent)
                    .build();
            return chain.proceed(requestWithUserAgent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        connectionPool = new ConnectionPool();

        rebuildHttpClient();

        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso picassoInstance = builder.build();
        Picasso.setSingletonInstance(picassoInstance);

        CountryCodeDictionary.getInstance().load(this);
        CountryFlagsLoader.getInstance();

        historyManager = new HistoryManager(this);
        favouriteManager = new FavouriteManager(this);
        recordingsManager = new RecordingsManager();
    }

    public void rebuildHttpClient() {
        httpClient = newHttpClient()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(new UserAgentInterceptor("RadioDroid2/" + BuildConfig.VERSION_NAME))
                .build();
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

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    public OkHttpClient.Builder newHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectionPool(connectionPool);
        setCurrentOkHttpProxy(builder);
        return builder;
    }

    public void setCurrentOkHttpProxy(@NonNull OkHttpClient.Builder builder) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        ProxySettings proxySettings = ProxySettings.fromPreferences(sharedPref);
        if (proxySettings != null) {
            Utils.setOkHttpProxy(builder, proxySettings);
        }
    }
}
