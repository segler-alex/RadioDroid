package net.programmierecke.radiodroid2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import android.util.TypedValue;
import android.widget.ImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import net.programmierecke.radiodroid2.data.StreamLiveInfo;

public class PlayerServiceUtil {

    private static Context mainContext = null;
    private Drawable stationIcon;

    public static void bind(Context context){
        Intent anIntent = new Intent(context, PlayerService.class);
        mainContext = context;
        context.bindService(anIntent, svcConn, Context.BIND_AUTO_CREATE);
        context.startService(anIntent);
    }

    public static void unBind(Context context){
        PlayerService thisService = new PlayerService();
        try {
            context.unbindService(svcConn);
        } catch (Exception e) {
        }
    }

    public static void shutdownService() {
        if (mainContext != null) {
            try {
                Intent anIntent = new Intent(mainContext, PlayerService.class);
                // context.bindService(anIntent, svcConn, Context.BIND_AUTO_CREATE);
                if(BuildConfig.DEBUG) { Log.d("PlayerServiceUtil", "PlayerServiceUtil: shutdownService"); }
                mainContext.stopService(anIntent);
            } catch(Exception e) {
                if(BuildConfig.DEBUG) { Log.d("PlayerServiceUtil", "PlayerServiceUtil: shutdownService E001:" + e.getMessage()); }
            }
        }
    }

    static IPlayerService itsPlayerService;
    private static ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            if(BuildConfig.DEBUG) { Log.d("PLAYER", "Service came online"); }
            itsPlayerService = IPlayerService.Stub.asInterface(binder);
        }

        public void onServiceDisconnected(ComponentName className) {
            if(BuildConfig.DEBUG) { Log.d("PLAYER", "Service offline"); }
            itsPlayerService = null;
        }
    };

    public static boolean isPlaying(){
        if (itsPlayerService != null){
            try {
                return itsPlayerService.isPlaying();
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

    public static void saveInfo(String result, String name, String id, String iconUrl) {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.SaveInfo(result,name,id,iconUrl);
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
    }

    public static void play(String result, String name, String id, String iconUrl) {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.SaveInfo(result,name,id,iconUrl);
                itsPlayerService.Play(false);
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
    }

    public static void pause() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.Pause();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
    }

    public static void resume() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.Resume();
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

    public static @NonNull StreamLiveInfo getMetadataLive() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getMetadataLive();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return new StreamLiveInfo(null);
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

    public static String getStationId() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getCurrentStationID();
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

    public static String getStationIconUrl() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getStationIconUrl();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return null;
    }

    public static void getStationIcon(final ImageView holder, String fromUrl) {
        final String iconUrl = fromUrl != null? fromUrl : getStationIconUrl();
        if (iconUrl != null) {
            Resources r = mainContext.getResources();
            final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, r.getDisplayMetrics());

            Callback imageLoadCallback = new Callback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onError() {
                    Picasso.with(mainContext)
                            .load(iconUrl)
                            .placeholder(ContextCompat.getDrawable(mainContext, R.drawable.ic_photo_black_24dp))
                            .resize((int) px, 0)
                            .networkPolicy(NetworkPolicy.NO_CACHE)
                            .into(holder);
                }
            };

            Picasso.with(mainContext)
                    .load(iconUrl)
                    .placeholder(ContextCompat.getDrawable(mainContext, R.drawable.ic_photo_black_24dp))
                    .resize((int) px, 0)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(holder, imageLoadCallback);
        }
    }

    public static int getMetadataBitrate() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getMetadataBitrate();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return 0;
    }

    public static String getMetadataHomepage() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getMetadataHomepage();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return null;
    }

    public static String getMetadataGenre() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getMetadataGenre();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return null;
    }

    public static void startRecording() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.startRecording();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
    }

    public static void stopRecording() {
        if (itsPlayerService != null) {
            try {
                itsPlayerService.stopRecording();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
    }

    public static boolean isRecording() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.isRecording();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return false;
    }

    public static String getCurrentRecordFileName() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getCurrentRecordFileName();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return null;
    }

    public static boolean getIsHls() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getIsHls();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return false;
    }

    public static long getTransferredBytes() {
        if (itsPlayerService != null) {
            try {
                return itsPlayerService.getTransferredBytes();
            } catch (RemoteException e) {
                Log.e("", "" + e);
            }
        }
        return 0;
    }
}
