package net.programmierecke.radiodroid2;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;

import net.programmierecke.radiodroid2.data.DataRadioStation;

import java.lang.ref.WeakReference;
import java.util.List;

import okhttp3.OkHttpClient;

public class RadioDroidBrowserService extends MediaBrowserServiceCompat {
    private RadioDroidBrowser radioDroidBrowser;
    private ServiceConnection playerServiceConnection;
    private IPlayerService playerService;
    private GetRealLinkAndPlayTask playTask;

    private static class GetRealLinkAndPlayTask extends AsyncTask<Void, Void, String> {
        private WeakReference<Context> contextRef;
        private DataRadioStation station;
        private WeakReference<IPlayerService> playerServiceRef;

        private OkHttpClient httpClient;

        public GetRealLinkAndPlayTask(Context context, DataRadioStation station, IPlayerService playerService) {
            this.contextRef = new WeakReference<>(context);
            this.station = station;
            this.playerServiceRef = new WeakReference<>(playerService);

            RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
            httpClient = radioDroidApp.getHttpClient();
        }

        @Override
        protected String doInBackground(Void... params) {
            Context context = contextRef.get();
            if (context != null) {
                return Utils.getRealStationLink(httpClient, context.getApplicationContext(), station.ID);
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            IPlayerService playerService = playerServiceRef.get();
            if (result != null && playerService != null && !isCancelled()) {
                try {
                    playerService.SaveInfo(result, station.Name, station.ID, station.IconUrl);
                    playerService.Play(false);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            super.onPostExecute(result);
        }
    }


    private final BroadcastReceiver playStationFromIdReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MediaSessionCallback.BROADCAST_PLAY_STATION_BY_ID.equals(action)) {
                String stationId = intent.getStringExtra(MediaSessionCallback.EXTRA_STATION_ID);

                DataRadioStation station = radioDroidBrowser.getStationById(stationId);

                if (station != null) {
                    if (playTask != null) {
                        playTask.cancel(false);
                    }

                    playTask = new GetRealLinkAndPlayTask(context, station, playerService);
                    playTask.execute();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        radioDroidBrowser = new RadioDroidBrowser((RadioDroidApp) getApplication());

        Intent anIntent = new Intent(this, PlayerService.class);
        startService(anIntent);

        playerServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                playerService = IPlayerService.Stub.asInterface(iBinder);
                try {
                    RadioDroidBrowserService.this.setSessionToken(playerService.getMediaSessionToken());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                playerService = null;
            }
        };

        bindService(anIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MediaSessionCallback.BROADCAST_PLAY_STATION_BY_ID);

        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.registerReceiver(playStationFromIdReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService(playerServiceConnection);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return radioDroidBrowser.onGetRoot(clientPackageName, clientUid, rootHints);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        radioDroidBrowser.onLoadChildren(parentId, result);
    }
}
