package net.programmierecke.radiodroid2;

import java.util.Map;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.players.ExoPlayerWrapper;
import net.programmierecke.radiodroid2.players.MediaPlayerWrapper;
import net.programmierecke.radiodroid2.players.RadioPlayer;

public class PlayerService extends Service implements RadioPlayer.PlayerListener {
    protected static final int NOTIFY_ID = 1;

    public static final String PLAYER_SERVICE_TIMER_UPDATE = "net.programmierecke.radiodroid2.timerupdate";
    public static final String PLAYER_SERVICE_STATUS_UPDATE = "net.programmierecke.radiodroid2.statusupdate";
    public static final String PLAYER_SERVICE_META_UPDATE = "net.programmierecke.radiodroid2.metaupdate";

    private final String TAG = "PLAY";

    private final String ACTION_PAUSE = "pause";
    private final String ACTION_RESUME = "resume";
    private final String ACTION_STOP = "stop";

    private final float FULL_VOLUME = 100f;
    private final float DUCK_VOLUME = 40f;

    private Context itsContext;

    private String currentStationID;
    private String currentStationName;
    private String currentStationURL;

    private RadioPlayer radioPlayer;

    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private BecomingNoisyReceiver becomingNoisyReceiver = new BecomingNoisyReceiver();

    private CountDownTimer timer;
    private long seconds = 0;

    private Map<String, String> liveInfo;
    private ShoutcastInfo streamInfo;

    private boolean isHls = false;

    void sendBroadCast(String action) {
        Intent local = new Intent();
        local.setAction(action);
        sendBroadcast(local);
    }

    private final IPlayerService.Stub itsBinder = new IPlayerService.Stub() {

        public void Play(String theUrl, String theName, String theID, boolean isAlarm) throws RemoteException {
            PlayerService.this.PlayUrl(theUrl, theName, theID, isAlarm);
        }

        public void Pause() throws RemoteException {
            PlayerService.this.Pause();
        }

        public void Resume() throws RemoteException {
            PlayerService.this.Resume();
        }

        public void Stop() throws RemoteException {
            PlayerService.this.Stop();
        }

        @Override
        public void addTimer(int secondsAdd) throws RemoteException {
            PlayerService.this.addTimer(secondsAdd);
        }

        @Override
        public void clearTimer() throws RemoteException {
            PlayerService.this.clearTimer();
        }

        @Override
        public long getTimerSeconds() throws RemoteException {
            return PlayerService.this.getTimerSeconds();
        }

        @Override
        public String getCurrentStationID() throws RemoteException {
            return currentStationID;
        }

        @Override
        public String getStationName() throws RemoteException {
            return currentStationName;
        }

        @Override
        public Map getMetadataLive() throws RemoteException {
            return PlayerService.this.liveInfo;
        }

        @Override
        public String getMetadataStreamName() throws RemoteException {
            if (streamInfo != null)
                return streamInfo.audioName;
            return null;
        }

        @Override
        public String getMetadataServerName() throws RemoteException {
            if (streamInfo != null)
                return streamInfo.serverName;
            return null;
        }

        @Override
        public String getMetadataGenre() throws RemoteException {
            if (streamInfo != null)
                return streamInfo.audioGenre;
            return null;
        }

        @Override
        public String getMetadataHomepage() throws RemoteException {
            if (streamInfo != null)
                return streamInfo.audioHP;
            return null;
        }

        @Override
        public int getMetadataBitrate() throws RemoteException {
            if (streamInfo != null)
                return streamInfo.bitrate;
            return 0;
        }

        @Override
        public int getMetadataSampleRate() throws RemoteException {
            if (streamInfo != null)
                return streamInfo.sampleRate;
            return 0;
        }

        @Override
        public int getMetadataChannels() throws RemoteException {
            if (streamInfo != null)
                return streamInfo.channels;
            return 0;
        }

        @Override
        public boolean getIsHls() throws RemoteException {
            return isHls;
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            return radioPlayer.isPlaying();
        }

        @Override
        public void startRecording() throws RemoteException {
            if (radioPlayer != null) {
                radioPlayer.startRecording(currentStationName);
                sendBroadCast(PLAYER_SERVICE_META_UPDATE);
            }
        }

        @Override
        public void stopRecording() throws RemoteException {
            if (radioPlayer != null) {
                radioPlayer.stopRecording();
                sendBroadCast(PLAYER_SERVICE_META_UPDATE);
            }
        }

        @Override
        public boolean isRecording() throws RemoteException {
            return radioPlayer != null && radioPlayer.isRecording();
        }

        @Override
        public String getCurrentRecordFileName() throws RemoteException {
            if (radioPlayer != null) {
                return radioPlayer.getRecordFileName();
            }
            return null;
        }

        @Override
        public long getTransferredBytes() throws RemoteException {
            if (radioPlayer != null) {
                return radioPlayer.getRecordedBytes();
            }
            return 0;
        }
    };

