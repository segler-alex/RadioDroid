package net.programmierecke.radiodroid2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        RadioAlarmManager ram = new RadioAlarmManager(context,null);
        ram.resetAllAlarms();
    }
}
