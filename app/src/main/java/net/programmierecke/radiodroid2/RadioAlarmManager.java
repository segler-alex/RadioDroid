package net.programmierecke.radiodroid2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RadioAlarmManager {

    Context context;
    List<DataRadioStationAlarm> list = new ArrayList<DataRadioStationAlarm>();

    public RadioAlarmManager(Context context){
        this.context = context;
        load();
    }

    public void add(DataRadioStation station, int hour, int minute){
        DataRadioStationAlarm alarm = new DataRadioStationAlarm();
        alarm.station = station;
        alarm.hour = hour;
        alarm.minute = minute;
        alarm.id = getFreeId();
        list.add(alarm);

        save();
    }

    public DataRadioStationAlarm[] getList(){
        return list.toArray(new DataRadioStationAlarm[0]);
    }

    int getFreeId(){
        int i = 0;
        while (!checkIdFree(i)){
            i++;
        }
        return i;
    }

    boolean checkIdFree(int id){
        for(DataRadioStationAlarm alarm: list){
            if (alarm.id == id){
                return false;
            }
        }
        return true;
    }

    void save(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();

        String items = "";

        for (DataRadioStationAlarm alarm: list){
            editor.putString("alarm."+alarm.id+".station",alarm.station.toJson().toString());
            editor.putInt("alarm."+alarm.id+".timeHour",alarm.hour);
            editor.putInt("alarm."+alarm.id+".timeMinutes",alarm.minute);
            editor.putBoolean("alarm."+alarm.id+".enabled",alarm.enabled);

            if (items.equals("")) {
                items = items + "," + alarm.id;
            }else{
                items = "" + alarm.id;
            }
        }

        editor.putString("alarm.ids",items);
        editor.commit();
    }

    void load(){
        list.clear();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String ids = sharedPref.getString("alarm.ids", null);
        if (ids != null){
            String[] idsArr = ids.split(",");
            for (String id: idsArr){
                DataRadioStationAlarm alarm = new DataRadioStationAlarm();

                alarm.station = DataRadioStation.DecodeJsonSingle(sharedPref.getString("alarm."+id+".station",null));
                alarm.hour = sharedPref.getInt("alarm."+id+".timeHour",0);
                alarm.minute = sharedPref.getInt("alarm."+id+".timeMinutes",0);
                alarm.enabled = sharedPref.getBoolean("alarm."+id+".enabled",false);

                if (alarm.station != null){
                    list.add(alarm);
                }
            }
        }
    }

    public void setEnabled(int alarmId, boolean enabled) {
        DataRadioStationAlarm alarm = getById(alarmId);
        if (alarm != null) {
            if (enabled != alarm.enabled) {
                alarm.enabled = enabled;
                save();

                if (enabled){
                    start(alarmId);
                }else{
                    stop(alarmId);
                }
            }
        }
    }

    DataRadioStationAlarm getById(int id){
        for(DataRadioStationAlarm alarm: list){
            if (id == alarm.id){
                return alarm;
            }
        }
        return null;
    }

    void start(int alarmId) {
        DataRadioStationAlarm alarm = getById(alarmId);
        if (alarm != null) {
            stop(alarmId);

            Log.w("ALARM","started:"+alarmId);
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, alarmId, intent, 0);
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
            calendar.set(Calendar.MINUTE, alarm.minute);
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 1000 * 60 * 60 * 24, alarmIntent);
        }
    }

    void stop(int alarmId) {
        DataRadioStationAlarm alarm = getById(alarmId);
        if (alarm != null) {
            Log.w("ALARM","stopped:"+alarmId);
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, alarmId, intent, 0);
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.cancel(alarmIntent);
        }
    }

    public void changeTime(int alarmId, int hourOfDay, int minute) {
        DataRadioStationAlarm alarm = getById(alarmId);
        if (alarm != null) {
            alarm.hour = hourOfDay;
            alarm.minute = minute;
            save();

            if (alarm.enabled){
                stop(alarmId);
                start(alarmId);
            }
        }
    }
}
