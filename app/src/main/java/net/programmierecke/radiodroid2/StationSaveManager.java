package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import net.programmierecke.radiodroid2.data.DataRadioStation;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class StationSaveManager {
    Context context;
    List<DataRadioStation> listStations = new ArrayList<DataRadioStation>();

    public StationSaveManager(Context ctx){
        this.context = ctx;
        Load();
    }

    protected String getSaveId(){
        return "default";
    }

    public void add(DataRadioStation station){
        listStations.add(station);
        Save();
    }

    public void addFront(DataRadioStation station){
        listStations.add(0, station);
        Save();
    }

    DataRadioStation getById(String id){
        for(DataRadioStation station: listStations){
            if (id.equals(station.ID)){
                return station;
            }
        }
        return null;
    }

    public void remove(String id){
        if (has(id)) {
            listStations.remove(getById(id));
            Save();
        }
    }

    public boolean has(String id){
        DataRadioStation station = getById(id);
        if (station != null){
            return true;
        }
        return false;
    }

    public DataRadioStation[] getList(){
        return listStations.toArray(new DataRadioStation[0]);
    }

    void Load(){
        listStations.clear();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String str = sharedPref.getString(getSaveId(), null);
        if (str != null){
            DataRadioStation[] arr =DataRadioStation.DecodeJson(str);
            for (int i=0;i<arr.length;i++) {
                listStations.add(arr[i]);
            }
        }else{
            Log.w("SAVE","Load() no stations to load");
        }
    }

    void Save(){
        JSONArray arr = new JSONArray();
        for (DataRadioStation station: listStations){
            arr.put(station.toJson());
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        String str = arr.toString();
        Log.w("SAVE","wrote: "+str);
        editor.putString(getSaveId(), str);
        editor.commit();
    }
}
