package net.programmierecke.radiodroid2.players.selector;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.CastHandler;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.players.mpd.MPDClient;
import net.programmierecke.radiodroid2.players.mpd.MPDServerData;
import net.programmierecke.radiodroid2.players.mpd.tasks.MPDPlayTask;
import net.programmierecke.radiodroid2.station.DataRadioStation;

import java.lang.ref.WeakReference;

public class PlayStationTask extends AsyncTask<Void, Void, String> {
    private interface PlayFunc {
        void play(String url);
    }

    private PlayFunc playFunc;
    private DataRadioStation stationToPlay;
    private WeakReference<Context> contextWeakReference;

    private PlayStationTask(@NonNull DataRadioStation stationToPlay, @NonNull Context ctx, PlayFunc playFunc) {
        this.stationToPlay = stationToPlay;
        this.contextWeakReference = new WeakReference<>(ctx);
        this.playFunc = playFunc;
    }

    public static PlayStationTask playMPD(MPDClient mpdClient, MPDServerData mpdServerData, DataRadioStation stationToPlay, Context ctx) {
        return new PlayStationTask(stationToPlay, ctx, url -> mpdClient.enqueueTask(mpdServerData, new MPDPlayTask(url, null)));
    }

    public static PlayStationTask playExternal(DataRadioStation stationToPlay, Context ctx) {
        return new PlayStationTask(stationToPlay, ctx, url -> {
            Intent share = new Intent(Intent.ACTION_VIEW);
            share.setDataAndType(Uri.parse(url), "audio/*");
            ctx.startActivity(share);
        });
    }

    public static PlayStationTask playCAST(DataRadioStation stationToPlay, Context ctx) {
        return new PlayStationTask(stationToPlay, ctx, url -> CastHandler.PlayRemote(stationToPlay.Name, url, stationToPlay.IconUrl));
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Context ctx = contextWeakReference.get();
        if (ctx != null) {
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(ActivityMain.ACTION_SHOW_LOADING));
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        Context ctx = contextWeakReference.get();
        if (ctx != null) {
            RadioDroidApp radioDroidApp = (RadioDroidApp) ctx.getApplicationContext();
            return Utils.getRealStationLink(radioDroidApp.getHttpClient(), ctx.getApplicationContext(), stationToPlay.StationUuid);
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Context ctx = contextWeakReference.get();
        if (ctx == null) {
            return;
        }

        LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));

        if (result != null) {
            stationToPlay.playableUrl = result;
            playFunc.play(result);
        } else {
            Toast toast = Toast.makeText(ctx.getApplicationContext(),
                    ctx.getResources()
                            .getText(R.string.error_station_load), Toast.LENGTH_SHORT);
            toast.show();
        }
        super.onPostExecute(result);
    }
}