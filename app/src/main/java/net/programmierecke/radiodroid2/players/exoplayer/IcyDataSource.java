package net.programmierecke.radiodroid2.players.exoplayer;


import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.data.StreamLiveInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static net.programmierecke.radiodroid2.Utils.getMimeType;
import static okhttp3.internal.Util.closeQuietly;

/**
 * An {@link HttpDataSource} that uses {@link OkHttpClient},
 * retrieves stream's {@link ShoutcastInfo} and {@link StreamLiveInfo} if any,
 * attempts to reconnect if connection is lost. These distinguishes it from {@link DefaultHttpDataSource}.
 * <p>
 * When connection is lost attempts to reconnect will made alongside with calling
 * {@link IcyDataSourceListener#onDataSourceConnectionLost()}.
 * After reconnecting time has passed
 * {@link IcyDataSourceListener#onDataSourceConnectionLostIrrecoverably()} will be called.
 **/
public class IcyDataSource implements HttpDataSource {

    public static final long DEFAULT_TIME_UNTIL_STOP_RECONNECTING = 2 * 60 * 1000; // 2 minutes

    public static final long DEFAULT_DELAY_BETWEEN_RECONNECTIONS = 0;

    public interface IcyDataSourceListener {
        /**
         * Called on first connection and after successful reconnection.
         */
        void onDataSourceConnected();

        /**
         * Called when connection is lost and reconnection attempts will be made.
         */
        void onDataSourceConnectionLost();

        /**
         * Called when data source gives up reconnecting.
         */
        void onDataSourceConnectionLostIrrecoverably();

        void onDataSourceShoutcastInfo(@Nullable ShoutcastInfo shoutcastInfo);

        void onDataSourceStreamLiveInfo(StreamLiveInfo streamLiveInfo);

        void onDataSourceBytesRead(byte[] buffer, int offset, int length);
    }

    private static final String TAG = "IcyDataSource";

    private DataSpec dataSpec;

    private final OkHttpClient httpClient;
    private final TransferListener transferListener;
    private final IcyDataSourceListener dataSourceListener;

    private long timeUntilStopReconnecting;
    private long delayBetweenReconnections;

    private Request request;

    private ResponseBody responseBody;
    private Map<String, List<String>> responseHeaders;

    private int remainingUntilMetadata = Integer.MAX_VALUE;

    private byte readBuffer[] = new byte[256 * 16];

    private boolean opened;

    private ShoutcastInfo shoutcastInfo;
    private StreamLiveInfo streamLiveInfo;

    public IcyDataSource(@NonNull OkHttpClient httpClient,
                         @NonNull TransferListener listener,
                         @NonNull IcyDataSourceListener dataSourceListener,
                         long timeUntilStopReconnecting,
                         long delayBetweenReconnections) {
        this.httpClient = httpClient;
        this.transferListener = listener;
        this.dataSourceListener = dataSourceListener;
        this.timeUntilStopReconnecting = timeUntilStopReconnecting;
        this.delayBetweenReconnections = delayBetweenReconnections;
    }

    @Override
    public long open(DataSpec dataSpec) throws HttpDataSourceException {
        close();

        this.dataSpec = dataSpec;

        final boolean allowGzip = (dataSpec.flags & DataSpec.FLAG_ALLOW_GZIP) != 0;

        HttpUrl url = HttpUrl.parse(dataSpec.uri.toString());
        Request.Builder builder = new Request.Builder().url(url)
                .addHeader("Icy-MetaData", "1");

        if (!allowGzip) {
            builder.addHeader("Accept-Encoding", "identity");
        }

        request = builder.build();

        return connect();
    }

    private long connect() throws HttpDataSourceException {
        Response response;
        try {
            response = httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new HttpDataSourceException("Unable to connect to " + dataSpec.uri.toString(), e,
                    dataSpec, HttpDataSourceException.TYPE_OPEN);
        }

        final int responseCode = response.code();

        if (!response.isSuccessful()) {
            final Map<String, List<String>> headers = request.headers().toMultimap();
            throw new InvalidResponseCodeException(responseCode, headers, dataSpec);
        }

        responseBody = response.body();
        assert responseBody != null;

        responseHeaders = response.headers().toMultimap();

        final MediaType contentType = responseBody.contentType();

        final String type = contentType == null ? getMimeType(dataSpec.uri.toString(), "audio/mpeg") : contentType.toString().toLowerCase();

        if (!REJECT_PAYWALL_TYPES.evaluate(type)) {
            close();
            throw new InvalidContentTypeException(type, dataSpec);
        }

        opened = true;

        dataSourceListener.onDataSourceConnected();
        transferListener.onTransferStart(this, dataSpec, true);

        if (type.equals("application/vnd.apple.mpegurl") || type.equals("application/x-mpegurl")) {
            return responseBody.contentLength();
        } else {
            // try to get shoutcast information from stream connection
            shoutcastInfo = ShoutcastInfo.Decode(response);
            dataSourceListener.onDataSourceShoutcastInfo(shoutcastInfo);

            if (shoutcastInfo != null) {
                remainingUntilMetadata = shoutcastInfo.metadataOffset;
            } else {
                remainingUntilMetadata = Integer.MAX_VALUE;
            }

            return responseBody.contentLength();
        }
    }

