package net.programmierecke.radiodroid2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RadioAlarmManager {

    IChanged callback;
    Context context;
    List<DataRadioStationAlarm> list = new ArrayList<DataRadioStationAlarm>();

    public RadioAlarmManager(Context context, final IChanged callback){
        this.context = context;
        load();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (callback != null) {
                    callback.onChanged();
                }
            }
        };
        sharedPref.registerOnSharedPreferenceChangeListener(listener);
    }

    public void add(DataRadioStation station, int hour, int minute){
        Log.i("alarm","added station:"+station.Name);
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
        Log.w("alarm","new free id:"+i);
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
            Log.i("alarm","save item:"+alarm.id+"/"+alarm.station.Name);
            editor.putString("alarm."+alarm.id+".station",alarm.station.toJson().toString());
            editor.putInt("alarm."+alarm.id+".timeHour",alarm.hour);
            editor.putInt("alarm."+alarm.id+".timeMinutes",alarm.minute);
            editor.putBoolean("alarm."+alarm.id+".enabled",alarm.enabled);

            if (items.equals("")) {
                items = "" + alarm.id;
            }else{
                items = items + "," + alarm.id;
            }
        }

        editor.putString("alarm.ids",items);
        editor.commit();
    }

    public void load(){
        list.clear();
        Log.w("alarm","load()");

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String ids = sharedPref.getString("alarm.ids", "");
        if (!ids.equals("")) {
            String[] idsArr = ids.split(",");
            Log.w("alarm", "load() - " + idsArr.length);
            for (String id : idsArr) {
                DataRadioStationAlarm alarm = new DataRadioStationAlarm();

                alarm.station = DataRadioStation.DecodeJsonSingle(sharedPref.getString("alarm." + id + ".station", null));
                alarm.hour = sharedPref.getInt("alarm." + id + ".timeHour", 0);
                alarm.minute = sharedPref.getInt("alarm." + id + ".timeMinutes", 0);
                alarm.enabled = sharedPref.getBoolean("alarm." + id + ".enabled", false);
                try {
                    alarm.id = Integer.parseInt(id);
                    if (alarm.station != null) {
                        list.add(alarm);
                    }
                } catch (Exception e) {
                    Log.e("alarm", "could not decode:" + id);
                }
            }
        }else{
            Log.w("alarm","empty load() string");
        }
    }

    public void setEnabled(int alarmId, boolean enabled) {
        DataRadioStationAlarm alarm = getById(alarmId);
        if (alarm != null) {
            if (enabled != alarm.enabled) {
                alarm.enabled = enabled;
                save();
                if (callback != null){
                    callback.onChanged();
                }

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

            Log.w("ALARM","started:"+alarmId + " "+alarm.hour+":"+alarm.minute);
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("id",alarmId);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
            calendar.set(Calendar.MINUTE, alarm.minute);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.w("alarm","START setExactAndAllowWhileIdle");
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),alarmIntent);
            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.w("alarm","START setAlarmClock");
                alarmMgr.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(),alarmIntent),alarmIntent);
            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Log.w("alarm","START setExact");
                alarmMgr.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
            }else{
                Log.w("alarm","START set");
                alarmMgr.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),alarmIntent);
            }
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
            if (callback != null){
                callback.onChanged();
            }

            if (alarm.enabled){
                stop(alarmId);
                start(alarmId);
            }
        }
    }

    public void remove(int id) {
        DataRadioStationAlarm alarm = getById(id);
        if (alarm != null) {
            stop(id);
            list.remove(alarm);
            save();
            if (callback != null){
                callback.onChanged();
            }
        }
    }

    public DataRadioStation getStation(int stationId) {
        DataRadioStationAlarm alarm = getById(stationId);
        if (alarm != null) {
            return alarm.station;
        }
        return null;
    }

    public void resetAllAlarms() {
        for(DataRadioStationAlarm alarm: list){
            if (alarm.enabled){
                Log.w("alarm","started alarm with id:"+alarm.id);
                start(alarm.id);
            }
        }
    }
}
