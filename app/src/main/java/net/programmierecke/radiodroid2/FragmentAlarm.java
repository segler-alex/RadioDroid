package net.programmierecke.radiodroid2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.Calendar;

public class FragmentAlarm extends Fragment {
    View view;

    public FragmentAlarm() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.layout_alarm, container, false);

        SharedPreferences.OnSharedPreferenceChangeListener spChanged = new
                SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                          String key) {
                        Log.e("a","changed prefs");
                        UpdateOutput();
                    }
                };

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        sharedPref.registerOnSharedPreferenceChangeListener(spChanged);

        SwitchCompat s = (SwitchCompat)view.findViewById(R.id.switch1);
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.w("ALARM","new state:"+isChecked);

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("alarm.enabled",isChecked);
                editor.commit();

                if (isChecked) {
                    startAlarm();
                    DialogFragment newFragment = new TimePickerFragment();
                    newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");
                }else{
                    stopAlarm();
                }
            }
        });

        UpdateOutput();

        return view;
    }

    private void UpdateOutput() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        String stationId = sharedPref.getString("alarm.id", null);
        String stationName = sharedPref.getString("alarm.name", null);

        int timeHour = sharedPref.getInt("alarm.timeHour", 0);
        int timeMinutes = sharedPref.getInt("alarm.timeMinutes", 0);

        TextView tvTime = (TextView)view.findViewById(R.id.textViewTime);
        TextView tvStation = (TextView)view.findViewById(R.id.textViewStationName);

        tvTime.setText(String.format("%02d:%02d",timeHour,timeMinutes));
        tvStation.setText(stationName);
    }

    void startAlarm(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        int timeHour = sharedPref.getInt("alarm.timeHour", 0);
        int timeMinutes = sharedPref.getInt("alarm.timeMinutes", 0);

        Intent intent = new Intent(getActivity(), AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
        AlarmManager alarmMgr = (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, timeHour);
        calendar.set(Calendar.MINUTE, timeMinutes);
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 1000 *1, alarmIntent);
    }

    void stopAlarm(){
        Intent intent = new Intent(getActivity(), AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
        AlarmManager alarmMgr = (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(alarmIntent);
    }
}