package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import net.programmierecke.radiodroid2.data.DataRadioStation;

import org.json.JSONArray;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StationSaveManager {
    Context context;
    List<DataRadioStation> listStations = new ArrayList<DataRadioStation>();

    public StationSaveManager(Context ctx) {
        this.context = ctx;
        Load();
    }

    protected String getSaveId() {
        return "default";
    }

    public void add(DataRadioStation station) {
        listStations.add(station);
        Save();
    }

    public void addFront(DataRadioStation station) {
        listStations.add(0, station);
        Save();
    }

    DataRadioStation getById(String id) {
        for (DataRadioStation station : listStations) {
            if (id.equals(station.ID)) {
                return station;
            }
        }
        return null;
    }

    public int remove(String id) {
        for (int i = 0; i < listStations.size(); i++) {
            if (listStations.get(i).ID.equals(id)) {
                listStations.remove(i);
                Save();
                return i;
            }
        }

        return -1;
    }

    public void restore(DataRadioStation station, int pos) {
        listStations.add(pos, station);
        Save();
    }

    public void clear() {
        listStations.clear();
        Save();
    }

    public int size(){
        return listStations.size();
    }

    public boolean isEmpty(){
        return listStations.size() == 0;
    }

    public boolean has(String id){
        DataRadioStation station = getById(id);
        return station != null;
    }

    public DataRadioStation[] getList() {
        return listStations.toArray(new DataRadioStation[0]);
    }

    void Load() {
        listStations.clear();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String str = sharedPref.getString(getSaveId(), null);
        if (str != null) {
            DataRadioStation[] arr = DataRadioStation.DecodeJson(str);
            Collections.addAll(listStations, arr);
        } else {
            Log.w("SAVE", "Load() no stations to load");
        }
    }

    void Save() {
        JSONArray arr = new JSONArray();
        for (DataRadioStation station : listStations) {
            arr.put(station.toJson());
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        String str = arr.toString();
        if (BuildConfig.DEBUG) {
            Log.d("SAVE", "wrote: " + str);
        }
        editor.putString(getSaveId(), str);
        editor.commit();
    }

    public static String getSaveDir() {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "";
        File folder = new File(path);
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                Log.e("SAVE", "could not create dir:" + path);
            }
        }
        return path;
    }

    public void SaveM3U(){
        String fileName = "radiodroid.m3u";
        String filePath = StationSaveManager.getSaveDir() + "";
        try {
            File f = new File(filePath, fileName);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, false));
            bw.write("#EXTM3U\n");
            for (DataRadioStation station : listStations) {
                bw.write("#EXTINF:-1," + station.Name + "\n");
                bw.write(station.StreamUrl + "\n\n");
            }
            bw.flush();
            bw.close();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))));
            }else {
                MediaScannerConnection
                        .scanFile(context, new String[]{f.getAbsolutePath()}, null, null);
            }

            Toast toast = Toast.makeText(context, "Wrote " + f.getAbsolutePath(), Toast.LENGTH_SHORT);
            toast.show();
        }
        catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());

            Toast toast = Toast.makeText(context, "Write failed: " + filePath + "/" + fileName, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
