package net.programmierecke.radiodroid2;

import android.util.Log;

import net.programmierecke.radiodroid2.data.PlaylistM3U;
import net.programmierecke.radiodroid2.data.PlaylistM3UEntry;
import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.data.StreamLiveInfo;
import net.programmierecke.radiodroid2.interfaces.IStreamProxyEventReceiver;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StreamProxy {
    private static final String TAG = "PROXY";

    private IStreamProxyEventReceiver callback;
    private String uri;
    private long connectionBytesTotal = 0;
    private volatile String localAddress = null;
    private FileOutputStream fileOutputStream;
    private boolean isStopped = false;
    private String outFileName = null;
    private boolean isHls = false;

    public StreamProxy(String uri, IStreamProxyEventReceiver callback) {
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

    private byte buf[] = new byte[256 * 16];

    private void proxyDefaultStream(ShoutcastInfo info, ResponseBody responseBody, OutputStream outStream) throws Exception {
        int bytesUntilMetaData = 0;
        boolean streamHasMetaData = false;

        if (info != null) {
            callback.foundShoutcastStream(info, false);
            bytesUntilMetaData = info.metadataOffset;
            streamHasMetaData = true;
        }

        InputStream inputStream = responseBody.byteStream();

        int readBytesBuffer = 0;

        while (!isStopped) {
            int readBytes;
            if (!streamHasMetaData || (bytesUntilMetaData > 0)) {
                int bytesToRead = Math.min(buf.length - readBytesBuffer, inputStream.available());
                if (streamHasMetaData) {
                    bytesToRead = Math.min(bytesUntilMetaData, bytesToRead);
                }
                readBytes = inputStream.read(buf, readBytesBuffer, bytesToRead);
                if (readBytes == 0) {
                    continue;
                }
                if (readBytes < 0) {
                    break;
                }
                readBytesBuffer += readBytes;
                connectionBytesTotal += readBytes;
                if (streamHasMetaData) {
                    bytesUntilMetaData -= readBytes;
                }

                if (BuildConfig.DEBUG) Log.d(TAG, "stream bytes relayed:" + readBytes);

                outStream.write(buf, 0, readBytesBuffer);
                if (fileOutputStream != null) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "writing to record file..");
                    fileOutputStream.write(buf, 0, readBytesBuffer);
                }
                readBytesBuffer = 0;
            } else {
                readBytes = readMetaData(inputStream);
                bytesUntilMetaData = info.metadataOffset;
                connectionBytesTotal += readBytes;
            }
        }

        stopRecord();
    }

    private int readMetaData(InputStream inputStream) throws IOException {
        int metadataBytes = inputStream.read() * 16;
        int metadataBytesToRead = metadataBytes;
        int readBytesBufferMetadata = 0;
        int readBytes;

        if (BuildConfig.DEBUG) Log.d(TAG, "metadata size:" + metadataBytes);
        if (metadataBytes > 0) {
            Arrays.fill(buf, (byte) 0);
            while (true) {
                readBytes = inputStream.read(buf, readBytesBufferMetadata, metadataBytesToRead);
                if (readBytes == 0) {
                    continue;
                }
                if (readBytes < 0) {
                    break;
                }
                metadataBytesToRead -= readBytes;
                readBytesBufferMetadata += readBytes;
                if (metadataBytesToRead <= 0) {
                    String s = new String(buf, 0, metadataBytes, "utf-8");
                    if (BuildConfig.DEBUG) Log.d(TAG, "METADATA:" + s);
                    Map<String, String> rawMetadata = decodeShoutcastMetadata(s);
                    StreamLiveInfo streamLiveInfo = new StreamLiveInfo(rawMetadata);
                    if (BuildConfig.DEBUG) Log.d(TAG, "META:" + streamLiveInfo.getTitle());
                    callback.foundLiveStreamInfo(streamLiveInfo);
                    break;
                }
            }
        }
        return readBytesBufferMetadata + 1;
    }

    private void streamFile(String urlStr, OutputStream outStream) throws IOException {
        if (BuildConfig.DEBUG) Log.d(TAG, "URL Stream Data:   " + urlStr);

        Request request = new Request.Builder().url(urlStr)
                .build();

        OkHttpClient httpClient = HttpClient.getInstance().newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        Response response = httpClient.newCall(request).execute();

        try {
            ResponseBody responseBody = response.body();

            InputStream inContent = responseBody.byteStream();
            boolean recordActive = false;
            if (fileOutputStream != null) {
                recordActive = true;
            }

            byte[] bufContent = new byte[1000];
            while (!isStopped) {
                int bytesRead = inContent.read(bufContent);
                if (bytesRead < 0) {
                    break;
                }
                connectionBytesTotal += bytesRead;
                outStream.write(bufContent, 0, bytesRead);
                if ((fileOutputStream != null) && recordActive) {
                    Log.v(TAG, "writing to record file..");
                    fileOutputStream.write(bufContent, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            response.close();
            throw e;
        }
    }

    private boolean containsString(ArrayList<String> list, String item) {
        for (String aList : list) {
            if (item.equals(aList)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<String> streamedFiles = new ArrayList<String>();

    private void proxyHlsStream(URL path, ResponseBody responseBody, OutputStream outStream) throws IOException {
        if (!isHls) {
            isHls = true;
            callback.foundShoutcastStream(null, true);
        }

        String responseString = responseBody.string();

        if (BuildConfig.DEBUG) Log.d(TAG, "read m3u:\n" + responseString);

        PlaylistM3U playlist = new PlaylistM3U(path, responseString);

        PlaylistM3UEntry[] entries = playlist.getEntries();
        for (PlaylistM3UEntry entry : entries) {
            String urlStr = entry.getContent();

            if (!entry.getIsStreamInformation()) {
                // use this url in the future
                uri = path.toString();

                String urlWithoutQuery = urlStr;
                int indexQuery = urlStr.indexOf("?");
                if (indexQuery >= 0) {
                    urlWithoutQuery = urlStr.substring(0, indexQuery);
                }

                if (!containsString(streamedFiles, urlWithoutQuery)) {
                    Log.w(TAG, "did not find in db:" + urlWithoutQuery);
                    streamedFiles.add(urlWithoutQuery);

                    streamFile(urlStr, outStream);
                }
            } else {
                Log.w(TAG, "URL Stream info:" + urlStr);

                Request request = new Request.Builder().url(urlStr)
                        .build();

                OkHttpClient httpClient = HttpClient.getInstance().newBuilder()
                        .connectTimeout(2, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build();

                Response response = httpClient.newCall(request).execute();

                try {
                    ResponseBody newResponseBody = response.body();
                    URL newStreamUrl = request.url().url();

                    proxyHlsStream(newStreamUrl, newResponseBody, outStream);
                } catch (Exception e) {
                    response.close();
                    throw e;
                }

                break;
            }
        }
    }

    private void connectToStream() {
        try {
            final int MaxRetries = 100;
            int retry = MaxRetries;
            while (!isStopped && retry > 0) {
                ResponseBody responseBody = null;
                Socket socketProxy = null;
                OutputStream outputStream = null;

                try {
                    // connect to stream
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "connecting to stream (try=" + retry + "):" + uri);
                    }

                    final Request request = new Request.Builder().url(uri)
                            .addHeader("Icy-MetaData", "1")
                            .build();

                    final OkHttpClient httpClient = HttpClient.getInstance().newBuilder()
                            .connectTimeout(2, TimeUnit.SECONDS)
                            .readTimeout(2, TimeUnit.SECONDS)
                            .build();

                    final Response response = httpClient.newCall(request).execute();
                    responseBody = response.body();
                    assert responseBody != null;

                    final MediaType contentType = responseBody.contentType();

                    if (BuildConfig.DEBUG) Log.d(TAG, "creating local proxy");

                    // Create proxy stream which media player will connect to.
                    final ServerSocket proxyServer = new ServerSocket(0, 1, InetAddress.getLocalHost());
                    final int port = proxyServer.getLocalPort();
                    localAddress = String.format(Locale.US, "http://localhost:%d", port);

                    if (BuildConfig.DEBUG) Log.d(TAG, "waiting...");

                    if (isStopped) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "stopped from the outside");
                        break;
                    }

                    callback.streamCreated(localAddress);
                    socketProxy = proxyServer.accept();
                    proxyServer.close();

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
                        // Hls stream
                        final URL url = request.url().url();
                        proxyHlsStream(url, responseBody, outputStream);
                        // wait some time for next check for new files in stream
                        Thread.sleep(5000);
                    } else {
                        // try to get shoutcast information from stream connection
                        final ShoutcastInfo info = ShoutcastInfo.Decode(response);

                        proxyDefaultStream(info, responseBody, outputStream);
                    }
                    // reset retry count, if connection was ok
                    retry = MaxRetries;
                } catch (ProtocolException protocolException) {
                    Log.e(TAG, "connecting to stream failed due to protocol exception, will NOT retry.", protocolException);
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "exception occurred inside the connection loop, retry.", e);
                } finally {
                    if (responseBody != null) {
                        responseBody.close();
                    }
                    try {
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

                retry--;
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted ex Proxy() ", e);
        }
        // inform outside if stream stopped, only if outside did not initiate stop
        if (!isStopped) {
            callback.streamStopped();
        }
        stop();
    }

    public void record(String stationName, String streamTitle) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        String appendTitle = "";
        if (!streamTitle.isEmpty()) {
            appendTitle = "_-_" + Utils.sanitizeName(streamTitle);
        }
        if (getIsHls()) {
            outFileName = String.format(Locale.US, "%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS_%2$s%3$s.ts", calendar, Utils.sanitizeName(stationName), appendTitle);
        } else {
            outFileName = String.format(Locale.US, "%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS_%2$s%3$s.mp3", calendar, Utils.sanitizeName(stationName), appendTitle);
        }
        recordInternal(outFileName);
    }

    public boolean getIsHls() {
        return isHls;
    }

    private void recordInternal(String fileName) {
        if (fileOutputStream == null) {
            try {
                String path = RecordingsManager.getRecordDir() + "/" + fileName;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "start recording to :" + fileName + " in dir " + path);
                }
                fileOutputStream = new FileOutputStream(path);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "record('" + fileName + "'): ", e);
            }
        }
    }

    public void stopRecord() {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "io exception while stopping record:" + e);
            }
            outFileName = null;
            fileOutputStream = null;
        }
    }

    private Map<String, String> decodeShoutcastMetadata(String metadata) {
        // icecast server does not encode "'" inside strings. so i am not able to check when a string ends
        //boolean stringStartedSingle = false;
        //boolean stringStartedDouble = false;
        String key = "";
        String value = "";
        boolean valueActive = false;

        Map<String, String> dict = new HashMap<String, String>();
        for (int i = 0; i < metadata.length(); i++) {
            char c = metadata.charAt(i);

            /*if (stringStartedDouble || stringStartedSingle){
                if (c == '\'' && stringStartedSingle){
                    stringStartedSingle = false;
                } else if (c == '"' && stringStartedDouble){
                    stringStartedDouble = false;
                }else{
                    if (valueActive){
                        value += c;
                    }else{
                        key += c;
                    }
                }
            } else */
            {
                if (c == '\'') {
                    //stringStartedSingle = true;
                } else if (c == '"') {
                    //stringStartedDouble = true;
                } else if (c == '=') {
                    valueActive = true;
                } else if (c == ';') {
                    valueActive = false;
                    dict.remove(key);
                    dict.put(key, value);
                    key = "";
                    value = "";
                } else {
                    if (valueActive) {
                        value += c;
                    } else {
                        key += c;
                    }
                }
            }

        }
        if (valueActive) {
            dict.remove(key);
            dict.put(key, value);
        }
        return dict;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void stop() {
        if (BuildConfig.DEBUG) Log.d(TAG, "stopping proxy.");
        isStopped = true;
    }

    public String getOutFileName() {
        return outFileName;
    }

    public long getTotalBytes() {
        return connectionBytesTotal;
    }
}
