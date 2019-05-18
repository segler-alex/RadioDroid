package net.programmierecke.radiodroid2.alarm;

import net.programmierecke.radiodroid2.station.DataRadioStation;

import java.util.ArrayList;

public class DataRadioStationAlarm {
    public DataRadioStation station;
    public int id;
    public int hour;
    public int minute;
    public boolean repeating;
    public ArrayList<Integer> weekDays;
    public boolean enabled;

}