    private void reconnect() throws HttpDataSourceException {
        close();
        connect();
        Log.i(TAG, "Reconnected successfully!");
    }

    @Override
    public void close() throws HttpDataSourceException {
        if (opened) {
            opened = false;
            transferListener.onTransferEnd(this, dataSpec, true);
        }

        if (responseBody != null) {
            closeQuietly(responseBody);
            responseBody = null;
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws HttpDataSourceException {
        try {
            final int bytesTransferred = readInternal(buffer, offset, readLength);
            transferListener.onBytesTransferred(this, dataSpec, true, bytesTransferred);
            return bytesTransferred;
        } catch (HttpDataSourceException readError) {
            dataSourceListener.onDataSourceConnectionLost();

            final long reconnectStartTime = System.currentTimeMillis();

            while (true) {
                final long currentTime = System.currentTimeMillis();

                if (BuildConfig.DEBUG) Log.d(TAG, "Reconnecting...");

                try {
                    reconnect();
                    break;
                } catch (HttpDataSourceException ex) {
                    try {
                        Thread.sleep(delayBetweenReconnections);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                if (currentTime - reconnectStartTime > timeUntilStopReconnecting) {
                    dataSourceListener.onDataSourceConnectionLostIrrecoverably();
                    throw new HttpDataSourceException("Reconnection retry time ended.", dataSpec, HttpDataSourceException.TYPE_READ);
                }
            }
        }

        return 0;
    }

    private int readInternal(byte[] buffer, int offset, int readLength) throws HttpDataSourceException {
        InputStream stream = responseBody.byteStream();

        int ret = 0;
        try {
            ret = stream.read(buffer, offset, remainingUntilMetadata < readLength ? remainingUntilMetadata : readLength);
        } catch (IOException e) {
            throw new HttpDataSourceException(e, dataSpec, HttpDataSourceException.TYPE_READ);
        }

        if(ret > 0) {
            dataSourceListener.onDataSourceBytesRead(buffer, offset, ret);
        }

        if (remainingUntilMetadata == ret) {
            try {
                readMetaData(stream);
                remainingUntilMetadata = shoutcastInfo.metadataOffset;
            } catch (IOException e) {
                throw new HttpDataSourceException(e, dataSpec, HttpDataSourceException.TYPE_READ);
            }
        } else {
            remainingUntilMetadata -= ret;
        }

        return ret;
    }

    @Override
    public Uri getUri() {
        return dataSpec.uri;
    }

    @Override
    public void setRequestProperty(String name, String value) {
        // Ignored
    }

    @Override
    public void clearRequestProperty(String name) {
        // Ignored
    }

    @Override
    public void clearAllRequestProperties() {
        // Ignored
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    private int readMetaData(InputStream inputStream) throws IOException {
        int metadataBytes = inputStream.read() * 16;
        int metadataBytesToRead = metadataBytes;
        int readBytesBufferMetadata = 0;
        int readBytes;

        if (metadataBytes > 0) {
            Arrays.fill(readBuffer, (byte) 0);
            while (true) {
                readBytes = inputStream.read(readBuffer, readBytesBufferMetadata, metadataBytesToRead);
                if (readBytes == 0) {
                    continue;
                }
                if (readBytes < 0) {
                    break;
                }
                metadataBytesToRead -= readBytes;
                readBytesBufferMetadata += readBytes;
                if (metadataBytesToRead <= 0) {
                    String s = new String(readBuffer, 0, metadataBytes, "utf-8");

                    if (BuildConfig.DEBUG) Log.d(TAG, "METADATA:" + s);

                    Map<String, String> rawMetadata = decodeShoutcastMetadata(s);
                    streamLiveInfo = new StreamLiveInfo(rawMetadata);

                    dataSourceListener.onDataSourceStreamLiveInfo(streamLiveInfo);

                    if (BuildConfig.DEBUG) Log.d(TAG, "META:" + streamLiveInfo.getTitle());
                    break;
                }
            }
        }
        return readBytesBufferMetadata + 1;
    }

    private Map<String, String> decodeShoutcastMetadata(String metadataStr) {
        Map<String, String> metadata = new HashMap<>();

        String[] kvs = metadataStr.split(";");

        for (String kv : kvs) {
            final int n = kv.indexOf('=');
            if (n < 1) continue;

            final boolean isString = n + 1 < kv.length()
                    && kv.charAt(kv.length() - 1) == '\''
                    && kv.charAt(n + 1) == '\'';

            final String key = kv.substring(0, n);
            final String val = isString ?
                    kv.substring(n + 2, kv.length() - 1) :
                    n + 1 < kv.length() ?
                            kv.substring(n + 1) : "";

            metadata.put(key, val);
        }

        return metadata;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {

    }
}
