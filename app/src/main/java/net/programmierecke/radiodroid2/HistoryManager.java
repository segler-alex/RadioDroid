package net.programmierecke.radiodroid2;

import android.content.Context;

import net.programmierecke.radiodroid2.station.DataRadioStation;

public class HistoryManager extends StationSaveManager{
    private static final int MAXSIZE = 25;

    @Override
    protected String getSaveId(){
        return "history";
    }

    public HistoryManager(Context ctx) {
        super(ctx);
    }

    @Override
    public void add(DataRadioStation station){
        DataRadioStation stationFromHistory = getById(station.StationUuid);
        if (stationFromHistory != null) {
            listStations.remove(stationFromHistory);
            listStations.add(0, stationFromHistory);
            Save();
            return;
        }

        cutList(MAXSIZE - 1);
        super.addFront(station);
    }

    private void cutList(int count){
        if (listStations.size() > count){
            listStations = listStations.subList(0,count);
        }
    }
}
