package net.programmierecke.radiodroid2.players.exoplayer;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

import okhttp3.OkHttpClient;

public class RadioDataSourceFactory implements DataSource.Factory {

    private OkHttpClient httpClient;
    private final TransferListener transferListener;
    private IcyDataSource.IcyDataSourceListener dataSourceListener;
    private boolean requestIcyMetadata;

    public RadioDataSourceFactory(@NonNull OkHttpClient httpClient,
                                  @NonNull TransferListener transferListener,
                                  @NonNull IcyDataSource.IcyDataSourceListener dataSourceListener,
                                  boolean requestIcyMetadata) {
        this.httpClient = httpClient;
        this.transferListener = transferListener;
        this.dataSourceListener = dataSourceListener;
    }

    @Override
    public DataSource createDataSource() {
        return new IcyDataSource(httpClient, transferListener, dataSourceListener, requestIcyMetadata);
    }
}
