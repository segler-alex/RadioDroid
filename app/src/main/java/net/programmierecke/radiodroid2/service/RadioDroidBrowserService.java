package net.programmierecke.radiodroid2.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.MediaBrowserServiceCompat;

import net.programmierecke.radiodroid2.IPlayerService;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.station.DataRadioStation;
import net.programmierecke.radiodroid2.utils.GetRealLinkAndPlayTask;

import java.util.List;

public class RadioDroidBrowserService extends MediaBrowserServiceCompat {
    private RadioDroidBrowser radioDroidBrowser;
    private ServiceConnection playerServiceConnection;
    private IPlayerService playerService;
    private GetRealLinkAndPlayTask playTask;


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
