package net.programmierecke.radiodroid2.players.mediaplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.players.PlayState;
import net.programmierecke.radiodroid2.station.live.ShoutcastInfo;
import net.programmierecke.radiodroid2.station.live.StreamLiveInfo;
import net.programmierecke.radiodroid2.players.PlayerWrapper;
import net.programmierecke.radiodroid2.recording.RecordableListener;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;

public class MediaPlayerWrapper implements PlayerWrapper, StreamProxyListener {

    final private String TAG = "MediaPlayerWrapper";

    private Handler playerThreadHandler;

    private MediaPlayer mediaPlayer;
    private StreamProxy proxy;
    private PlayListener stateListener;

    private String streamUrl;
    private Context context;
    private boolean isAlarm;

    private boolean isHls;

    private long totalTransferredBytes;
    private long currentPlaybackTransferredBytes;

    private AtomicBoolean playerIsInLegalState = new AtomicBoolean(false);

    public MediaPlayerWrapper(Handler playerThreadHandler) {
        this.playerThreadHandler = playerThreadHandler;
    }

    @Override
    public void playRemote(@NonNull OkHttpClient httpClient, @NonNull String streamUrl, @NonNull Context context, boolean isAlarm) {
        if (!streamUrl.equals(this.streamUrl)) {
            currentPlaybackTransferredBytes = 0;
        }

        this.streamUrl = streamUrl;
        this.context = context;
        this.isAlarm = isAlarm;

        Log.v(TAG, "Stream url:" + streamUrl);

        isHls = Utils.urlIndicatesHlsStream(streamUrl);

        if (!isHls) {
            if (proxy != null) {
                if (BuildConfig.DEBUG) Log.d(TAG, "stopping old proxy.");
                stopProxy();
            }

            proxy = new StreamProxy(httpClient, streamUrl, MediaPlayerWrapper.this);
        } else {
            stopProxy();
            onStreamCreated(streamUrl);
        }
    }

    private void playProxyStream(String proxyUrl, Context context, boolean isAlarm) {
        playerIsInLegalState.set(false);

        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        mediaPlayer.reset();

        try {
            mediaPlayer.setAudioStreamType(isAlarm ? AudioManager.STREAM_ALARM : AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(proxyUrl);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    playerIsInLegalState.set(true);

                    stateListener.onStateChanged(PlayState.PrePlaying);
                    mediaPlayer.start();
                    stateListener.onStateChanged(PlayState.Playing);
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stateListener.onPlayerError(R.string.error_play_stream);
                return true;
            });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "" + e);
            stateListener.onPlayerError(R.string.error_stream_url);
        } catch (IOException e) {
            Log.e(TAG, "" + e);
            stateListener.onPlayerError(R.string.error_caching_stream);
        } catch (Exception e) {
            Log.e(TAG, "" + e);
            stateListener.onPlayerError(R.string.error_play_stream);
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();

                stateListener.onStateChanged(PlayState.Paused);
            } else {
                stop();
            }
        }

        stopProxy();
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            playerIsInLegalState.set(false);

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }

            mediaPlayer.release();
            mediaPlayer = null;

            playerIsInLegalState.set(true);
        }

        stateListener.onStateChanged(PlayState.Idle);

        stopProxy();
    }

    @Override
    public boolean isPlaying() {
        if (mediaPlayer == null) {
            return false;
        }

        // If player is in illegal state it is either starting playback or stopping it so we treat
        // it as playing state.
        return !playerIsInLegalState.get() || (mediaPlayer != null && mediaPlayer.isPlaying());
    }

    @Override
    public long getBufferedMs() {
        return -1;
    }

    @Override
    public int getAudioSessionId() {
        if (mediaPlayer != null) {
            return mediaPlayer.getAudioSessionId();
        }
        return 0;
    }

    @Override
    public long getTotalTransferredBytes() {
        return totalTransferredBytes;
    }

    @Override
    public long getCurrentPlaybackTransferredBytes() {
        return currentPlaybackTransferredBytes;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public void setVolume(float newVolume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(newVolume, newVolume);
        }
    }

    @Override
    public void setStateListener(PlayListener listener) {
        stateListener = listener;
    }

    @Override
    public boolean canRecord() {
        return mediaPlayer != null && !isHls;
    }

    @Override
    public void startRecording(@NonNull RecordableListener recordableListener) {
        if (proxy != null) {
            proxy.startRecording(recordableListener);
        }
    }

    @Override
    public void stopRecording() {
        proxy.stopRecording();
    }

    @Override
    public boolean isRecording() {
        return proxy != null && proxy.isRecording();
    }

    @Override
    public Map<String, String> getRecordNameFormattingArgs() {
        return null;
    }

    @Override
    public String getExtension() {
        return proxy.getExtension();
    }

    @Override
    public void onFoundShoutcastStream(ShoutcastInfo shoutcastInfo, boolean isHls) {
        stateListener.onDataSourceShoutcastInfo(shoutcastInfo, isHls);
    }

    @Override
    public void onFoundLiveStreamInfo(StreamLiveInfo liveInfo) {
        stateListener.onDataSourceStreamLiveInfo(liveInfo);
    }

    @Override
    public void onStreamCreated(final String proxyConnection) {
        playerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                playProxyStream(proxyConnection, context, isAlarm);
            }
        });
    }

    @Override
    public void onStreamStopped() {
        stop();
    }

    @Override
    public void onBytesRead(byte[] buffer, int offset, int length) {
        totalTransferredBytes += length;
        currentPlaybackTransferredBytes += length;
    }

    private void stopProxy() {
        if (proxy != null) {
            try {
                proxy.stop();
            } catch (Exception e) {
                Log.e(TAG, "proxy stop exception: ", e);
            }
            proxy = null;
        }
    }
}
