package net.programmierecke.radiodroid2.players;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.StreamProxy;
import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.interfaces.IStreamProxyEventReceiver;

import java.util.Map;

public class RadioPlayer implements IStreamProxyEventReceiver, PlayerWrapper.PlayStateListener {

    final private String TAG = "RadioPlayer";

    public enum PlayState {
        Idle,
        CreateProxy,
        ClearOld,
        PrepareStream,
        PrePlaying, Playing, Paused
    }

    public interface PlayerListener {
        void onStateChanged(final PlayState status, final int audioSessionId);

        void onPlayerError(final int messageId);

        // We are not interested in this events here so they will be forwarded to whoever hold RadioPlayer
        void foundShoutcastStream(ShoutcastInfo bitrate, boolean isHls);

        void foundLiveStreamInfo(Map<String, String> liveInfo);
    }

    private PlayerWrapper player;
    private StreamProxy proxy;
    private Context mainContext;

    private HandlerThread playerThread;
    private Handler playerThreadHandler;

    private PlayerListener playerListener;
    private PlayState playState = PlayState.Idle;

    private boolean isAlarm = false;
    private String stationUrl;

    public RadioPlayer(Context mainContext, PlayerWrapper playerWrapper) {
        this.mainContext = mainContext;
        this.player = playerWrapper;

        playerThread = new HandlerThread("PlayerThread");
        playerThread.start();

        playerThreadHandler = new Handler(playerThread.getLooper());

        player.setStateListener(this);
    }

    public final void play(final String stationURL, boolean isAlarm) {
        this.isAlarm = isAlarm;
        this.stationUrl = stationURL;

        setState(PlayState.Idle, getAudioSessionId());

        playerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (proxy != null) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "stopping old proxy.");
                    stopProxy();
                }

                setState(PlayState.CreateProxy, -1);
                proxy = new StreamProxy(stationURL, RadioPlayer.this);
            }
        });
    }

    public final void pause() {
        playerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                final int audioSessionId = getAudioSessionId();
                player.pause();

                if (proxy != null) {
                    stopProxy();

                    proxy = null;
                }

                setState(PlayState.Paused, audioSessionId);
            }
        });
    }

    public final void stop() {
        playerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                final int audioSessionId = getAudioSessionId();

                player.stop();

                if (proxy != null) {
                    stopProxy();

                    setState(PlayState.Idle, audioSessionId);
                }
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

    public final void startRecording(final String fileName, final String streamTitle) {
        if (proxy != null) {
            proxy.record(fileName, streamTitle);
        }
    }

    public final void stopRecording() {
        if (proxy != null) {
            proxy.stopRecord();
        }
    }

    public final boolean isRecording() {
        return proxy != null && proxy.getOutFileName() != null;
    }

    public final String getRecordFileName() {
        if (proxy != null) {
            return proxy.getOutFileName();
        }

        return "";
    }

    public final long getRecordedBytes() {
        if (proxy != null) {
            return proxy.getTotalBytes();
        }

        return 0;
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

        playState = state;
        playerListener.onStateChanged(state, audioSessionId);
    }

    private void stopProxy() {
        try {
            proxy.stop();
        } catch (Exception e) {
            Log.e(TAG, "proxy stop exception: ", e);
        }
        proxy = null;
    }

    @Override
    public void foundShoutcastStream(ShoutcastInfo bitrate, boolean isHls) {
        playerListener.foundShoutcastStream(bitrate, isHls);
    }

    @Override
    public void foundLiveStreamInfo(Map<String, String> liveInfo) {
        playerListener.foundLiveStreamInfo(liveInfo);
    }

    @Override
    public void streamCreated(final String proxyConnection) {
        playerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                player.play(proxyConnection, mainContext, isAlarm);
            }
        });
    }

    @Override
    public void streamStopped() {
        stop();
        play(stationUrl, isAlarm);
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
}
