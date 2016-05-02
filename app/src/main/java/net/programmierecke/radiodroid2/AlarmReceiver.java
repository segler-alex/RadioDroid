package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
    ProgressDialog itsProgressLoading;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w("abc","received broadcast");
        Toast toast = Toast.makeText(context, "Alarm!", Toast.LENGTH_SHORT);
        toast.show();

        Intent anIntent = new Intent(context, PlayerService.class);
        context.getApplicationContext().bindService(anIntent, svcConn, context.BIND_AUTO_CREATE);
        context.getApplicationContext().startService(anIntent);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String stationId = sharedPref.getString("alarm.id", null);
        String stationName = sharedPref.getString("alarm.name", null);

        if (stationId != null && stationName != null) {
            Play(context, stationId, stationName);
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
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.v("", "Service offline");
            itsPlayerService = null;
        }
    };

    private void Play(final Context context, final String stationId, final String stationName) {
        if (itsPlayerService != null) {
            itsProgressLoading = ProgressDialog.show(context, "", "Loading...");
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    return Utils.getRealStationLink(context, stationId);
                }

                @Override
                protected void onPostExecute(String result) {
                    itsProgressLoading.dismiss();

                    if (result != null) {
                        try {
                            itsPlayerService.Play(result, stationName, stationId);
                        } catch (RemoteException e) {
                            Log.e("", "" + e);
                        }
                    } else {
                        Toast toast = Toast.makeText(context, context.getResources().getText(R.string.error_station_load), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    super.onPostExecute(result);
                }
            }.execute();
        }
    }
}
