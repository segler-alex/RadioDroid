package net.programmierecke.radiodroid2.players.exoplayer;

import android.support.annotation.NonNull;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

import okhttp3.OkHttpClient;

public class RadioDataSourceFactory implements DataSource.Factory {

    private OkHttpClient httpClient;
    private final TransferListener<? super HttpDataSource> transferListener;
    private IcyDataSource.IcyDataSourceListener dataSourceListener;
    private long retryTimeout = IcyDataSource.DEFAULT_TIME_UNTIL_STOP_RECONNECTING;
    private long retryDelay = IcyDataSource.DEFAULT_DELAY_BETWEEN_RECONNECTIONS;

    public RadioDataSourceFactory(@NonNull OkHttpClient httpClient,
                                  @NonNull TransferListener<? super HttpDataSource> transferListener,
                                  @NonNull IcyDataSource.IcyDataSourceListener dataSourceListener,
                                  long retryTimeout,
                                  long retryDelay) {
        this.httpClient = httpClient;
        this.transferListener = transferListener;
        this.dataSourceListener = dataSourceListener;
        this.retryTimeout = retryTimeout;
        this.retryDelay = retryDelay;
    }

    @Override
    public DataSource createDataSource() {
        return new IcyDataSource(httpClient, transferListener, dataSourceListener, retryTimeout, retryDelay);
    }
}
