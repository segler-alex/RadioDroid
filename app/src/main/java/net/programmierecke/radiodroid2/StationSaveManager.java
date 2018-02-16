package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import net.programmierecke.radiodroid2.data.DataRadioStation;

import org.json.JSONArray;

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
}
