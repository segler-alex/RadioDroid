package net.programmierecke.radiodroid2;

import android.content.Context;
import android.util.Log;

import net.programmierecke.radiodroid2.data.PlaylistM3U;
import net.programmierecke.radiodroid2.data.PlaylistM3UEntry;
import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.interfaces.IStreamProxyEventReceiver;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StreamProxy {
    private IStreamProxyEventReceiver callback;
    private String uri;
    private long connectionBytesTotal = 0;
    private Socket socketProxy;
    private volatile String localAdress = null;
    private FileOutputStream fileOutputStream;
    private boolean isStopped = false;
    private String outFileName = null;
    private final String TAG = "PROXY";
    private boolean isHls = false;

    public StreamProxy(String uri, IStreamProxyEventReceiver callback) {
        this.uri = uri;
        this.callback = callback;

        createProxy();
    }

    private void createProxy() {
        if (BuildConfig.DEBUG) Log.d(TAG, "thread started");
        /*
        ServerSocket proxyServer = null;
        try {
            proxyServer = new ServerSocket(0, 1, InetAddress.getLocalHost());
            int port = proxyServer.getLocalPort();
            localAdress = String.format(Locale.US,"http://localhost:%d",port);
        } catch (IOException e) {
            Log.e(TAG,"createProxy() create server socket: "+e);
        }
        */

        //if (proxyServer != null) {
//            final ServerSocket finalProxyServer = proxyServer;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    doConnectToStream();
                    if (BuildConfig.DEBUG) Log.d(TAG, "createProxy() ended");
                } catch (Exception e) {
                    Log.e(TAG, "", e);
                }
            }
        }, "StreamProxy").start();
        //}
    }

    private InputStream in;
    private OutputStream out;

    private byte buf[] = new byte[256 * 16];

    private void defaultStream(ShoutcastInfo info) throws Exception {
        int bytesUntilMetaData = 0;
        boolean streamHasMetaData = false;

        if (info != null) {
            callback.foundShoutcastStream(info, false);
            bytesUntilMetaData = info.metadataOffset;
            streamHasMetaData = true;
        }

        int readBytesBuffer = 0;

        while (!isStopped) {
            int readBytes = 0;
            if (!streamHasMetaData || (bytesUntilMetaData > 0)) {
                int bytesToRead = Math.min(buf.length - readBytesBuffer, in.available());
                if (streamHasMetaData) {
                    bytesToRead = Math.min(bytesUntilMetaData, bytesToRead);
                }
                readBytes = in.read(buf, readBytesBuffer, bytesToRead);
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
                out.write(buf, 0, readBytesBuffer);
                if (fileOutputStream != null) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "writing to record file..");
                    fileOutputStream.write(buf, 0, readBytesBuffer);
                }
                readBytesBuffer = 0;
            } else {
                readBytes = readMetaData();
                bytesUntilMetaData = info.metadataOffset;
                connectionBytesTotal += readBytes;
            }
        }

        stopInternal();
    }

    private int readMetaData() throws IOException {
        int metadataBytes = in.read() * 16;
        int metadataBytesToRead = metadataBytes;
        int readBytesBufferMetadata = 0;
        int readBytes = 0;

        if (BuildConfig.DEBUG) Log.d(TAG, "metadata size:" + metadataBytes);
        if (metadataBytes > 0) {
            Arrays.fill(buf, (byte) 0);
            while (true) {
                readBytes = in.read(buf, readBytesBufferMetadata, metadataBytesToRead);
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
                    Map<String, String> dict = DecodeShoutcastMetadata(s);
                    if (BuildConfig.DEBUG) Log.d(TAG, "META:" + dict.get("StreamTitle"));
                    callback.foundLiveStreamInfo(dict);
                    break;
                }
            }
        }
        return readBytesBufferMetadata + 1;
    }

    private void streamFile(String urlStr) throws IOException {
        if (BuildConfig.DEBUG) Log.d(TAG, "URL Stream Data:   " + urlStr);
        URL u = new URL(urlStr);
        URLConnection connection = u.openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(10000);
        connection.connect();
        InputStream inContent = connection.getInputStream();
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
            out.write(bufContent, 0, bytesRead);
            if ((fileOutputStream != null) && recordActive) {
                Log.v(TAG, "writing to record file..");
                fileOutputStream.write(bufContent, 0, bytesRead);
            }
        }
    }

    private boolean containsString(ArrayList<String> list, String item) {
        for (int i = 0; i < list.size(); i++) {
            if (item.equals(list.get(i))) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<String> streamedFiles = new ArrayList<String>();

    private void hlsStream(URL path, int size, InputStream inM3U) throws Exception {
        int readBytes = 0;
        int readBytesBuffer = 0;
        int bytesToRead = size;
        byte bufM3U[] = new byte[size];

        if (!isHls) {
            isHls = true;
            callback.foundShoutcastStream(null, true);
        }

        while (!isStopped) {
            readBytes = inM3U.read(bufM3U, readBytesBuffer, bytesToRead);
            if (readBytes < 0) {
                break;
            }
            readBytesBuffer += readBytes;
            bytesToRead -= readBytes;
        }

        String s = new String(bufM3U, 0, readBytesBuffer, "utf-8");
        if (BuildConfig.DEBUG) Log.d(TAG, "read m3u:\n" + s);
        PlaylistM3U playlist = new PlaylistM3U(path, s);

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

                    streamFile(urlStr);
                }
            } else {
                Log.w(TAG, "URL Stream info:" + urlStr);
                URL u = new URL(urlStr);
                URLConnection connection = u.openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(10000);
                connection.connect();
                int sizeItem = connection.getHeaderFieldInt("Content-Length", 0);
                hlsStream(u, sizeItem, connection.getInputStream());
                break;
            }
        }
    }

    private void doConnectToStream() {
        try {
            final int MaxRetries = 100;
            int retry = MaxRetries;
            while (!isStopped && retry > 0) {
                try {
                    // connect to stream
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "doConnectToStream (try=" + retry + "):" + uri);
                    }

                    URL u = new URL(uri);
                    URLConnection connection = u.openConnection();
                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);
                    connection.setRequestProperty("Icy-MetaData", "1");
                    connection.connect();

                    if (BuildConfig.DEBUG) Log.d(TAG, "create serversocket..");
                    ServerSocket proxyServer = null;
                    proxyServer = new ServerSocket(0, 1, InetAddress.getLocalHost());
                    int port = proxyServer.getLocalPort();
                    localAdress = String.format(Locale.US, "http://localhost:%d", port);
                    if (BuildConfig.DEBUG) Log.d(TAG, "waiting..");

                    if (isStopped) {
                        break;
                    }

                    callback.streamCreated(localAdress);
                    socketProxy = proxyServer.accept();
                    proxyServer.close();

                    // send ok message to local mediaplayer
                    out = socketProxy.getOutputStream();
                    out.write(("HTTP/1.0 200 OK\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Content-Type: " + connection.getContentType() +
                            "\r\n\r\n").getBytes("utf-8"));

                    String type = connection.getHeaderField("Content-Type").toLowerCase();
                    Integer size = connection.getHeaderFieldInt("Content-Length", 0);
                    if (BuildConfig.DEBUG) Log.d(TAG, "Content Type: " + type);
                    if (type.equals("application/vnd.apple.mpegurl") || type.equals("application/x-mpegurl")) {
                        // Hls stream
                        hlsStream(u, size, connection.getInputStream());
                        // wait some time for next check for new files in stream
                        Thread.sleep(5000);
                    } else {
                        // try to get shoutcast information from stream connection
                        ShoutcastInfo info = ShoutcastInfo.Decode(connection);

                        in = connection.getInputStream();
                        defaultStream(info);
                    }
                    // reset retry count, if connection was ok
                    retry = MaxRetries;
                } catch (Exception e) {
                    Log.e(TAG, "Inside loop ex Proxy()", e);
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

    public void record(String stationName) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        if (getIsHls())
            outFileName = String.format("%2$s - %1$tY_%1$tm_%1$td_%1$tH_%1$tM_%1$tS.ts", calendar, Utils.sanitizeName(stationName));
        else
            outFileName = String.format("%2$s - %1$tY_%1$tm_%1$td_%1$tH_%1$tM_%1$tS.mp3", calendar, Utils.sanitizeName(stationName));
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

    Map<String, String> DecodeShoutcastMetadata(String metadata) {
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

    public String getLocalAdress() {
        return localAdress;
    }

    public void stop() {
        if (BuildConfig.DEBUG) Log.d(TAG, "stopping proxy.");
        isStopped = true;
    }

    private void stopInternal() {
        stopRecord();
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
                Log.e(TAG, "in.close() ", e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                Log.e(TAG, "out.close() ", e);
            }
        }
        if (socketProxy != null) {
            try {
                socketProxy.close();
            } catch (Exception e) {
                Log.e(TAG, "socketProxy.close() ", e);
            }
            socketProxy = null;
        }
        in = null;
        out = null;
    }

    public String getOutFileName() {
        return outFileName;
    }

    public long getTotalBytes() {
        return connectionBytesTotal;
    }
}
