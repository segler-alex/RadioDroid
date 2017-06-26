package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import net.programmierecke.radiodroid2.data.DataRadioStation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class MPDClient {
    public static int StringToInt(String str, int defaultValue){
        try{
            return Integer.parseInt(str);
        }
        catch (Exception e){
        }
        return defaultValue;
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

    private static Boolean PlayRemoteMPD(String mpd_hostname, int mpd_port, String url){
        Boolean result = false;
        try {
            Log.i("MPD", "Start");
            Socket s = new Socket(mpd_hostname, mpd_port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            String info = reader.readLine();
            Log.i("MPD", info);
            if (info.startsWith("OK")){
                String cmd = "addid "+url;
                writer.write(cmd);
                writer.newLine();
                writer.flush();

                info = reader.readLine();
                Log.i("MPD", info);
                if (info.startsWith("Id:")){
                    int songid = Integer.parseInt(info.substring(3).trim());
                    cmd = "playid "+songid;
                    writer.write(cmd);
                    writer.newLine();
                    writer.flush();
                    Log.i("MPD", "OK");
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
}
