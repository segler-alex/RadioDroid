package net.programmierecke.radiodroid2.players.mediaplayer;

import android.util.Log;

import androidx.annotation.NonNull;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.station.live.ShoutcastInfo;
import net.programmierecke.radiodroid2.station.live.StreamLiveInfo;
import net.programmierecke.radiodroid2.recording.Recordable;
import net.programmierecke.radiodroid2.recording.RecordableListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StreamProxy implements Recordable {
    private static final String TAG = "PROXY";

    private static final int MAX_RETRIES = 100;

    private OkHttpClient httpClient;
    private StreamProxyListener callback;
    private RecordableListener recordableListener;
    private String uri;
    private byte readBuffer[] = new byte[256 * 16];
    private volatile String localAddress = null;
    private boolean isStopped = false;

    public StreamProxy(OkHttpClient httpClient, String uri, StreamProxyListener callback) {
        this.httpClient = httpClient;
        this.uri = uri;
        this.callback = callback;

        createProxy();
    }

    private void createProxy() {
        if (BuildConfig.DEBUG) Log.d(TAG, "thread started");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connectToStream();
                    if (BuildConfig.DEBUG) Log.d(TAG, "createProxy() ended");
                } catch (Exception e) {
                    Log.e(TAG, "", e);
                }
            }
        }, "StreamProxy").start();
    }

    private void proxyDefaultStream(ShoutcastInfo info, ResponseBody responseBody, OutputStream outStream) throws Exception {
        int bytesUntilMetaData = 0;
        boolean streamHasMetaData = false;

        if (info != null) {
            callback.onFoundShoutcastStream(info, false);
            bytesUntilMetaData = info.metadataOffset;
            streamHasMetaData = true;
        }

        InputStream inputStream = responseBody.byteStream();

        while (!isStopped) {
            if (!streamHasMetaData || (bytesUntilMetaData > 0)) {
                int bytesToRead = Math.min(readBuffer.length, inputStream.available());
                if (streamHasMetaData) {
                    bytesToRead = Math.min(bytesUntilMetaData, bytesToRead);
                }

                int readBytes = inputStream.read(readBuffer, 0, bytesToRead);
                if (readBytes == 0) {
                    continue;
                }
                if (readBytes < 0) {
                    break;
                }

                if (streamHasMetaData) {
                    bytesUntilMetaData -= readBytes;
                }

                outStream.write(readBuffer, 0, readBytes);

                if (recordableListener != null) {
                    recordableListener.onBytesAvailable(readBuffer, 0, readBytes);
                }

                callback.onBytesRead(readBuffer, 0, readBytes);
            } else {
                readMetaData(inputStream);
                bytesUntilMetaData = info.metadataOffset;
            }
        }

        stopRecording();
    }

    private int readMetaData(InputStream inputStream) throws IOException {
        int metadataBytes = inputStream.read() * 16;
        int metadataBytesToRead = metadataBytes;
        int readBytesBufferMetadata = 0;
        int readBytes;

        if (BuildConfig.DEBUG) Log.d(TAG, "metadata size:" + metadataBytes);
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
                    StreamLiveInfo streamLiveInfo = new StreamLiveInfo(rawMetadata);
                    if (BuildConfig.DEBUG) Log.d(TAG, "META:" + streamLiveInfo.getTitle());
                    callback.onFoundLiveStreamInfo(streamLiveInfo);
                    break;
                }
            }
        }
        return readBytesBufferMetadata + 1;
    }

    private void connectToStream() {
        isStopped = false;

        int retry = MAX_RETRIES;

        Socket socketProxy = null;
        OutputStream outputStream = null;
        ServerSocket proxyServer = null;

        try {
            if (BuildConfig.DEBUG) Log.d(TAG, "creating local proxy");

            // Create proxy stream which media player will connect to.

            try {
                proxyServer = new ServerSocket(0, 1, InetAddress.getLocalHost());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            final int port = proxyServer.getLocalPort();
            localAddress = String.format(Locale.US, "http://localhost:%d", port);

            final Request request = new Request.Builder().url(uri)
                    .addHeader("Icy-MetaData", "1")
                    .build();

            while (!isStopped && retry > 0) {
                ResponseBody responseBody = null;

                try {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "connecting to stream (try=" + retry + "):" + uri);
                    }

                    Response response = httpClient.newCall(request).execute();
                    responseBody = response.body();
                    assert responseBody != null;

                    final MediaType contentType = responseBody.contentType();

                    if (BuildConfig.DEBUG) Log.d(TAG, "waiting...");

                    if (isStopped) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "stopped from the outside");
                        break;
                    }

                    if (socketProxy != null) {
                        socketProxy.close();
                        socketProxy = null;
                    }

                    if (outputStream != null) {
                        outputStream.close();
                        outputStream = null;
                    }

                    callback.onStreamCreated(localAddress);
                    proxyServer.setSoTimeout(2000);
                    socketProxy = proxyServer.accept();

                    // send ok message to local mediaplayer
                    if (BuildConfig.DEBUG) Log.d(TAG, "sending OK to the local media player");
                    outputStream = socketProxy.getOutputStream();
                    outputStream.write(("HTTP/1.0 200 OK\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Content-Type: " + contentType +
                            "\r\n\r\n").getBytes("utf-8"));

                    final String type = contentType.toString().toLowerCase();

                    if (BuildConfig.DEBUG) Log.d(TAG, "Content Type: " + type);

                    if (type.equals("application/vnd.apple.mpegurl") || type.equals("application/x-mpegurl")) {
                        Log.e(TAG, "Cannot play HLS streams through proxy!");
                    } else {
                        // try to get shoutcast information from stream connection
                        final ShoutcastInfo info = ShoutcastInfo.Decode(response);

                        proxyDefaultStream(info, responseBody, outputStream);
                    }
                    // reset retry count, if connection was ok
                    retry = MAX_RETRIES;
                } catch (ProtocolException protocolException) {
                    Log.e(TAG, "connecting to stream failed due to protocol exception, will NOT retry.", protocolException);
                    break;
                } catch (SocketTimeoutException ignored) {
                } catch (Exception e) {
                    Log.e(TAG, "exception occurred inside the connection loop, retry.", e);
                } finally {
                    if (responseBody != null) {
                        responseBody.close();
                    }
                }

                if (isStopped) {
                    break;
                }

                retry--;
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted ex Proxy() ", e);
        } finally {
            try {
                if (proxyServer != null) {
                    proxyServer.close();
                }

                if (socketProxy != null) {
                    socketProxy.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "exception occurred while closing resources.", e);
            }
        }

        // inform outside if stream stopped, only if outside did not initiate stop
        if (!isStopped) {
            callback.onStreamStopped();
        }

        stop();
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

    public String getLocalAddress() {
        return localAddress;
    }

    public void stop() {
        if (BuildConfig.DEBUG) Log.d(TAG, "stopping proxy.");

        isStopped = true;

        stopRecording();
    }

    @Override
    public boolean canRecord() {
        return true;
    }

    @Override
    public void startRecording(@NonNull RecordableListener recordableListener) {
        this.recordableListener = recordableListener;
    }

    @Override
    public void stopRecording() {
        if (recordableListener != null) {
            recordableListener.onRecordingEnded();
            recordableListener = null;
        }
    }

    @Override
    public boolean isRecording() {
        return recordableListener != null;
    }

    @Override
    public Map<String, String> getRecordNameFormattingArgs() {
        return null;
    }

    @Override
    public String getExtension() {
        return "mp3";
    }
}
