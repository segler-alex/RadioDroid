package net.programmierecke.radiodroid2.alarm;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.IPlayerService;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.service.PlayerService;
import net.programmierecke.radiodroid2.station.DataRadioStation;

import okhttp3.OkHttpClient;

public class AlarmReceiver extends BroadcastReceiver {
    String url;
    int alarmId;
    DataRadioStation station;
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private final String TAG = "RECV";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "received broadcast");
        }
        aquireLocks(context);

        Toast toast = Toast.makeText(context, context.getResources().getText(R.string.alert_alarm_working), Toast.LENGTH_SHORT);
        toast.show();

        alarmId = intent.getIntExtra("id", -1);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "alarm id:" + alarmId);
        }

        RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
        RadioAlarmManager ram = radioDroidApp.getAlarmManager();
        station = ram.getStation(alarmId);
        ram.resetAllAlarms();

        if (station != null && alarmId >= 0) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "radio id:" + alarmId);
            }
            Play(context, station.StationUuid);
        } else {
            toast = Toast.makeText(context, context.getResources().getText(R.string.alert_alarm_not_working), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void aquireLocks(Context context) {
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmReceiver:");
        }
        if (!wakeLock.isHeld()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "acquire wakelock");
            }
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        }
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            if (wifiLock == null) {
                wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "AlarmReceiver");
            }
            if (!wifiLock.isHeld()) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "acquire wifilock");
                }
                wifiLock.acquire();
            }
        } else {
            Log.e(TAG, "could not acquire wifi lock");
        }
    }

    private void releaseLocks() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "release wakelock");
            }
        }
        if (wifiLock != null) {
            wifiLock.release();
            wifiLock = null;
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "release wifilock");
            }
        }
    }

    IPlayerService itsPlayerService;
    private ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Service came online");
            }
            itsPlayerService = IPlayerService.Stub.asInterface(binder);
            try {
                station.playableUrl = url;
                itsPlayerService.SetStation(station);
                itsPlayerService.Play(true);
                // default timeout 1 hour
                itsPlayerService.addTimer(timeout * 60);
            } catch (RemoteException e) {
                Log.e(TAG, "play error:" + e);
            }

            releaseLocks();
        }

        public void onServiceDisconnected(ComponentName className) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Service offline");
            }
            itsPlayerService = null;
        }
    };

    int timeout = 10;

    private void Play(final Context context, final String stationId) {
        RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
        final OkHttpClient httpClient = radioDroidApp.getHttpClient();

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String result;
                for (int i = 0; i < 20; i++) {
                    result = Utils.getRealStationLink(httpClient, context, stationId);
                    if (result != null) {
                        return result;
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Play() " + e);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    url = result;

                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    boolean play_external = sharedPref.getBoolean("alarm_external", false);
                    String packageName = sharedPref.getString("shareapp_package", null);
                    String activityName = sharedPref.getString("shareapp_activity", null);
                    try {
                        timeout = Integer.parseInt(sharedPref.getString("alarm_timeout", "10"));
                    } catch (Exception e) {
                        timeout = 10;
                    }
                    if (play_external && packageName != null && activityName != null) {
                        Intent share = new Intent(Intent.ACTION_VIEW);
                        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        share.setClassName(packageName, activityName);
                        share.setDataAndType(Uri.parse(url), "audio/*");
                        context.startActivity(share);
                        if (wakeLock != null) {
                            wakeLock.release();
                            wakeLock = null;
                        }
                        if (wifiLock != null) {
                            wifiLock.release();
                            wifiLock = null;
                        }
                    } else {
                        Intent anIntent = new Intent(context, PlayerService.class);
                        context.getApplicationContext().bindService(anIntent, svcConn, Context.BIND_AUTO_CREATE);
                        context.getApplicationContext().startService(anIntent);
                    }
                } else {
                    Toast toast = Toast.makeText(context, context.getResources().getText(R.string.error_station_load), Toast.LENGTH_SHORT);
                    toast.show();
                    if (wakeLock != null) {
                        wakeLock.release();
                        wakeLock = null;
                    }
                    if (wifiLock != null) {
                        wifiLock.release();
                        wifiLock = null;
                    }
                }
                super.onPostExecute(result);
            }
        }.execute();
    }
}
