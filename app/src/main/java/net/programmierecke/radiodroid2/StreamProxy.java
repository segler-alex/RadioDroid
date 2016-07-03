package net.programmierecke.radiodroid2;

import android.content.Context;
import android.util.Log;

import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.interfaces.IStreamProxyEventReceiver;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
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

    private void doConnectToStream() {
        try{
            final int MaxRetries = 30;
            int retry = MaxRetries;
            while (!isStopped && retry > 0) {
                try {
                    // connect to stream
                    URLConnection connection = new URL(uri).openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(10000);
                    connection.setRequestProperty("Icy-MetaData", "1");
                    connection.connect();

                    in = connection.getInputStream();

                    // send ok message to local mediaplayer
                    out = socketProxy.getOutputStream();
                    out.write(("HTTP/1.0 200 OK\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Content-Type: " + connection.getContentType() +
                            "\r\n\r\n").getBytes("utf-8"));

                    // try to get shoutcast information from stream connection
                    ShoutcastInfo info = ShoutcastInfo.Decode(connection);

                    int bytesUntilMetaData = 0;
                    boolean readMetaData = false;
                    boolean filterOutMetaData = false;

                    if (info != null) {
                        callback.foundShoutcastStream(info);
                        bytesUntilMetaData = info.metadataOffset;
                        filterOutMetaData = true;
                    }

                    byte buf[] = new byte[163840];
                    byte bufMetadata[] = new byte[256 * 16];
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
                        // reset retry count, if connection was ok
                        retry = MaxRetries;
                    }
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
        outFileName = String.format("%2$s - %1$tY_%1$tm_%1$td_%1$tH_%1$tM_%1$tS.mp3", calendar, Utils.sanitizeName(stationName));
        recordInternal(outFileName);
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
