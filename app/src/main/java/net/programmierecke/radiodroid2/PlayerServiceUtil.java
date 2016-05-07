package net.programmierecke.radiodroid2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class PlayerServiceUtil {
    public static void bind(Context context){
        Intent anIntent = new Intent(context, PlayerService.class);
        context.bindService(anIntent, svcConn, Context.BIND_AUTO_CREATE);
        context.startService(anIntent);
    }

    public static void unBind(Context context){
        PlayerService thisService = new PlayerService();
        thisService.unbindSafely(context, svcConn);
    }

    static IPlayerService itsPlayerService;
    private static ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.i("PLAYER", "Service came online");
            itsPlayerService = IPlayerService.Stub.asInterface(binder);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i("PLAYER", "Service offline");
            itsPlayerService = null;
        }
    };

    public static boolean isPlaying(){
        if (itsPlayerService != null){
            try {
                return itsPlayerService.getCurrentStationID() != null;
            } catch (RemoteException e) {
            }
        }
        return false;
    }

    public static void stop() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.Stop();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
    }

    public static void play(String result, String name, String id) {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.Play(result,name,id);
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
    }

    public static void clearTimer() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.clearTimer();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
    }

    public static void addTimer(int secondsAdd) {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.addTimer(secondsAdd);
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
    }

    public static long getTimerSeconds() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getTimerSeconds();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return 0;
    }

    public static Map<String,String> getMetadataLive() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getMetadataLive();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return new HashMap<String,String>();
    }

    public static String getStreamName() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getMetadataStreamName();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return null;
    }

    public static String getStationName() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getStationName();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return null;
    }
}
