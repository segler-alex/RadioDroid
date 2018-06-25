package net.programmierecke.radiodroid2.players;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.HttpClient;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.data.StreamLiveInfo;
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

    public enum PlayState {
        Idle,
        PrePlaying,
        Playing,
        Paused
    }

    public interface PlayerListener {
        void onStateChanged(final PlayState status, final int audioSessionId);

        void onPlayerError(final int messageId);

        void onBufferedTimeUpdate(final long bufferedMs);

        // We are not interested in this events here so they will be forwarded to whoever hold RadioPlayer
        void foundShoutcastStream(ShoutcastInfo bitrate, boolean isHls);

        void foundLiveStreamInfo(StreamLiveInfo liveInfo);
    }

    private PlayerWrapper player;
    private Context mainContext;

    private String streamName;

    private HandlerThread playerThread;
    private Handler playerThreadHandler;

    private PlayerListener playerListener;
    private PlayState playState = PlayState.Idle;

    private StreamLiveInfo lastLiveInfo;

    private CountDownTimer bufferCheckTimer = new CountDownTimer(Long.MAX_VALUE, 2000) {
        @Override
        public void onTick(long l) {
            final long bufferTimeMs = player.getBufferedMs();

            playerListener.onBufferedTimeUpdate(bufferTimeMs);

            if (BuildConfig.DEBUG) Log.d(TAG, String.format("buffered %d ms.", bufferTimeMs));
        }

        @Override
        public void onFinish() {

        }
    };

    public RadioPlayer(Context mainContext) {
        this.mainContext = mainContext;

        playerThread = new HandlerThread("PlayerThread");
        playerThread.start();

        playerThreadHandler = new Handler(playerThread.getLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            player = new ExoPlayerWrapper();
        } else {
            // use old MediaPlayer on API levels < 16
            // https://github.com/google/ExoPlayer/issues/711
            player = new MediaPlayerWrapper(playerThreadHandler);
        }

        player.setStateListener(this);
    }

    public final void play(final String stationURL, final String streamName, final boolean isAlarm) {
        setState(PlayState.PrePlaying, -1);

        this.streamName = streamName;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext.getApplicationContext());
        final int connectTimeout = prefs.getInt("stream_connect_timeout", 4);
        final int readTimeout = prefs.getInt("stream_read_timeout", 10);

        RadioDroidApp radioDroidApp = (RadioDroidApp) mainContext.getApplicationContext();

        final OkHttpClient customizedHttpClient = radioDroidApp.newHttpClient()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .build();

        playerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                player.playRemote(customizedHttpClient, stationURL, mainContext, isAlarm);
            }
        });
    }

    public final void pause() {
        playerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                final int audioSessionId = getAudioSessionId();
                player.pause();

                if (BuildConfig.DEBUG) {
                    bufferCheckTimer.cancel();
                }

                setState(PlayState.Paused, audioSessionId);
            }
        });
    }

    public final void stop() {
        if (playState == PlayState.Idle) {
            return;
        }

        playerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                final int audioSessionId = getAudioSessionId();

                player.stop();

                if (BuildConfig.DEBUG) {
                    bufferCheckTimer.cancel();
                }

                setState(PlayState.Idle, audioSessionId);
            }
        });
    }

    public final void destroy() {
        stop();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            playerThread.quitSafely();
        } else {
            Looper looper = playerThread.getLooper();
            if (looper != null) {
                playerThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        playerThread.quit();
                    }
                });
            }
        }
    }

    public final boolean isPlaying() {
        return player.isPlaying();
    }

    public final int getAudioSessionId() {
        return player.getAudioSessionId();
    }

    public final void setVolume(float volume) {
        player.setVolume(volume);
    }

    @Override
    public boolean canRecord() {
        return player.canRecord();
    }

    @Override
    public void startRecording(@NonNull RecordableListener recordableListener) {
        player.startRecording(recordableListener);
    }

    @Override
    public void stopRecording() {
        player.stopRecording();
    }

    @Override
    public boolean isRecording() {
        return player.isRecording();
    }

    @Override
    public Map<String, String> getNameFormattingArgs() {
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
        return player.getExtension();
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

        if (BuildConfig.DEBUG) {
            if (state == PlayState.Playing) {
                bufferCheckTimer.cancel();
                bufferCheckTimer.start();
            } else {
                bufferCheckTimer.cancel();
            }
        }

        playState = state;
        playerListener.onStateChanged(state, audioSessionId);
    }

    public long getTotalTransferredBytes() {
        return player.getTotalTransferredBytes();
    }

    public long getCurrentPlaybackTransferredBytes() {
        return player.getCurrentPlaybackTransferredBytes();
    }

    @Override
    public void onStateChanged(PlayState state) {
        setState(state, getAudioSessionId());
    }

    @Override
    public void onPlayerError(int messageId) {
        stop();
        playerListener.onPlayerError(messageId);
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
