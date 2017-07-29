package net.programmierecke.radiodroid2.players;


import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class ExoPlayerWrapper implements PlayerWrapper {

    final private String TAG = "ExoPlayerWrapper";

    private SimpleExoPlayer player;
    private PlayStateListener stateListener;

    @Override
    public void play(String proxyConnection, Context context, boolean isAlarm) {
        Log.v(TAG, "Stream url:" + proxyConnection);
        stateListener.onStateChanged(RadioPlayer.PlayState.ClearOld);

        if (player != null) {
            player.stop();
        }

        if (player == null) {
            // 1. Create a default TrackSelector
            //Handler mainHandler = new Handler();
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
            TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

            // 2. Create a default LoadControl
            LoadControl loadControl = new DefaultLoadControl();

            // 3. Create the player
            player = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);
            player.setAudioStreamType(isAlarm ? AudioManager.STREAM_ALARM : AudioManager.STREAM_MUSIC);
        }
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "yourApplicationName"), null);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        MediaSource videoSource = null;
        videoSource = new ExtractorMediaSource(Uri.parse(proxyConnection), dataSourceFactory, extractorsFactory, null, null);
        player.prepare(videoSource);
        player.setPlayWhenReady(true);

        stateListener.onStateChanged(RadioPlayer.PlayState.Playing);
    }

    @Override
    public void pause() {
        Log.i(TAG, "Pause. Stopping exoplayer.");

        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping exoplayer.");

        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    @Override
    public boolean isPlaying() {
        return player != null && player.getPlayWhenReady();

    }

    @Override
    public void setVolume(float newVolume) {
        if (player != null) {
            player.setVolume(newVolume);
        }
    }

    @Override
    public void setStateListener(PlayStateListener listener) {
        stateListener = listener;
    }
}
