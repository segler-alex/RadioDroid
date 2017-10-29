package net.programmierecke.radiodroid2.players;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import net.programmierecke.radiodroid2.R;

import java.io.IOException;

public class MediaPlayerWrapper implements PlayerWrapper {

    final private String TAG = "MediaPlayerWrapper";

    private MediaPlayer mediaPlayer;
    private PlayStateListener stateListener;

    @Override
    public void play(String proxyConnection, Context context, boolean isAlarm) {
        Log.v(TAG, "Stream url:" + proxyConnection);
        stateListener.onStateChanged(RadioPlayer.PlayState.ClearOld);

        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        try {
            stateListener.onStateChanged(RadioPlayer.PlayState.PrepareStream);
            mediaPlayer.setAudioStreamType(isAlarm ? AudioManager.STREAM_ALARM : AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(proxyConnection);
            mediaPlayer.prepare();
            stateListener.onStateChanged(RadioPlayer.PlayState.PrePlaying);
            mediaPlayer.start();
            stateListener.onStateChanged(RadioPlayer.PlayState.Playing);
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
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    @Override
    public int getAudioSessionId() {
        if (mediaPlayer != null) {
            return mediaPlayer.getAudioSessionId();
        }
        return 0;
    }

    @Override
    public void setVolume(float newVolume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(newVolume, newVolume);
        }
    }

    @Override
    public void setStateListener(PlayStateListener listener) {
        stateListener = listener;
    }
}
