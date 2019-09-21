package net.programmierecke.radiodroid2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.programmierecke.radiodroid2.alarm.RadioAlarmManager;

public class BootReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        RadioDroidApp radioDroidApp = (RadioDroidApp)context.getApplicationContext();
        radioDroidApp.getAlarmManager().resetAllAlarms();
    }
}
