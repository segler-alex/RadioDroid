package net.programmierecke.radiodroid2;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
    String url;
    int alarmId;
    DataRadioStation station;
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();

        Log.w("recv","received broadcast");
        Toast toast = Toast.makeText(context, "Alarm!", Toast.LENGTH_SHORT);
        toast.show();

        alarmId = intent.getIntExtra("id",-1);
        Log.w("recv","alarm id:"+alarmId);

        RadioAlarmManager ram = new RadioAlarmManager(context.getApplicationContext(),null);
        station = ram.getStation(alarmId);
        ram.resetAllAlarms();

        if (station != null && alarmId >= 0) {
            Log.w("recv","radio id:"+alarmId);
            Play(context, station.ID);
        }else{
            toast = Toast.makeText(context, "not enough info for alarm!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    IPlayerService itsPlayerService;
    private ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.v("", "Service came online");
            itsPlayerService = IPlayerService.Stub.asInterface(binder);
            try {
                itsPlayerService.Play(url, station.Name, station.ID);
                // default timeout 1 hour
                itsPlayerService.addTimer(60*60);
            } catch (RemoteException e) {
                Log.e("recv","play error:"+e);
            }

            wakeLock.release();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.v("", "Service offline");
            itsPlayerService = null;
        }
    };

    private void Play(final Context context, final String stationId) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return Utils.getRealStationLink(context, stationId);
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    url = result;

                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    boolean play_external = sharedPref.getBoolean("play_external", false);
                    String packageName = sharedPref.getString("shareapp_package",null);
                    String activityName = sharedPref.getString("shareapp_activity",null);
                    if (play_external && packageName != null && activityName != null){
                        Intent share = new Intent(Intent.ACTION_VIEW);
                        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        share.setClassName(packageName,activityName);
                        share.setDataAndType(Uri.parse(url), "audio/*");
                        context.startActivity(share);
                        wakeLock.release();
                    }else {
                        Intent anIntent = new Intent(context, PlayerService.class);
                        context.getApplicationContext().bindService(anIntent, svcConn, context.BIND_AUTO_CREATE);
                        context.getApplicationContext().startService(anIntent);
                    }
                } else {
                    Toast toast = Toast.makeText(context, context.getResources().getText(R.string.error_station_load), Toast.LENGTH_SHORT);
                    toast.show();
                    wakeLock.release();
                }
                super.onPostExecute(result);
            }
        }.execute();
    }
}
