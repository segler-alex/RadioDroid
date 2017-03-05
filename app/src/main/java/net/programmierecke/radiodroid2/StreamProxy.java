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
    private Context context;
    private FileOutputStream fileOutputStream;
    private boolean isStopped = false;
    private String outFileName = null;
    final String TAG = "PROXY";
    boolean isHls = false;

    public StreamProxy(Context context, String uri, IStreamProxyEventReceiver callback) {
        this.context = context;
        this.uri = uri;
        this.callback = callback;

        createProxy();
    }

    private void createProxy() {
        Log.i(TAG,"thread started");
        ServerSocket proxyServer = null;
        try {
            proxyServer = new ServerSocket(0, 1, InetAddress.getLocalHost());
            int port = proxyServer.getLocalPort();
            localAdress = String.format(Locale.US,"http://localhost:%d",port);
        } catch (IOException e) {
            Log.e(TAG,"createProxy() create server socket: "+e);
        }

        if (proxyServer != null) {
            final ServerSocket finalProxyServer = proxyServer;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.i(TAG, "waiting..");
                        socketProxy = finalProxyServer.accept();
                        finalProxyServer.close();

                        doConnectToStream();

                        Log.i(TAG, "createProxy() ended");
                    } catch (IOException e) {
                        Log.e(TAG, "" + e);
                    }
                }
            }).start();

            while (localAdress == null) {
                try {
                    Log.i(TAG, "starting serversock...");
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
        }
    }

    InputStream in;
    OutputStream out;

    byte buf[] = new byte[16384*2];
    byte bufMetadata[] = new byte[256 * 16];

    private void defaultStream(ShoutcastInfo info) throws Exception{
        int bytesUntilMetaData = 0;
        boolean readMetaData = false;
        boolean filterOutMetaData = false;

        if (info != null) {
            callback.foundShoutcastStream(info,false);
            bytesUntilMetaData = info.metadataOffset;
            filterOutMetaData = true;
        }

        int readBytesBuffer = 0;
        int readBytesBufferMetadata = 0;

        while (!isStopped) {
            int readBytes = 0;
            if (!filterOutMetaData || (bytesUntilMetaData > 0)) {
                int bytesToRead = buf.length - readBytesBuffer;
                if (filterOutMetaData) {
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
                if (filterOutMetaData) {
                    bytesUntilMetaData -= readBytes;
                }

                Log.v(TAG, "in:" + readBytes);
                if (readBytesBuffer > buf.length / 2) {
                    Log.v(TAG, "out:" + readBytesBuffer);
                    out.write(buf, 0, readBytesBuffer);
                    if (fileOutputStream != null) {
                        Log.v(TAG, "writing to record file..");
                        fileOutputStream.write(buf, 0, readBytesBuffer);
                    }
                    readBytesBuffer = 0;
                }
            } else {
                int metadataBytes = in.read() * 16;
                int metadataBytesToRead = metadataBytes;
                readBytesBufferMetadata = 0;
                bytesUntilMetaData = info.metadataOffset;
                Log.d(TAG, "metadata size:" + metadataBytes);
                if (metadataBytes > 0) {
                    Arrays.fill(bufMetadata, (byte) 0);
                    while (true) {
                        readBytes = in.read(bufMetadata, readBytesBufferMetadata, metadataBytesToRead);
                        if (readBytes == 0) {
                            continue;
                        }
                        if (readBytes < 0) {
                            break;
                        }
                        metadataBytesToRead -= readBytes;
                        readBytesBufferMetadata += readBytes;
                        if (metadataBytesToRead <= 0) {
                            String s = new String(bufMetadata, 0, metadataBytes, "utf-8");
                            Log.d(TAG, "METADATA:" + s);
                            Map<String, String> dict = DecodeShoutcastMetadata(s);
                            Log.d(TAG, "META:" + dict.get("StreamTitle"));
                            callback.foundLiveStreamInfo(dict);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void streamFile(String urlStr) throws IOException {
        Log.i(TAG,"URL Stream Data:   "+urlStr);
        URL u = new URL(urlStr);
        URLConnection connection = u.openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(10000);
        connection.connect();
        InputStream inContent = connection.getInputStream();
        boolean recordActive = false;
        if (fileOutputStream != null){
            recordActive = true;
        }

        byte[] bufContent = new byte[1000];
        while(!isStopped){
            int bytesRead = inContent.read(bufContent);
            if (bytesRead < 0){
                break;
            }
            connectionBytesTotal+=bytesRead;
            out.write(bufContent,0,bytesRead);
            if ((fileOutputStream != null) && recordActive) {
                Log.v(TAG, "writing to record file..");
                fileOutputStream.write(bufContent, 0, bytesRead);
            }
        }
    }

    boolean containsString(ArrayList<String> list, String item){
        for (int i=0;i<list.size();i++){
            if (item.equals(list.get(i))){
                return true;
            }
        }
        return false;
    }

    ArrayList<String> streamedFiles = new ArrayList<String>();

    private void hlsStream(URL path, int size, InputStream inM3U) throws Exception{
        int readBytes = 0;
        int readBytesBuffer = 0;
        int bytesToRead = size;
        byte bufM3U[] = new byte[size];

        if (!isHls){
            isHls = true;
            callback.foundShoutcastStream(null,true);
        }

        while (!isStopped){
            readBytes = inM3U.read(bufM3U, readBytesBuffer, bytesToRead);
            if (readBytes < 0){
                break;
            }
            readBytesBuffer += readBytes;
            bytesToRead -= readBytes;
        }

        String s = new String(bufM3U, 0, readBytesBuffer, "utf-8");
        Log.d(TAG,"read m3u:\n"+s);
        PlaylistM3U playlist = new PlaylistM3U(path, s);

        PlaylistM3UEntry[] entries = playlist.getEntries();
        for (int i=0;i<entries.length;i++){
            PlaylistM3UEntry entry = entries[i];
            String urlStr = entry.getContent();

            if (!entry.getIsStreamInformation()){
                // use this url in the future
                uri = path.toString();

                String urlWithoutQuery = urlStr;
                int indexQuery = urlStr.indexOf("?");
                if (indexQuery >= 0){
                    urlWithoutQuery = urlStr.substring(0,indexQuery);
                }

                if (!containsString(streamedFiles,urlWithoutQuery)){
                    Log.w(TAG,"did not find in db:"+urlWithoutQuery);
                    streamedFiles.add(urlWithoutQuery);

                    streamFile(urlStr);
                    continue;
                }
            } else {
                Log.w(TAG,"URL Stream info:"+urlStr);
                URL u = new URL(urlStr);
                URLConnection connection = u.openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(10000);
                connection.connect();
                int sizeItem = connection.getHeaderFieldInt("Content-Length",0);
                hlsStream(u,sizeItem,connection.getInputStream());
                break;
            }
        }
    }

    private void doConnectToStream() {
        try{
            final int MaxRetries = 30;
            int retry = MaxRetries;
            while (!isStopped && retry > 0) {
                try {
                    // connect to stream
                    Log.i(TAG,"doConnectToStream:"+uri);
                    URL u = new URL(uri);
                    URLConnection connection = u.openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(10000);
                    connection.setRequestProperty("Icy-MetaData", "1");
                    connection.connect();

                    // send ok message to local mediaplayer
                    out = socketProxy.getOutputStream();
                    out.write(("HTTP/1.0 200 OK\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Content-Type: " + connection.getContentType() +
                            "\r\n\r\n").getBytes("utf-8"));

                    String type = connection.getHeaderField("Content-Type").toLowerCase();
                    Integer size = connection.getHeaderFieldInt("Content-Length",0);
                    Log.d(TAG,"Content Type: "+type);
                    if (type.equals("application/vnd.apple.mpegurl") || type.equals("application/x-mpegurl")){
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
                    Log.e(TAG, "Inside loop ex Proxy()" + e);
                }

                retry--;
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Log.e(TAG,"Interrupted ex Proxy() "+e);
        }
        // inform outside if stream stopped, only if outside did not initiate stop
        if (!isStopped){
            callback.streamStopped();
        }
        stop();
    }

    public void record(String stationName){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        if (getIsHls())
            outFileName = String.format("%2$s - %1$tY_%1$tm_%1$td_%1$tH_%1$tM_%1$tS.ts", calendar, Utils.sanitizeName(stationName));
        else
            outFileName = String.format("%2$s - %1$tY_%1$tm_%1$td_%1$tH_%1$tM_%1$tS.mp3", calendar, Utils.sanitizeName(stationName));
        recordInternal(outFileName);
    }

    public boolean getIsHls(){
        return isHls;
    }

    void recordInternal(String fileName){
        if (fileOutputStream == null) {
            try {
                String path = RecordingsManager.getRecordDir() + "/" + fileName;
                Log.i(TAG,"start recording to :"+fileName + " in dir " + path);
                fileOutputStream = new FileOutputStream(path);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "record('"+fileName+"'): " + e);
            }
        }
    }

    public void stopRecord() {
        if (fileOutputStream != null){
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG,"stopRecord():"+e);
            }
            outFileName = null;
            fileOutputStream = null;
        }
    }

    Map<String,String> DecodeShoutcastMetadata(String metadata){
        // icecast server does not encode "'" inside strings. so i am not able to check when a string ends
        //boolean stringStartedSingle = false;
        //boolean stringStartedDouble = false;
        String key = "";
        String value = "";
        boolean valueActive = false;

        Map<String,String> dict = new HashMap<String,String>();
        for (int i=0;i<metadata.length();i++){
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
            } else */{
                if (c == '\''){
                    //stringStartedSingle = true;
                } else if (c == '"'){
                    //stringStartedDouble = true;
                } else if (c == '=') {
                    valueActive = true;
                } else if (c == ';'){
                    valueActive = false;
                    dict.remove(key);
                    dict.put(key,value);
                    key = "";
                    value = "";
                } else {
                    if (valueActive){
                        value += c;
                    } else{
                        key += c;
                    }
                }
            }

        }
        if (valueActive){
            dict.remove(key);
            dict.put(key,value);
        }
        return dict;
    }

    public String getLocalAdress() {
        return localAdress;
    }

    public void stop() {
        Log.i(TAG,"stop()");
        isStopped = true;
        stopRecord();
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
                Log.e(TAG,"stop() in.close() "+e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                Log.e(TAG,"stop() out.close() "+e);
            }
        }
        if (socketProxy != null){
            try {
                socketProxy.close();
            } catch (Exception e) {
                Log.e(TAG,"stop() socketProxy.close() "+e);
            }
            socketProxy = null;
        }
        in = null;
        out = null;
    }

    public String getOutFileName() {
        return outFileName;
    }

    public long getTotalBytes(){
        return connectionBytesTotal;
    }
}
