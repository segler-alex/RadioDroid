package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

interface IMPDClientStatusChange{
    void changed();
}

public class MPDClient {
    static final String TAG = "MPD";
    public static boolean connected;
    public static boolean isPlaying;

    public static int StringToInt(String str, int defaultValue){
        try{
            return Integer.parseInt(str);
        }
        catch (Exception e){
            Log.e(TAG, "could not parse integer " + str, e);
        }
        return defaultValue;
    }

    public static void Connect(IMPDClientStatusChange listener){
        connected = true;
        listener.changed();
    }

    public static void Disconnect(Context context, IMPDClientStatusChange listener){
        connected = false;
        isPlaying = false;
        listener.changed();
        // Don't stop playback in that case. User can listen via MPD and via the app
        // User can stop playback via MPD with pause button
        //Stop(context);
    }

    public static void Play(final String url, final Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final String mpd_hostname = sharedPref.getString("mpd_hostname", null);
        final int mpd_port = StringToInt(sharedPref.getString("mpd_port", "6600"), 6600);

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return PlayRemoteMPD(mpd_hostname, mpd_port, url);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
            }
        }.execute();
    }

    static boolean discovered = false;
    static Thread t = null;
    static boolean discoveryActive = false;

    public static boolean Discovered(){
        return discovered;
    }

    public static void StartDiscovery(final Context context, final IMPDClientStatusChange listener){
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        if (t == null) {
            discoveryActive = true;
            t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(discoveryActive){
                        try {
                            final String mpd_hostname = sharedPref.getString("mpd_hostname", "").trim();
                            final int mpd_port = StringToInt(sharedPref.getString("mpd_port", "6600"), 6600);

                            if (!"".equals(mpd_hostname)) {
                                SetDiscoveredStatus(CheckConnection(mpd_hostname, mpd_port), listener);
                            }
                            // check every 5 seconds
                            Thread.sleep(5*1000);
                        } catch (Exception e) {
                            SetDiscoveredStatus(false, listener);
                        }
                    }
                    SetDiscoveredStatus(false, listener);
                    t = null;
                }
            });
            t.start();
        }
    }

    private static void SetDiscoveredStatus(boolean status, IMPDClientStatusChange listener){
        if (status != discovered){
            discovered = status;
            listener.changed();
        }
    }

    private static boolean CheckConnection(String mpd_hostname, int mpd_port) {
        Boolean result = false;

        try {
            if(BuildConfig.DEBUG) { Log.d(TAG, "Check connection..."); }
            Socket s = new Socket(mpd_hostname, mpd_port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            String info = reader.readLine();
            if(BuildConfig.DEBUG) { Log.d(TAG, info); }
            if (info.startsWith("OK")){
                result = true;
            }
            reader.close();
            writer.close();
            s.close();
        } catch (Exception e) {
            Log.e(TAG,e.toString());
        }
        if(BuildConfig.DEBUG) { Log.d(TAG, "Connection status:"+result); }
        return result;
    }


    public static void Stop(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final String mpd_hostname = sharedPref.getString("mpd_hostname", "").trim();
        final int mpd_port = StringToInt(sharedPref.getString("mpd_port", "6600"), 6600);
        new Thread(new Runnable() {
            @Override
            public void run() {
                StopInternal(mpd_hostname, mpd_port);
            }
        }).start();
    }

    private static boolean StopInternal(String mpd_hostname, int mpd_port) {
        Boolean result = false;
        isPlaying = false;
        try {
            if(BuildConfig.DEBUG) { Log.d(TAG, "Check connection..."); }
            Socket s = new Socket(mpd_hostname, mpd_port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            String info = reader.readLine();
            if(BuildConfig.DEBUG) { Log.d(TAG, info); }
            if (info.startsWith("OK")) {
                String cmd = "stop";
                writer.write(cmd);
                writer.newLine();
                writer.flush();
                result = true;
            }
            reader.close();
            writer.close();
            s.close();
        } catch (Exception e) {
            Log.e(TAG,e.toString());
        }
        if(BuildConfig.DEBUG) { Log.d(TAG, "Connection status:"+result); }
        return result;
    }

    public static void StopDiscovery(){
        discoveryActive = false;
        discovered = false;
        t = null;
    }

    private static Boolean PlayRemoteMPD(String mpd_hostname, int mpd_port, String url){
        Boolean result = false;
        isPlaying = true;
        try {
            if(BuildConfig.DEBUG) { Log.d("MPD", "Start"); }
            Socket s = new Socket(mpd_hostname, mpd_port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            String info = reader.readLine();
            if(BuildConfig.DEBUG) { Log.d("MPD", info); }
            if (info.startsWith("OK")){
                String cmd = "addid "+url;
                writer.write(cmd);
                writer.newLine();
                writer.flush();

                info = reader.readLine();
                if(BuildConfig.DEBUG) { Log.d("MPD", info); }
                if (info.startsWith("Id:")){
                    int songid = Integer.parseInt(info.substring(3).trim());
                    cmd = "playid "+songid;
                    writer.write(cmd);
                    writer.newLine();
                    writer.flush();
                    if(BuildConfig.DEBUG) { Log.d("MPD", "OK"); }
                    result = true;
                }
            }
            reader.close();
            writer.close();
            s.close();
        } catch (Exception e) {
            Log.e("MPD",e.toString());
        }
        return result;
    }

    public static boolean Connected() {
        return connected;
    }
}
