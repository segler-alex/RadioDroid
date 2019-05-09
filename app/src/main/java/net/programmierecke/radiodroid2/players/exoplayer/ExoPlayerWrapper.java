package net.programmierecke.radiodroid2.players.exoplayer;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.recording.RecordableListener;
import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.data.StreamLiveInfo;
import net.programmierecke.radiodroid2.players.PlayerWrapper;
import net.programmierecke.radiodroid2.players.RadioPlayer;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;

public class ExoPlayerWrapper implements PlayerWrapper, IcyDataSource.IcyDataSourceListener {

    final private String TAG = "ExoPlayerWrapper";

    private SimpleExoPlayer player;
    private PlayListener stateListener;

    private String streamUrl;

    private DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

    private RecordableListener recordableListener;

    private long totalTransferredBytes;
    private long currentPlaybackTransferredBytes;

    private boolean isHls;
    private boolean isPlayingFlag;

    Context context;
    MediaSource audioSource;

    boolean interruptedByConnectionLoss = false;

    private final BroadcastReceiver networkChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (interruptedByConnectionLoss && player != null && audioSource != null
                    && Utils.hasAnyConnection(context)) {
                interruptedByConnectionLoss = false;
                Log.i(TAG, "Regained connection. Resuming playback.");
                player.prepare(audioSource);
                player.setPlayWhenReady(true);
            }
        }
    };


    @Override
    public void playRemote(@NonNull OkHttpClient httpClient, @NonNull String streamUrl, @NonNull Context context, boolean isAlarm) {
        // I don't know why, but it is still possible that streamUrl is null,
        // I still get exceptions from this from google
        if (streamUrl == null){
            return;
        }
        if (!streamUrl.equals(this.streamUrl)) {
            currentPlaybackTransferredBytes = 0;
        }

        this.context = context;
        this.streamUrl = streamUrl;

        stateListener.onStateChanged(RadioPlayer.PlayState.PrePlaying);

        if (player != null) {
            player.stop();
        }

        if (player == null) {
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
            TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

            LoadControl loadControl = new DefaultLoadControl.Builder().createDefaultLoadControl();
            player = ExoPlayerFactory.newSimpleInstance(context, new DefaultRenderersFactory(context), trackSelector, loadControl);
            player.setAudioAttributes(new AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_MUSIC)
                    .setUsage(isAlarm ? C.USAGE_ALARM : C.USAGE_MEDIA).build());

            player.addListener(new ExoPlayerListener());
            player.addAnalyticsListener(new AnalyticEventListener());
        }

        isHls = streamUrl.endsWith(".m3u8");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        final int retryTimeout = prefs.getInt("settings_retry_timeout", 4);
        final int retryDelay = prefs.getInt("settings_retry_delay", 10);

        DataSource.Factory dataSourceFactory = new RadioDataSourceFactory(httpClient, bandwidthMeter, this, retryTimeout, retryDelay);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        if (!isHls) {
            audioSource = new ExtractorMediaSource.Factory(dataSourceFactory).setExtractorsFactory(extractorsFactory)
                    .createMediaSource(Uri.parse(streamUrl));
            player.prepare(audioSource);
        } else {
            audioSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(streamUrl));
            player.prepare(audioSource);
        }

        player.setPlayWhenReady(true);

        interruptedByConnectionLoss = false;
        context.registerReceiver(networkChangedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        // State changed will be called when audio session id is available.
    }

    @Override
    public void pause() {
        Log.i(TAG, "Pause. Stopping exoplayer.");

        if (player != null) {
            context.unregisterReceiver(networkChangedReceiver);
            player.stop();
            player.release();
            player = null;
        }
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping exoplayer.");

        if (player != null) {
            context.unregisterReceiver(networkChangedReceiver);
            player.stop();
            player.release();
            player = null;
        }

        stopRecording();
    }

    @Override
    public boolean isPlaying() {
        return player != null && isPlayingFlag;
    }

    @Override
    public long getBufferedMs() {
        if (player != null) {
            return (int) (player.getBufferedPosition() - player.getCurrentPosition());
        }

        return 0;
    }

    @Override
    public int getAudioSessionId() {
        if (player != null) {
            return player.getAudioSessionId();
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
    public void setVolume(float newVolume) {
        if (player != null) {
            player.setVolume(newVolume);
        }
    }

    @Override
    public void setStateListener(PlayListener listener) {
        stateListener = listener;
    }

    @Override
    public void onDataSourceConnected() {

    }

    @Override
    public void onDataSourceConnectionLost() {

    }

    @Override
    public void onDataSourceConnectionLostIrrecoverably() {
        Log.i(TAG, "Connection lost irrecoverably.");

        stateListener.onStateChanged(RadioPlayer.PlayState.Idle);
        stateListener.onPlayerError(R.string.error_stream_reconnect_timeout);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int resumeWithin = sharedPref.getInt("settings_resume_within", 60);
        if(resumeWithin > 0) {
            Log.d(TAG, "Trying to resume playback within " + resumeWithin + "s.");
            player.setPlayWhenReady(false);
            interruptedByConnectionLoss = true;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (interruptedByConnectionLoss) {
                        interruptedByConnectionLoss = false;
                        stop();
                        stateListener.onPlayerError(R.string.giving_up_resume);
                    }
                }
            }, resumeWithin * 1000);
        } else {
            stop();
        }
    }

    @Override
    public void onDataSourceShoutcastInfo(@Nullable ShoutcastInfo shoutcastInfo) {
        stateListener.onDataSourceShoutcastInfo(shoutcastInfo, false);
    }

    @Override
    public void onDataSourceStreamLiveInfo(StreamLiveInfo streamLiveInfo) {
        stateListener.onDataSourceStreamLiveInfo(streamLiveInfo);
    }

    @Override
    public void onDataSourceBytesRead(byte[] buffer, int offset, int length) {
        totalTransferredBytes += length;
        currentPlaybackTransferredBytes += length;

        if (recordableListener != null) {
            recordableListener.onBytesAvailable(buffer, offset, length);
        }
    }

    @Override
    public boolean canRecord() {
        return player != null;
    }

    @Override
    public void startRecording(@NonNull RecordableListener recordableListener) {
        this.recordableListener = recordableListener;
    }

    @Override
    public void stopRecording() {
        if (recordableListener != null) {
            recordableListener.onRecordingEnded();
            recordableListener = null;
        }
    }

    @Override
    public boolean isRecording() {
        return recordableListener != null;
    }

    @Override
    public Map<String, String> getNameFormattingArgs() {
        return null;
    }

    @Override
    public String getExtension() {
        return isHls ? "ts" : "mp3";
    }

    private class ExoPlayerListener implements Player.EventListener {

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            // Do nothing
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            // Do nothing
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            // Do nothing
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            // Do nothing
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            // Stop playing since it is either irrecoverable error in the player or our data source failed to reconnect.

            if(!interruptedByConnectionLoss) {
                stop();
                stateListener.onStateChanged(RadioPlayer.PlayState.Idle);
                stateListener.onPlayerError(R.string.error_play_stream);
            }
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            // Do nothing
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            // Do nothing
        }
    }

    private class AnalyticEventListener implements AnalyticsListener {
        @Override
        public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {
            isPlayingFlag = playWhenReady;
        }

        @Override
        public void onTimelineChanged(EventTime eventTime, int reason) {

        }

        @Override
        public void onPositionDiscontinuity(EventTime eventTime, int reason) {

        }

        @Override
        public void onSeekStarted(EventTime eventTime) {

        }

        @Override
        public void onSeekProcessed(EventTime eventTime) {

        }

        @Override
        public void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) {

        }

        @Override
        public void onRepeatModeChanged(EventTime eventTime, int repeatMode) {

        }

        @Override
        public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {

        }

        @Override
        public void onLoadingChanged(EventTime eventTime, boolean isLoading) {

        }

        @Override
        public void onPlayerError(EventTime eventTime, ExoPlaybackException error) {

        }

        @Override
        public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadStarted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {

        }

        @Override
        public void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {

        }

        @Override
        public void onLoadCanceled(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {

        }

        @Override
        public void onLoadError(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {

        }

        @Override
        public void onDownstreamFormatChanged(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {

        }

        @Override
        public void onUpstreamDiscarded(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {

        }

        @Override
        public void onMediaPeriodCreated(EventTime eventTime) {

        }

        @Override
        public void onMediaPeriodReleased(EventTime eventTime) {

        }

        @Override
        public void onReadingStarted(EventTime eventTime) {

        }

        @Override
        public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {

        }

        @Override
        public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {

        }

        @Override
        public void onMetadata(EventTime eventTime, Metadata metadata) {

        }

        @Override
        public void onDecoderEnabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {

        }

        @Override
        public void onDecoderInitialized(EventTime eventTime, int trackType, String decoderName, long initializationDurationMs) {

        }

        @Override
        public void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) {

        }

        @Override
        public void onDecoderDisabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {

        }

        @Override
        public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
            stateListener.onStateChanged(RadioPlayer.PlayState.Playing);
        }

        @Override
        public void onAudioAttributesChanged(EventTime eventTime, AudioAttributes audioAttributes) {

        }

        @Override
        public void onVolumeChanged(EventTime eventTime, float volume) {

        }

        @Override
        public void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

        }

        @Override
        public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {

        }

        @Override
        public void onVideoSizeChanged(EventTime eventTime, int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

        }

        @Override
        public void onRenderedFirstFrame(EventTime eventTime, @Nullable Surface surface) {

        }

        @Override
        public void onDrmSessionAcquired(EventTime eventTime) {

        }

        @Override
        public void onDrmKeysLoaded(EventTime eventTime) {

        }

        @Override
        public void onDrmSessionManagerError(EventTime eventTime, Exception error) {

        }

        @Override
        public void onDrmKeysRestored(EventTime eventTime) {

        }

        @Override
        public void onDrmKeysRemoved(EventTime eventTime) {

        }

        @Override
        public void onDrmSessionReleased(EventTime eventTime) {

        }
    }
}
