package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager extends StationSaveManager{
    int MAXSIZE = 25;

    @Override
    protected String getSaveId(){
        return "history";
    }

    public HistoryManager(Context ctx) {
        super(ctx);
    }

    @Override
    public void add(DataRadioStation station){
        cutList(MAXSIZE - 1);
        super.addFront(station);
    }

    public void cutList(int count){

        while (listStations.size() > count){
            listStations = listStations.subList(0,count);
        }
    }
}
