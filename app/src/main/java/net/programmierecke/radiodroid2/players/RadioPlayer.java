package net.programmierecke.radiodroid2.players;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.station.DataRadioStation;
import net.programmierecke.radiodroid2.station.live.ShoutcastInfo;
import net.programmierecke.radiodroid2.station.live.StreamLiveInfo;
import net.programmierecke.radiodroid2.players.exoplayer.ExoPlayerWrapper;
import net.programmierecke.radiodroid2.players.mediaplayer.MediaPlayerWrapper;
import net.programmierecke.radiodroid2.recording.Recordable;
import net.programmierecke.radiodroid2.recording.RecordableListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class RadioPlayer implements PlayerWrapper.PlayListener, Recordable {

    final private String TAG = "RadioPlayer";

    public interface PlayerListener {
        void onStateChanged(final PlayState status, final int audioSessionId);

        void onPlayerWarning(final int messageId);

        void onPlayerError(final int messageId);

        void onBufferedTimeUpdate(final long bufferedMs);

        // We are not interested in this events here so they will be forwarded to whoever hold RadioPlayer
        void foundShoutcastStream(ShoutcastInfo bitrate, boolean isHls);

        void foundLiveStreamInfo(StreamLiveInfo liveInfo);
    }

    private PlayerWrapper currentPlayer;
    private Context mainContext;

    private String streamName;

    private HandlerThread playerThread;
    private Handler playerThreadHandler;

    private PlayerListener playerListener;
    private PlayState playState = PlayState.Idle;

    private StreamLiveInfo lastLiveInfo;

    private PlayStationTask playStationTask;

    private Runnable bufferCheckRunnable = new Runnable() {
        @Override
        public void run() {
            final long bufferTimeMs = currentPlayer.getBufferedMs();

            playerListener.onBufferedTimeUpdate(bufferTimeMs);

            if (BuildConfig.DEBUG) Log.d(TAG, String.format("buffered %d ms.", bufferTimeMs));

            playerThreadHandler.postDelayed(this, 2000);
        }
    };

    public RadioPlayer(Context mainContext) {
        this.mainContext = mainContext;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // ExoPlayer has its own thread for cpu intensive tasks
            playerThreadHandler = new Handler(Looper.getMainLooper());
            currentPlayer = new ExoPlayerWrapper();
        } else {
            playerThread = new HandlerThread("MediaPlayerThread");
            playerThread.start();

            // MediaPlayer requires to be run in non-ui thread.
            playerThreadHandler = new Handler(playerThread.getLooper());
            // use old MediaPlayer on API levels < 16
            // https://github.com/google/ExoPlayer/issues/711
            currentPlayer = new MediaPlayerWrapper(playerThreadHandler);
        }

        currentPlayer.setStateListener(this);
    }

    public final void play(final String stationURL, final String streamName, final boolean isAlarm) {
        setState(PlayState.PrePlaying, -1);

        this.streamName = streamName;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext.getApplicationContext());
        final int connectTimeout = prefs.getInt("stream_connect_timeout", 4);
        final int readTimeout = prefs.getInt("stream_read_timeout", 10);

        RadioDroidApp radioDroidApp = (RadioDroidApp) mainContext.getApplicationContext();

        // TODO: Should we not pass http client if currentPlayer is external?

        final OkHttpClient customizedHttpClient = radioDroidApp.newHttpClient()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .build();

        playerThreadHandler.post(() -> currentPlayer.playRemote(customizedHttpClient, stationURL, mainContext, isAlarm));
    }

    public final void play(final DataRadioStation station, final boolean isAlarm) {
        setState(PlayState.PrePlaying, -1);

        playStationTask = new PlayStationTask(station, mainContext,
                (url) -> RadioPlayer.this.play(station.playableUrl, station.Name, isAlarm),
                (executionResult) -> {
                    RadioPlayer.this.playStationTask = null;

                    if (executionResult == PlayStationTask.ExecutionResult.FAILURE) {
                        RadioPlayer.this.onPlayerError(R.string.error_station_load);
                    }
                });

        playStationTask.execute();
    }

    private void cancelStationLinkRetrieval() {
        if (playStationTask != null) {
            playStationTask.cancel(true);
            playStationTask = null;
        }
    }

    public final void pause() {
        cancelStationLinkRetrieval();

        playerThreadHandler.post(() -> {
            if (playState == PlayState.Idle || playState == PlayState.Paused) {
                return;
            }

            final int audioSessionId = getAudioSessionId();
            currentPlayer.pause();

            if (BuildConfig.DEBUG) {
                playerThreadHandler.removeCallbacks(bufferCheckRunnable);
            }

            setState(PlayState.Paused, audioSessionId);
        });
    }

    public final void stop() {
        if (playState == PlayState.Idle) {
            return;
        }

        cancelStationLinkRetrieval();

        playerThreadHandler.post(() -> {
            final int audioSessionId = getAudioSessionId();

            currentPlayer.stop();

            if (BuildConfig.DEBUG) {
                playerThreadHandler.removeCallbacks(bufferCheckRunnable);
            }

            setState(PlayState.Idle, audioSessionId);
        });
    }

    public final void destroy() {
        stop();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Looper looper = playerThread.getLooper();
            if (looper != null) {
                playerThreadHandler.post(() -> playerThread.quit());
            }
        }
    }

    public final boolean isPlaying() {
        // From user perspective PlayState.PrePlaying is playing and otherwise will lead to
        // inconsistencies in UI.
        return playState == PlayState.PrePlaying || playState == PlayState.Playing;
    }

    public final int getAudioSessionId() {
        return currentPlayer.getAudioSessionId();
    }

    public final void setVolume(float volume) {
        currentPlayer.setVolume(volume);
    }

    @Override
    public boolean canRecord() {
        return currentPlayer.canRecord();
    }

    @Override
    public void startRecording(@NonNull RecordableListener recordableListener) {
        currentPlayer.startRecording(recordableListener);
    }

    @Override
    public void stopRecording() {
        currentPlayer.stopRecording();
    }

    @Override
    public boolean isRecording() {
        return currentPlayer.isRecording();
    }

    @Override
    public Map<String, String> getRecordNameFormattingArgs() {
        Map<String, String> args = new HashMap<>();
        args.put("station", Utils.sanitizeName(streamName));

        if (lastLiveInfo != null) {
            args.put("artist", Utils.sanitizeName(lastLiveInfo.getArtist()));
            args.put("track", Utils.sanitizeName(lastLiveInfo.getTrack()));
        } else {
            args.put("artist", "-");
            args.put("track", "-");
        }

        return args;
    }

    @Override
    public String getExtension() {
        return currentPlayer.getExtension();
    }

    public final void runInPlayerThread(Runnable runnable) {
        playerThreadHandler.post(runnable);
    }

    public final void setPlayerListener(PlayerListener listener) {
        playerListener = listener;
    }

    public PlayState getPlayState() {
        return playState;
    }

    private void setState(PlayState state, int audioSessionId) {
        if (BuildConfig.DEBUG) Log.d(TAG, String.format("set state '%s'", state.name()));

        if (playState == state) {
            return;
        }

        if (BuildConfig.DEBUG) {
            if (state == PlayState.Playing) {
                playerThreadHandler.removeCallbacks(bufferCheckRunnable);
                playerThreadHandler.post(bufferCheckRunnable);
            } else {
                playerThreadHandler.removeCallbacks(bufferCheckRunnable);
            }
        }

        playState = state;
        playerListener.onStateChanged(state, audioSessionId);
    }

    public long getTotalTransferredBytes() {
        return currentPlayer.getTotalTransferredBytes();
    }

    public long getCurrentPlaybackTransferredBytes() {
        return currentPlayer.getCurrentPlaybackTransferredBytes();
    }

    public long getBufferedSeconds() {
        return currentPlayer.getBufferedMs() / 1000;
    }

    public boolean isLocal() {
        return currentPlayer.isLocal();
    }

    @Override
    public void onStateChanged(PlayState state) {
        setState(state, getAudioSessionId());
    }

    @Override
    public void onPlayerWarning(int messageId) {
        playerThreadHandler.post(() -> playerListener.onPlayerWarning(messageId));
    }

    @Override
    public void onPlayerError(int messageId) {
        pause();
        playerThreadHandler.post(() -> playerListener.onPlayerError(messageId));
    }

    @Override
    public void onDataSourceShoutcastInfo(ShoutcastInfo shoutcastInfo, boolean isHls) {
        playerListener.foundShoutcastStream(shoutcastInfo, isHls);
    }

    @Override
    public void onDataSourceStreamLiveInfo(StreamLiveInfo liveInfo) {
        lastLiveInfo = liveInfo;
        playerListener.foundLiveStreamInfo(liveInfo);
    }
}
