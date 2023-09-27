package net.programmierecke.radiodroid2.players.exoplayer;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.icy.IcyHeaders;
import com.google.android.exoplayer2.metadata.icy.IcyInfo;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.players.PlayState;
import net.programmierecke.radiodroid2.players.PlayerWrapper;
import net.programmierecke.radiodroid2.recording.RecordableListener;
import net.programmierecke.radiodroid2.station.live.ShoutcastInfo;
import net.programmierecke.radiodroid2.station.live.StreamLiveInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

public class ExoPlayerWrapper implements PlayerWrapper, IcyDataSource.IcyDataSourceListener, Player.Listener {

    final private String TAG = "ExoPlayerWrapper";

    private ExoPlayer player;
    private PlayListener stateListener;

    private String streamUrl;

    private final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

    private RecordableListener recordableListener;

    private long totalTransferredBytes;
    private long currentPlaybackTransferredBytes;

    private boolean isHls;
    private boolean isPlayingFlag;

    private Handler playerThreadHandler;

    private Context context;
    private MediaSource audioSource;

    private Runnable fullStopTask;

    private final BroadcastReceiver networkChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (fullStopTask != null && player != null && audioSource != null && Utils.hasAnyConnection(context)) {
                Log.i(TAG, "Regained connection. Resuming playback.");

                cancelStopTask();

                player.setMediaSource(audioSource);
                player.prepare();
                player.setPlayWhenReady(true);
            }
        }
    };

    @Override
    public void playRemote(@NonNull OkHttpClient httpClient, @NonNull String streamUrl, @NonNull Context context, boolean isAlarm) {
        // I don't know why, but it is still possible that streamUrl is null,
        // I still get exceptions from this from google
        if (!streamUrl.equals(this.streamUrl)) {
            currentPlaybackTransferredBytes = 0;
        }

        this.context = context;
        this.streamUrl = streamUrl;

        cancelStopTask();

        stateListener.onStateChanged(PlayState.PrePlaying);

        if (player != null) {
            player.stop();
        }

        if (player == null) {
            player = new ExoPlayer.Builder(context).build();
            player.setAudioAttributes(new AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(isAlarm ? C.USAGE_ALARM : C.USAGE_MEDIA).build(), false);

            player.addListener(this);
            player.addAnalyticsListener(new AnalyticEventListener());
        }

        if (playerThreadHandler == null) {
            playerThreadHandler = new Handler(Looper.getMainLooper());
        }

        isHls = Utils.urlIndicatesHlsStream(streamUrl);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        final int retryTimeout = prefs.getInt("settings_retry_timeout", 10);
        final int retryDelay = prefs.getInt("settings_retry_delay", 100);

        DataSource.Factory dataSourceFactory = new RadioDataSourceFactory(httpClient, bandwidthMeter, this, retryTimeout, retryDelay);
        // Produces Extractor instances for parsing the media data.
        if (!isHls) {
            audioSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(new CustomLoadErrorHandlingPolicy())
                    .createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl)));
            player.setMediaSource(audioSource);
            player.prepare();
        } else {
            audioSource = new HlsMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(new CustomLoadErrorHandlingPolicy())
                    .createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl)));
            player.setMediaSource(audioSource);
            player.prepare();
        }

        player.setPlayWhenReady(true);

        context.registerReceiver(networkChangedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        // State changed will be called when audio session id is available.
    }

    @Override
    public void pause() {
        Log.i(TAG, "Pause. Stopping exoplayer.");

        cancelStopTask();

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

        cancelStopTask();

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
    public boolean isLocal() {
        return true;
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
    public void onMetadata(@NonNull Metadata metadata) {
        if (BuildConfig.DEBUG) Log.d(TAG, "META: " + metadata);
        final int length = metadata.length();
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                final Metadata.Entry entry = metadata.get(i);
                if (entry == null) {
                    continue;
                }
                if (entry instanceof IcyInfo) {
                    final IcyInfo icyInfo = ((IcyInfo) entry);
                    Log.d(TAG, "IcyInfo: " + icyInfo);
                    if (icyInfo.title != null) {
                        Map<String, String> rawMetadata = new HashMap<>() {{
                            put("StreamTitle", icyInfo.title);
                        }};
                        StreamLiveInfo streamLiveInfo = new StreamLiveInfo(rawMetadata);
                        onDataSourceStreamLiveInfo(streamLiveInfo);
                    }
                } else if (entry instanceof IcyHeaders) {
                    final IcyHeaders icyHeaders = ((IcyHeaders) entry);
                    Log.d(TAG, "IcyHeaders: " + icyHeaders);
                    onDataSourceShoutcastInfo(new ShoutcastInfo(icyHeaders));
                } else if (entry instanceof Id3Frame) {
                    final Id3Frame id3Frame = ((Id3Frame) entry);
                    Log.d(TAG, "id3 metadata: " + id3Frame);
                }
            }
        }
    }

    @Override
    public void onDataSourceConnectionLostIrrecoverably() {
        Log.i(TAG, "Connection lost irrecoverably.");
    }

    void resumeWhenNetworkConnected() {
        playerThreadHandler.post(() -> {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            int resumeWithin = sharedPref.getInt("settings_resume_within", 60);
            if (resumeWithin > 0) {
                Log.d(TAG, "Trying to resume playback within " + resumeWithin + "s.");

                // We want user to be able to paused during connection loss.
                // TODO: Find a way to notify user that even if current state is Playing
                //       we are actually trying to reconnect.
                //stateListener.onStateChanged(PlayState.Paused);

                cancelStopTask();

                fullStopTask = () -> {
                    stop();
                    stateListener.onPlayerError(R.string.giving_up_resume);

                    ExoPlayerWrapper.this.fullStopTask = null;
                };
                playerThreadHandler.postDelayed(fullStopTask, resumeWithin * 1000L);

                stateListener.onPlayerWarning(R.string.warning_no_network_trying_resume);
            } else {
                stop();

                stateListener.onPlayerError(R.string.error_stream_reconnect_timeout);
            }
        });
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
    public Map<String, String> getRecordNameFormattingArgs() {
        return null;
    }

    @Override
    public String getExtension() {
        return isHls ? "ts" : "mp3";
    }

    private void cancelStopTask() {
        if (fullStopTask != null) {
            playerThreadHandler.removeCallbacks(fullStopTask);
            fullStopTask = null;
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        // Do nothing
    }

    @Override
    public void onPlayerErrorChanged(PlaybackException error) {
        Log.d(TAG, "Player error: ", error);
        // Stop playing since it is either irrecoverable error in the player or our data source failed to reconnect.
        if (fullStopTask != null) {
            stop();
            stateListener.onPlayerError(R.string.error_play_stream);
        }
    }

    @Override
    public void onPlaybackParametersChanged(@NonNull PlaybackParameters playbackParameters) {
        // Do nothing
    }

    final class CustomLoadErrorHandlingPolicy extends DefaultLoadErrorHandlingPolicy {
        final int MIN_RETRY_DELAY_MS = 10;
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        // We need to read the retry delay here on each error again because the user might change
        // this value between retries and experiment with different vales to get the best result for
        // the specific situation. We also need to make sure that a sensible minimum value is chosen.
        int getSanitizedRetryDelaySettingsMs() {
            return Math.max(sharedPrefs.getInt("settings_retry_delay", 100), MIN_RETRY_DELAY_MS);
        }

        @Override
        public long getRetryDelayMsFor(LoadErrorInfo loadErrorInfo) {

            int retryDelay = getSanitizedRetryDelaySettingsMs();
            IOException exception = loadErrorInfo.exception;

            if (exception instanceof HttpDataSource.InvalidContentTypeException) {
                stateListener.onPlayerError(R.string.error_play_stream);
                return C.TIME_UNSET; // Immediately surface error if we cannot play content type
            }

            if (!Utils.hasAnyConnection(context)) {
                int resumeWithinS = sharedPrefs.getInt("settings_resume_within", 60);
                if (resumeWithinS > 0) {
                    resumeWhenNetworkConnected();
                    retryDelay = 1000 * resumeWithinS + retryDelay;
                }
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Providing retry delay of " + retryDelay + "ms " +
                        "error count: " + loadErrorInfo.errorCount + ", " +
                        "exception " + exception.getClass() + ", " +
                        "message: " + exception.getMessage());
            }
            return retryDelay;
        }

        @Override
        public int getMinimumLoadableRetryCount(int dataType) {
            return sharedPrefs.getInt("settings_retry_timeout", 10) * 1000 / getSanitizedRetryDelaySettingsMs() + 1;
        }
    }

    private class AnalyticEventListener implements AnalyticsListener {
        @Override
        public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {
            isPlayingFlag = playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING;

            switch (playbackState) {
                case Player.STATE_READY:
                    cancelStopTask();
                    stateListener.onStateChanged(PlayState.Playing);
                    break;
                case Player.STATE_BUFFERING:
                    stateListener.onStateChanged(PlayState.PrePlaying);
                    break;
            }

        }

        @Override
        public void onTimelineChanged(@NonNull EventTime eventTime, int reason) {

        }

        @Override
        public void onPlaybackParametersChanged(@NonNull EventTime eventTime, @NonNull PlaybackParameters playbackParameters) {

        }

        @Override
        public void onRepeatModeChanged(@NonNull EventTime eventTime, int repeatMode) {

        }

        @Override
        public void onShuffleModeChanged(@NonNull EventTime eventTime, boolean shuffleModeEnabled) {

        }

        @Override
        public void onBandwidthEstimate(@NonNull EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {

        }

        @Override
        public void onSurfaceSizeChanged(@NonNull EventTime eventTime, int width, int height) {

        }

        @Override
        public void onMetadata(@NonNull EventTime eventTime, @NonNull Metadata metadata) {

        }

        @Override
        public void onAudioAttributesChanged(@NonNull EventTime eventTime, @NonNull AudioAttributes audioAttributes) {

        }

        @Override
        public void onVolumeChanged(@NonNull EventTime eventTime, float volume) {

        }

        @Override
        public void onDroppedVideoFrames(@NonNull EventTime eventTime, int droppedFrames, long elapsedMs) {

        }

        @Override
        public void onDrmKeysLoaded(@NonNull EventTime eventTime) {

        }

        @Override
        public void onDrmSessionManagerError(@NonNull EventTime eventTime, @NonNull Exception error) {

        }

        @Override
        public void onDrmKeysRestored(@NonNull EventTime eventTime) {

        }

        @Override
        public void onDrmKeysRemoved(@NonNull EventTime eventTime) {

        }

        @Override
        public void onDrmSessionReleased(@NonNull EventTime eventTime) {

        }
    }
}