    private final MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            final KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        PlayerServiceUtil.pause();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        PlayerServiceUtil.resume();
                        break;
                }
            }

            return true;
        }
    };

    AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if(BuildConfig.DEBUG) { Log.d(TAG, "audiofocus gain"); }

                            CreateMediaSession();
                            Resume();

                            radioPlayer.setVolume(FULL_VOLUME);
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            if(BuildConfig.DEBUG) { Log.d(TAG, "audiofocus loss"); }

                            Stop();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if(BuildConfig.DEBUG) { Log.d(TAG, "audiofocus loss transient"); }

                            Pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            if(BuildConfig.DEBUG) { Log.d(TAG, "audiofocus loss transient can duck"); }

                            radioPlayer.setVolume(DUCK_VOLUME);
                            break;
                    }
                }
            };

    private long getTimerSeconds() {
        return seconds;
    }

    private void clearTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;

            seconds = 0;

            sendBroadCast(PLAYER_SERVICE_TIMER_UPDATE);
        }
    }

    private void addTimer(int secondsAdd) {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        seconds += secondsAdd;

        timer = new CountDownTimer(seconds * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                seconds = millisUntilFinished / 1000;
                if(BuildConfig.DEBUG) { Log.d(TAG, "" + seconds); }

                Intent local = new Intent();
                local.setAction(PLAYER_SERVICE_TIMER_UPDATE);
                sendBroadcast(local);
            }

            public void onFinish() {
                Stop();
                timer = null;
            }
        }.start();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return itsBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        itsContext = this;
        timer = null;
        powerManager = (PowerManager) itsContext.getSystemService(Context.POWER_SERVICE);
        audioManager = (AudioManager) itsContext.getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            radioPlayer = new RadioPlayer(PlayerService.this, new ExoPlayerWrapper());
        } else {
            // use old MediaPlayer on API levels < 16
            // https://github.com/google/ExoPlayer/issues/711
            radioPlayer = new RadioPlayer(PlayerService.this, new MediaPlayerWrapper());
        }

        radioPlayer.setPlayerListener(this);
    }

    @Override
    public void onDestroy() {
        if(BuildConfig.DEBUG) { Log.d(TAG, "onDestroy()"); }
        Stop();

        radioPlayer.destroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_STOP:
                        Stop();
                        break;
                    case ACTION_PAUSE:
                        Pause();
                        break;
                    case ACTION_RESUME:
                        Resume();
                        break;
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void CreateMediaSession() {
        if (mediaSession == null) {
            becomingNoisyReceiver = new BecomingNoisyReceiver();

            IntentFilter becomingNoisyFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            registerReceiver(becomingNoisyReceiver, becomingNoisyFilter);

            mediaSession = new MediaSessionCompat(getBaseContext(), getBaseContext().getPackageName());
            mediaSession.setCallback(mediaSessionCallback);

            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
            mediaSession.setActive(true);

            SetMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
        }
    }

    private void DestroyMediaSession() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;

            unregisterReceiver(becomingNoisyReceiver);
        }
    }

    private void SetMediaPlaybackState(int state) {
        if (mediaSession == null) {
            return;
        }

        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else if (state == PlaybackStateCompat.STATE_PAUSED) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        } else {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_STOP);
        }

        playbackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mediaSession.setPlaybackState(playbackStateBuilder.build());
    }

    public void SendMessage(String theTitle, String theMessage, String theTicker) {
        Intent notificationIntent = new Intent(itsContext, ActivityPlayerInfo.class);
        notificationIntent.putExtra("stationid", currentStationID);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent stopIntent = new Intent(itsContext, PlayerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pendingIntentStop = PendingIntent.getService(itsContext, 0, stopIntent, 0);

        PendingIntent contentIntent = PendingIntent.getActivity(itsContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(itsContext)
                .setContentIntent(contentIntent)
                .setContentTitle(theTitle)
                .setContentText(theMessage)
                .setWhen(System.currentTimeMillis())
                .setTicker(theTicker)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_play_arrow_24dp)
                .setLargeIcon((((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher, null)).getBitmap()))
                .addAction(R.drawable.ic_stop_24dp, "Stop", pendingIntentStop);

        if (radioPlayer.getPlayState() == RadioPlayer.PlayState.Playing) {
            Intent pauseIntent = new Intent(itsContext, PlayerService.class);
            pauseIntent.setAction(ACTION_PAUSE);
            PendingIntent pendingIntentPause = PendingIntent.getService(itsContext, 0, pauseIntent, 0);

            notificationBuilder.addAction(R.drawable.ic_pause_24dp, "Pause", pendingIntentPause);
            notificationBuilder.setUsesChronometer(true);
        } else if (radioPlayer.getPlayState() == RadioPlayer.PlayState.Paused) {
            Intent resumeIntent = new Intent(itsContext, PlayerService.class);
            resumeIntent.setAction(ACTION_RESUME);
            PendingIntent pendingIntentResume = PendingIntent.getService(itsContext, 0, resumeIntent, 0);

            notificationBuilder.addAction(R.drawable.ic_play_arrow_24dp, "Resume", pendingIntentResume);
            notificationBuilder.setUsesChronometer(false);
        }

        Notification notification = notificationBuilder.build();

        startForeground(NOTIFY_ID, notification);
    }

    public void PlayUrl(String theURL, String theName, String theID, final boolean isAlarm) {
        currentStationID = theID;
        currentStationName = theName;
        currentStationURL = theURL;

        int result = audioManager.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Start playback.
            CreateMediaSession();
            ReplayCurrent(isAlarm);
        } else {
            ToastOnUi(R.string.error_grant_audiofocus);
        }
    }

    public void Pause() {
        if(BuildConfig.DEBUG) { Log.d(TAG, "pause()"); }

        radioPlayer.pause();
    }

    public void Resume() {
        if(BuildConfig.DEBUG) { Log.d(TAG, "resume()"); }

        if(!radioPlayer.isPlaying()) {
            ReplayCurrent(false);
        }
    }

    public void Stop() {
        if(BuildConfig.DEBUG) { Log.d(TAG, "stop()"); }

        DestroyMediaSession();

        audioManager.abandonAudioFocus(afChangeListener);

        radioPlayer.stop();

        liveInfo = null;
        streamInfo = null;
        clearTimer();
        stopForeground(true);
        sendBroadCast(PLAYER_SERVICE_STATUS_UPDATE);

        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                if(BuildConfig.DEBUG) { Log.d(TAG, "release wakelock"); }
            }
            wakeLock = null;
        }
        if (wifiLock != null) {
            if (wifiLock.isHeld()) {
                if(BuildConfig.DEBUG) { Log.d(TAG, "release wifilock"); }
                wifiLock.release();
            }
            wifiLock = null;
        }
    }

    public void ReplayCurrent(final boolean isAlarm) {
        liveInfo = null;
        streamInfo = null;

        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PlayerService");
        }
        if (!wakeLock.isHeld()) {
            if(BuildConfig.DEBUG) { Log.d(TAG, "acquire wakelock"); }
            wakeLock.acquire();
        }
        WifiManager wm = (WifiManager) itsContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {

            if (wifiLock == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PlayerService");
                } else {
                    wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "PlayerService");
                }
            }
            if (!wifiLock.isHeld()) {
                if(BuildConfig.DEBUG) { Log.d(TAG, "acquire wifilock"); }
                wifiLock.acquire();
            }
        } else {
            Log.e(TAG, "could not aquire wifi lock");
        }

        radioPlayer.play(currentStationURL, isAlarm);
    }

    void ToastOnUi(final int messageId) {
        Handler h = new Handler(itsContext.getMainLooper());

        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(itsContext, itsContext.getResources().getString(messageId), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void UpdateNotification() {
        switch (radioPlayer.getPlayState()) {
            case Idle:
                break;
            case CreateProxy:
                SendMessage(currentStationName, itsContext.getResources().getString(R.string.notify_start_proxy), itsContext.getResources().getString(R.string.notify_start_proxy));
                break;
            case ClearOld:
                SendMessage(currentStationName, itsContext.getResources().getString(R.string.notify_stop_player), itsContext.getResources().getString(R.string.notify_stop_player));
                break;
            case PrepareStream:
                SendMessage(currentStationName, itsContext.getResources().getString(R.string.notify_prepare_stream), itsContext.getResources().getString(R.string.notify_prepare_stream));
                break;
            case PrePlaying:
                SendMessage(currentStationName, itsContext.getResources().getString(R.string.notify_try_play), itsContext.getResources().getString(R.string.notify_try_play));
                break;
            case Playing:
                if (liveInfo != null) {
                    String title = liveInfo.get("StreamTitle");
                    if (!TextUtils.isEmpty(title)) {
                        if(BuildConfig.DEBUG) { Log.d(TAG, "update message:" + title); }
                        SendMessage(currentStationName, title, title);
                    } else {
                        SendMessage(currentStationName, itsContext.getResources().getString(R.string.notify_play), currentStationName);
                    }
                } else {
                    SendMessage(currentStationName, itsContext.getResources().getString(R.string.notify_play), currentStationName);
                }
                break;
            case Paused:
                SendMessage(currentStationName, itsContext.getResources().getString(R.string.notify_paused), currentStationName);
                break;
        }
    }

    @Override
    public void onStateChanged(final RadioPlayer.PlayState state) {
        // State changed can be called from the player's thread.

        Handler h = new Handler(itsContext.getMainLooper());

        h.post(new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case Paused:
                        SetMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                        break;
                    case Playing:
                        SetMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

                        CreateMediaSession();
                        mediaSession.setActive(true);
                        break;
                    default:
                        SetMediaPlaybackState(PlaybackStateCompat.STATE_NONE);

                        if (mediaSession != null) {
                            mediaSession.setActive(false);
                        }
                        break;
                }

                UpdateNotification();
            }
        });
    }

    @Override
    public void onPlayerError(int messageId) {
        ToastOnUi(messageId);
    }

    @Override
    public void foundShoutcastStream(ShoutcastInfo info, boolean isHls) {
        this.streamInfo = info;
        this.isHls = isHls;
        if (info != null) {
            if (info.audioName != null) {
                if (!info.audioName.trim().equals("")) {
                    currentStationName = info.audioName.trim();
                }
            }
            if(BuildConfig.DEBUG) {
                Log.d(TAG, "Metadata offset:" + info.metadataOffset);
                Log.d(TAG, "Bitrate:" + info.bitrate);
                Log.d(TAG, "Name:" + info.audioName);
                Log.d(TAG, "Hls:" + isHls);
                Log.d(TAG, "Server:" + info.serverName);
                Log.d(TAG, "AudioInfo:" + info.audioInfo);
            }
        }
        sendBroadCast(PLAYER_SERVICE_META_UPDATE);
    }

    @Override
    public void foundLiveStreamInfo(Map<String, String> liveInfo) {
        this.liveInfo = liveInfo;
        for (String key : liveInfo.keySet()) {
            if(BuildConfig.DEBUG) { Log.d(TAG, "INFO:" + key + "=" + liveInfo.get(key)); }
        }
        sendBroadCast(PLAYER_SERVICE_META_UPDATE);
        UpdateNotification();
    }
}
