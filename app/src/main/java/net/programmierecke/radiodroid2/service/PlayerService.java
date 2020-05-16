package net.programmierecke.radiodroid2.service;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.media.audiofx.AudioEffect;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.FavouriteManager;
import net.programmierecke.radiodroid2.HistoryManager;
import net.programmierecke.radiodroid2.IPlayerService;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.history.TrackHistoryEntry;
import net.programmierecke.radiodroid2.history.TrackHistoryRepository;
import net.programmierecke.radiodroid2.players.PlayState;
import net.programmierecke.radiodroid2.players.selector.PlayerType;
import net.programmierecke.radiodroid2.station.DataRadioStation;
import net.programmierecke.radiodroid2.station.live.ShoutcastInfo;
import net.programmierecke.radiodroid2.station.live.StreamLiveInfo;
import net.programmierecke.radiodroid2.players.RadioPlayer;
import net.programmierecke.radiodroid2.recording.RecordingsManager;
import net.programmierecke.radiodroid2.recording.RunningRecordingInfo;

import static android.content.Intent.ACTION_MEDIA_BUTTON;

public class PlayerService extends JobIntentService implements RadioPlayer.PlayerListener {
    protected static final int NOTIFY_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = "default";

    public static final String METERED_CONNECTION_WARNING_KEY = "warn_no_wifi";

    public static final String PLAYER_SERVICE_NO_NOTIFICATION_EXTRA = "no_notification";

    public static final String PLAYER_SERVICE_TIMER_UPDATE = "net.programmierecke.radiodroid2.timerupdate";
    public static final String PLAYER_SERVICE_META_UPDATE = "net.programmierecke.radiodroid2.metaupdate";

    public static final String PLAYER_SERVICE_STATE_CHANGE = "net.programmierecke.radiodroid2.statechange";
    public static final String PLAYER_SERVICE_STATE_EXTRA_KEY = "state";

    public static final String PLAYER_SERVICE_METERED_CONNECTION = "net.programmierecke.radiodroid2.metered_connection";
    public static final String PLAYER_SERVICE_METERED_CONNECTION_PLAYER_TYPE = "PLAYER_TYPE";

    public static final String PLAYER_SERVICE_BOUND = "net.programmierecke.radiodroid2.playerservicebound";

    private final String TAG = "PLAY";

    private final String ACTION_PAUSE = "pause";
    private final String ACTION_RESUME = "resume";
    private final String ACTION_SKIP_TO_NEXT = "next";
    private final String ACTION_SKIP_TO_PREVIOUS = "previous";
    private final String ACTION_STOP = "stop";

    private static final float FULL_VOLUME = 100f;
    private static final float DUCK_VOLUME = 40f;

    private static final int METERED_CONNECTION_WARNING_COOLDOWN = 20 * 1000; // 20 seconds

    private static final int AUDIO_WARNING_DURATION = 2000;

    private SharedPreferences sharedPref;

    private TrackHistoryRepository trackHistoryRepository;

    private Context itsContext;
    private Handler handler;

    private DataRadioStation currentStation;

    private BitmapDrawable radioIcon;

    private RadioPlayer radioPlayer;

    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private BecomingNoisyReceiver becomingNoisyReceiver = new BecomingNoisyReceiver();
    private HeadsetConnectionReceiver headsetConnectionReceiver = new HeadsetConnectionReceiver();
    private ConnectivityChecker connectivityChecker = new ConnectivityChecker();

    private PauseReason pauseReason = PauseReason.NONE;

    private int lastErrorFromPlayer = -1;

    private long lastMeteredConnectionWarningTime;

    private ToneGenerator toneGenerator;
    private Runnable toneGeneratorStopRunnable;

    private CountDownTimer timer;
    private long seconds = 0;

    private StreamLiveInfo liveInfo = new StreamLiveInfo(null);
    private ShoutcastInfo streamInfo;

    private boolean isHls = false;

    private long lastPlayStartTime = 0;

    private boolean notificationIsActive = false;

    void sendBroadCast(String action) {
        Intent local = new Intent();
        local.setAction(action);
        LocalBroadcastManager.getInstance(itsContext).sendBroadcast(local);
    }

    private final IPlayerService.Stub itsBinder = new IPlayerService.Stub() {
        // This method exist because we need to set information about current radio station
        // and then use it in playerFragment when MPD player is working.
        public void SetStation(DataRadioStation station) {
            PlayerService.this.setStation(station);
        }

        public void SkipToNext() throws RemoteException {
            PlayerService.this.next();
        }

        public void SkipToPrevious() throws RemoteException {
            PlayerService.this.previous();
        }

        public void Play(boolean isAlarm) throws RemoteException {
            PlayerService.this.playCurrentStation(isAlarm);
        }

        public void Pause(PauseReason pauseReason) throws RemoteException {
            PlayerService.this.pause(pauseReason);
        }

        public void Resume() throws RemoteException {
            PlayerService.this.resume();
        }

        public void Stop() throws RemoteException {
            PlayerService.this.stop();
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
            return currentStation != null ? currentStation.StationUuid : null;
        }

        @Override
        public DataRadioStation getCurrentStation() throws RemoteException {
            return currentStation;
        }

        @Override
        public StreamLiveInfo getMetadataLive() throws RemoteException {
            return PlayerService.this.liveInfo;
        }

        @Override
        public ShoutcastInfo getShoutcastInfo() throws RemoteException {
            return streamInfo;
        }

        @Override
        public MediaSessionCompat.Token getMediaSessionToken() throws RemoteException {
            return PlayerService.this.mediaSession.getSessionToken();
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
        public PlayState getPlayerState() throws RemoteException {
            return radioPlayer.getPlayState();
        }

        @Override
        public void startRecording() throws RemoteException {
            if (radioPlayer != null) {
                RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
                RecordingsManager recordingsManager = radioDroidApp.getRecordingsManager();

                recordingsManager.record(PlayerService.this, radioPlayer);

                sendBroadCast(PLAYER_SERVICE_META_UPDATE);
            }
        }

        @Override
        public void stopRecording() throws RemoteException {
            if (radioPlayer != null) {
                RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
                RecordingsManager recordingsManager = radioDroidApp.getRecordingsManager();

                recordingsManager.stopRecording(radioPlayer);

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
                RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
                RecordingsManager recordingsManager = radioDroidApp.getRecordingsManager();

                RunningRecordingInfo info = recordingsManager.getRecordingInfo(radioPlayer);
                if (info != null) {
                    return info.getFileName();
                }
            }
            return null;
        }

        @Override
        public long getTransferredBytes() throws RemoteException {
            if (radioPlayer != null) {
                return radioPlayer.getCurrentPlaybackTransferredBytes();
            }
            return 0;
        }

        @Override
        public long getBufferedSeconds() throws RemoteException {
            if (radioPlayer != null) {
                return radioPlayer.getBufferedSeconds();
            }
            return 0;
        }

        @Override
        public long getLastPlayStartTime() throws RemoteException {
            return lastPlayStartTime;
        }

        @Override
        public PauseReason getPauseReason() throws RemoteException {
            return PlayerService.this.pauseReason;
        }

        @Override
        public void enableMPD(String hostname, int port) throws RemoteException {
            if (radioPlayer != null) {
                // radioPlayer.enableMPDPlayer(hostname, port);
            }
        }

        @Override
        public void disableMPD() throws RemoteException {
            if (radioPlayer != null) {
                // radioPlayer.disableMPDPlayer();
            }
        }

        @Override
        public void warnAboutMeteredConnection(PlayerType playerType) throws RemoteException {
            PlayerService.this.warnAboutMeteredConnection(playerType);
        }
    };

    private MediaSessionCompat.Callback mediaSessionCallback = null;

    private AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if (!radioPlayer.isLocal()) {
                        return;
                    }

                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (BuildConfig.DEBUG) Log.d(TAG, "audio focus gain");

                            if (pauseReason == PauseReason.FOCUS_LOSS_TRANSIENT) {
                                enableMediaSession();
                                resume();
                            }

                            radioPlayer.setVolume(FULL_VOLUME);
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            if (BuildConfig.DEBUG) Log.d(TAG, "audio focus loss");

                            if (radioPlayer.isPlaying()) {
                                pause(PauseReason.FOCUS_LOSS);
                            }

                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (BuildConfig.DEBUG) Log.d(TAG, "audio focus loss transient");

                            if (radioPlayer.isPlaying()) {
                                pause(PauseReason.FOCUS_LOSS_TRANSIENT);
                            }

                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "audio focus loss transient can duck");

                            radioPlayer.setVolume(DUCK_VOLUME);
                            break;
                    }
                }
            };

    private ConnectivityChecker.ConnectivityCallback connectivityCallback = new ConnectivityChecker.ConnectivityCallback() {
        @Override
        public void onConnectivityChanged(boolean connected, ConnectivityChecker.ConnectionType connectionType) {
            if (connectionType == ConnectivityChecker.ConnectionType.METERED && sharedPref.getBoolean(METERED_CONNECTION_WARNING_KEY, false)) {
                warnAboutMeteredConnection(PlayerType.RADIODROID);
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
                if (BuildConfig.DEBUG) Log.d(TAG, "" + seconds);

                sendBroadCast(PLAYER_SERVICE_TIMER_UPDATE);
            }

            public void onFinish() {
                stop();
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

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        handler = new Handler(getMainLooper());

        itsContext = this;
        timer = null;
        powerManager = (PowerManager) itsContext.getSystemService(Context.POWER_SERVICE);
        audioManager = (AudioManager) itsContext.getSystemService(Context.AUDIO_SERVICE);
        radioIcon = ((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher, null));

        radioPlayer = new RadioPlayer(PlayerService.this);
        radioPlayer.setPlayerListener(this);

        mediaSessionCallback = new MediaSessionCallback(this, itsBinder);

        mediaSession = new MediaSessionCompat(getBaseContext(), getBaseContext().getPackageName());
        mediaSession.setCallback(mediaSessionCallback);

        Intent startActivityIntent = new Intent(itsContext.getApplicationContext(), ActivityMain.class);
        mediaSession.setSessionActivity(PendingIntent.getActivity(itsContext.getApplicationContext(), 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
        trackHistoryRepository = radioDroidApp.getTrackHistoryRepository();

        final IntentFilter headsetConnectionFilter = new IntentFilter();
        headsetConnectionFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        headsetConnectionFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        headsetConnectionFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);

        registerReceiver(headsetConnectionReceiver, headsetConnectionFilter);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "RadioDroid2 Player", NotificationManager.IMPORTANCE_LOW);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) Log.d(TAG, "PlayService should be destroyed.");

        stop();

        mediaSession.release();

        radioPlayer.destroy();

        unregisterReceiver(headsetConnectionReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Service could be started by external forces, e.g. when we had the last media session
        // and user presses play/pause media button.
        PlayerServiceUtil.bind(itsContext.getApplicationContext());

        if (currentStation == null) {
            RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
            HistoryManager historyManager = radioDroidApp.getHistoryManager();
            currentStation = historyManager.getFirst();
        }

        if (currentStation == null) {
            RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
            FavouriteManager favouriteManager = radioDroidApp.getFavouriteManager();
            currentStation = favouriteManager.getFirst();
        }

        boolean showNotification = true;

        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_SKIP_TO_PREVIOUS:
                        previous();
                        break;
                    case ACTION_SKIP_TO_NEXT:
                        next();
                        break;
                    case ACTION_STOP:
                        stop();
                        return START_NOT_STICKY;
                    case ACTION_PAUSE:
                        pause(PauseReason.USER);
                        break;
                    case ACTION_RESUME:
                        resume();
                        break;
                    case ACTION_MEDIA_BUTTON:
                        KeyEvent key = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                        if (key.getAction() == KeyEvent.ACTION_UP) {
                            int keycode = key.getKeyCode();
                            switch (keycode) {
                                case KeyEvent.KEYCODE_MEDIA_PLAY:
                                    resume();
                                    break;
                                case KeyEvent.KEYCODE_MEDIA_NEXT:
                                    next();
                                    break;
                                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                    previous();
                                    break;
                            }
                        }
                        break;
                }
            }

            MediaButtonReceiver.handleIntent(mediaSession, intent);

            showNotification = !intent.getBooleanExtra(PLAYER_SERVICE_NO_NOTIFICATION_EXTRA, false);
        }

        // It is an error for service started via Context.startForegroundService not to create
        // a notification since Android O. It must call startForeground within 5 seconds of being started.
        // Thus if we can show a notification - we always show it.
        if (showNotification && !notificationIsActive) {
            if (currentStation == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // On Android O+ we MUST show notification if started via startForegroundService

                    NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                            "Temporary", NotificationManager.IMPORTANCE_DEFAULT);
                    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
                    Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                            .setContentTitle("")
                            .setContentText("").build();
                    startForeground(NOTIFY_ID, notification);
                    stopForeground(true);
                } else {
                    stopSelf();
                    return START_NOT_STICKY;
                }
            } else {
                updateNotification(PlayState.Paused);
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void playWithoutWarnings(DataRadioStation station) {
        setStation(station);
        playCurrentStation(false);
    }

    private void playAndWarnIfMetered(DataRadioStation station) {
        RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
        Utils.playAndWarnIfMetered(radioDroidApp, station, PlayerType.RADIODROID,
                () -> playWithoutWarnings(station),
                (station1, playerType) -> {
                    setStation(station1);
                    warnAboutMeteredConnection(playerType);
                });
    }

    public void setStation(DataRadioStation station) {
        this.currentStation = station;
    }

    public void playCurrentStation(final boolean isAlarm) {
        if (Utils.shouldLoadIcons(itsContext))
            downloadRadioIcon();

        int result = acquireAudioFocus();
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Start playback.
            enableMediaSession();

            liveInfo = new StreamLiveInfo(null);
            streamInfo = null;

            acquireWakeLockAndWifiLock();

            radioPlayer.play(currentStation, isAlarm);
        }
    }

    public void pause(PauseReason pauseReason) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, String.format("pausing playback, reason %s", pauseReason.toString()));
        }

        this.pauseReason = pauseReason;

        forceStopAudioWarning();

        if (pauseReason == PauseReason.METERED_CONNECTION) {
            lastMeteredConnectionWarningTime = System.currentTimeMillis();
        }

        releaseWakeLockAndWifiLock();

        // Pausing due to focus loss means that we can gain it again
        // so we should keep the focus and the wait for callback.
        if (pauseReason != PauseReason.FOCUS_LOSS_TRANSIENT) {
            releaseAudioFocus();
        }

        radioPlayer.pause();
    }

    public void next() {
        if (currentStation == null) {
            return;
        }

        RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
        DataRadioStation station = radioDroidApp.getFavouriteManager().getNextById(currentStation.StationUuid);

        if (station != null) {
            if (radioPlayer.isPlaying()) {
                // Since we are using data at the moment user doesn't need any notifications about
                // metered connection because he already received them if there were any.
                playWithoutWarnings(station);
            } else {
                playAndWarnIfMetered(station);
            }
        }
    }

    public void previous() {
        if (currentStation == null) {
            return;
        }

        RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
        DataRadioStation station = radioDroidApp.getFavouriteManager().getPreviousById(currentStation.StationUuid);
        if (station != null) {
            if (radioPlayer.isPlaying()) {
                playWithoutWarnings(station);
            } else {
                playAndWarnIfMetered(station);
            }
        }
    }

    public void resume() {
        if (BuildConfig.DEBUG) Log.d(TAG, "resuming playback.");

        forceStopAudioWarning();

        boolean bypassMeteredConnectionWarning = false;

        if (pauseReason == PauseReason.METERED_CONNECTION) {
            long now = System.currentTimeMillis();
            long delta = now - lastMeteredConnectionWarningTime;

            bypassMeteredConnectionWarning = delta < METERED_CONNECTION_WARNING_COOLDOWN && delta > 0;
        }

        this.pauseReason = PauseReason.NONE;
        this.lastMeteredConnectionWarningTime = 0;

        if (!radioPlayer.isPlaying()) {
            RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
            DataRadioStation station = currentStation;

            if (currentStation == null) {
                HistoryManager historyManager = radioDroidApp.getHistoryManager();
                station = historyManager.getFirst();
            }

            if (station != null) {
                if (bypassMeteredConnectionWarning) {
                    startMeteredConnectionListener();
                    acquireAudioFocus();

                    playWithoutWarnings(station);
                } else {
                    playAndWarnIfMetered(station);
                }

            }
        }
    }

    public void stop() {
        if (BuildConfig.DEBUG) Log.d(TAG, "stopping playback.");

        this.pauseReason = PauseReason.NONE;
        this.lastMeteredConnectionWarningTime = 0;
        this.notificationIsActive = false;

        liveInfo = new StreamLiveInfo(null);
        streamInfo = null;

        forceStopAudioWarning();

        releaseAudioFocus();
        disableMediaSession();
        radioPlayer.stop();
        releaseWakeLockAndWifiLock();
        clearTimer();

        stopForeground(true);

        stopMeteredConnectionListener();

        //sendBroadCast(PLAYER_SERVICE_STATE_CHANGE);
    }

    private void setMediaPlaybackState(int state) {
        if (mediaSession == null) {
            return;
        }

        long actions = PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                | PlaybackStateCompat.ACTION_PLAY_PAUSE;

        if (state == PlaybackStateCompat.STATE_BUFFERING || state == PlaybackStateCompat.STATE_PLAYING) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }

        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        playbackStateBuilder.setActions(actions);

        if (state == PlaybackStateCompat.STATE_ERROR) {
            String error = "";

            PlayState currentPlayerState = radioPlayer.getPlayState();
            if ((currentPlayerState == PlayState.Paused || currentPlayerState == PlayState.Idle)
                    && pauseReason == PauseReason.METERED_CONNECTION) {
                error = itsContext.getResources().getString(R.string.notify_metered_connection);
            } else {
                try {
                    error = itsContext.getResources().getString(lastErrorFromPlayer);
                } catch (Resources.NotFoundException ex) {
                    Log.e(TAG, String.format("Unknown play error: %d", lastErrorFromPlayer), ex);
                }
            }

            playbackStateBuilder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_ACTION_ABORTED, error);
        }

        playbackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mediaSession.setPlaybackState(playbackStateBuilder.build());
    }

    private void enableMediaSession() {
        if (BuildConfig.DEBUG) Log.d(TAG, "enabling media session.");

        IntentFilter becomingNoisyFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, becomingNoisyFilter);

        mediaSession.setActive(true);

        setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
    }

    private void disableMediaSession() {

        if (mediaSession.isActive()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "disabling media session.");

            mediaSession.setActive(false);

            unregisterReceiver(becomingNoisyReceiver);
        }
    }

    private int acquireAudioFocus() {
        if (BuildConfig.DEBUG) Log.d(TAG, "acquiring audio focus.");

        int result = audioManager.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e(TAG, "acquiring audio focus failed!");
            toastOnUi(R.string.error_grant_audiofocus);
        }

        return result;
    }

    private void releaseAudioFocus() {
        if (BuildConfig.DEBUG) Log.d(TAG, "releasing audio focus.");

        audioManager.abandonAudioFocus(afChangeListener);
    }

    void acquireWakeLockAndWifiLock() {
        if (BuildConfig.DEBUG) Log.d(TAG, "acquiring wake lock and wifi lock.");

        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PlayerService:");
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "wake lock is already acquired.");
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
                wifiLock.acquire();
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "wifi lock is already acquired.");
            }
        } else {
            Log.e(TAG, "could not acquire wifi lock, WifiManager does not exist!");
        }
    }

    private void releaseWakeLockAndWifiLock() {
        if (BuildConfig.DEBUG) Log.d(TAG, "releasing wake lock and wifi lock.");

        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            wakeLock = null;
        }

        if (wifiLock != null) {
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
            wifiLock = null;
        }
    }

    private void sendMessage(String theTitle, String theMessage, String theTicker) {
        Intent notificationIntent = new Intent(itsContext, ActivityMain.class);
        notificationIntent.putExtra("stationid", currentStation.StationUuid);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent stopIntent = new Intent(itsContext, PlayerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pendingIntentStop = PendingIntent.getService(itsContext, 0, stopIntent, 0);

        Intent nextIntent = new Intent(itsContext, PlayerService.class);
        nextIntent.setAction(ACTION_SKIP_TO_NEXT);
        PendingIntent pendingIntentNext = PendingIntent.getService(itsContext, 0, nextIntent, 0);

        Intent previousIntent = new Intent(itsContext, PlayerService.class);
        previousIntent.setAction(ACTION_SKIP_TO_PREVIOUS);
        PendingIntent pendingIntentPrevious = PendingIntent.getService(itsContext, 0, previousIntent, 0);

        PlayState currentPlayerState = radioPlayer.getPlayState();

        if ((currentPlayerState == PlayState.Paused || currentPlayerState == PlayState.Idle)
                && pauseReason == PauseReason.METERED_CONNECTION) {
            theMessage = itsContext.getResources().getString(R.string.notify_metered_connection);
        } else if (lastErrorFromPlayer != -1) {
            try {
                theMessage = itsContext.getResources().getString(lastErrorFromPlayer);
            } catch (Resources.NotFoundException ex) {
                Log.e(TAG, String.format("Unknown play error: %d", lastErrorFromPlayer), ex);
            }
        }

        PendingIntent contentIntent = PendingIntent.getActivity(itsContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(itsContext, NOTIFICATION_CHANNEL_ID)
                .setContentIntent(contentIntent)
                .setContentTitle(theTitle)
                .setContentText(theMessage)
                .setWhen(System.currentTimeMillis())
                .setTicker(theTicker)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_play_arrow_white_24dp)
                .setLargeIcon(radioIcon.getBitmap())
                .addAction(R.drawable.ic_stop_white_24dp, getString(R.string.action_stop), pendingIntentStop)
                .addAction(R.drawable.ic_skip_previous_24dp, getString(R.string.action_skip_to_previous), pendingIntentPrevious);

        if (currentPlayerState == PlayState.Playing || currentPlayerState == PlayState.PrePlaying) {
            Intent pauseIntent = new Intent(itsContext, PlayerService.class);
            pauseIntent.setAction(ACTION_PAUSE);
            PendingIntent pendingIntentPause = PendingIntent.getService(itsContext, 0, pauseIntent, 0);

            notificationBuilder.addAction(R.drawable.ic_pause_white_24dp, getString(R.string.action_pause), pendingIntentPause);
            notificationBuilder.setUsesChronometer(true)
                    .setOngoing(true);
        } else if (currentPlayerState == PlayState.Paused || currentPlayerState == PlayState.Idle) {
            Intent resumeIntent = new Intent(itsContext, PlayerService.class);
            resumeIntent.setAction(ACTION_RESUME);
            PendingIntent pendingIntentResume = PendingIntent.getService(itsContext, 0, resumeIntent, 0);

            notificationBuilder.addAction(R.drawable.ic_play_arrow_white_24dp, getString(R.string.action_resume), pendingIntentResume);
            notificationBuilder.setUsesChronometer(false)
                    .setDeleteIntent(pendingIntentStop)
                    .setOngoing(false);
        }

        notificationBuilder.addAction(R.drawable.ic_skip_next_24dp, getString(R.string.action_skip_to_next), pendingIntentNext)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(1, 2, 3 /* previous, play/pause, next */)
                        .setCancelButtonIntent(pendingIntentStop)
                        .setShowCancelButton(true));
        Notification notification = notificationBuilder.build();

        startForeground(NOTIFY_ID, notification);
        notificationIsActive = true;

        if (currentPlayerState == PlayState.Paused || currentPlayerState == PlayState.Idle) {
            stopForeground(false); // necessary to make notification dismissible
        }
    }

    private void toastOnUi(final int messageId) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(itsContext, itsContext.getResources().getString(messageId), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNotification() {
        updateNotification(radioPlayer.getPlayState());
    }

    private void updateNotification(PlayState playState) {
        switch (playState) {
            case Idle:
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                notificationManager.cancel(NOTIFY_ID);

                setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
                break;
            case PrePlaying:
                sendMessage(currentStation.Name, itsContext.getResources().getString(R.string.notify_pre_play), itsContext.getResources().getString(R.string.notify_pre_play));

                setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                break;
            case Playing:
                final String title = liveInfo.getTitle();
                if (!TextUtils.isEmpty(title)) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "update message:" + title);
                    sendMessage(currentStation.Name, title, title);
                } else {
                    sendMessage(currentStation.Name, itsContext.getResources().getString(R.string.notify_play), currentStation.Name);
                }

                if (mediaSession != null) {
                    final MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
                    builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentStation.Name);
                    builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, liveInfo.getArtist());
                    builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, liveInfo.getTrack());
                    builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentStation.Name);
                    if (liveInfo.hasArtistAndTrack()) {
                        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, liveInfo.getArtist());
                        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, liveInfo.getTrack());
                    } else {
                        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, liveInfo.getTitle());
                        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentStation.Name); // needed for android-media-controller to show an icon
                    }
                    builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, radioIcon.getBitmap());
                    builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, radioIcon.getBitmap());
                    mediaSession.setMetadata(builder.build());
                }

                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

                break;
            case Paused:
                sendMessage(currentStation.Name, itsContext.getResources().getString(R.string.notify_paused), currentStation.Name);

                if (lastErrorFromPlayer != -1) {
                    setMediaPlaybackState(PlaybackStateCompat.STATE_ERROR);
                } else {
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                }

                break;
        }
    }

    private void downloadRadioIcon() {
        final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, getResources().getDisplayMetrics());

        if (!currentStation.hasIcon()) {
            radioIcon = (BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher, null);
            updateNotification();
            return;
        }

        Picasso.get()
                .load(currentStation.IconUrl)
                .resize((int) px, 0)
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        final boolean useCircularIcons = Utils.useCircularIcons(itsContext);
                        if (!useCircularIcons)
                            radioIcon = new BitmapDrawable(getResources(), bitmap);
                        else {
                            // Icon is not circular with this code. So we need to create custom notification view and then use RoundedBitmapDrawable there
                            RoundedBitmapDrawable rb = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                            rb.setCircular(true);
                            radioIcon = new BitmapDrawable(getResources(), rb.getBitmap());
                        }
                        updateNotification();
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });
    }

    private void warnAboutMeteredConnection(PlayerType playerType) {
        // The idea is to play a warning and give user some time frame to press the play media button
        // again to resume the playback. However media buttons may not work as expected and think
        // that current state is "playing" and send us "pause" regardless of our attempt to set state
        // to "paused".
        // FIXME: Make media buttons send correct events considering the above.

        stopMeteredConnectionListener();

        pause(PauseReason.METERED_CONNECTION);

        handler.post(() -> {
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGenerator.startTone(ToneGenerator.TONE_SUP_RADIO_NOTAVAIL, AUDIO_WARNING_DURATION);
        });

        toneGeneratorStopRunnable = () -> {
            if (toneGenerator != null) {
                toneGenerator.stopTone();
                toneGenerator.release();
                toneGenerator = null;
            }
            toneGeneratorStopRunnable = null;

            setMediaPlaybackState(PlaybackStateCompat.STATE_ERROR);
        };

        handler.postDelayed(toneGeneratorStopRunnable, AUDIO_WARNING_DURATION);

        Intent broadcast = new Intent();
        broadcast.setAction(PLAYER_SERVICE_METERED_CONNECTION);
        broadcast.putExtra(PLAYER_SERVICE_METERED_CONNECTION_PLAYER_TYPE, (Parcelable) playerType);
        LocalBroadcastManager.getInstance(itsContext).sendBroadcast(broadcast);

        updateNotification(PlayState.Paused);
    }

    private void forceStopAudioWarning() {
        if (toneGenerator != null) {
            handler.removeCallbacks(toneGeneratorStopRunnable);
            toneGeneratorStopRunnable = null;

            handler.post(() -> {
                if (toneGenerator != null) {
                    toneGenerator.stopTone();
                    toneGenerator.release();
                    toneGenerator = null;
                }
            });
        }
    }

    private void startMeteredConnectionListener() {
        if (sharedPref.getBoolean(METERED_CONNECTION_WARNING_KEY, false)) {
            connectivityChecker.startListening(PlayerService.this, connectivityCallback);
        }
    }

    private void stopMeteredConnectionListener() {
        connectivityChecker.stopListening(PlayerService.this);
    }

    @Override
    public void onStateChanged(final PlayState state, final int audioSessionId) {
        // State changed can be called from the player's thread.

        handler.post(new Runnable() {
            @Override
            public void run() {
                lastErrorFromPlayer = -1;

                switch (state) {
                    case Paused:
                        break;
                    case Playing: {
                        enableMediaSession();

                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Open audio effect control session, session id=" + audioSessionId);
                        }

                        lastPlayStartTime = System.currentTimeMillis();

                        Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
                        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
                        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
                        itsContext.sendBroadcast(i);
                        break;
                    }
                    default: {
                        if (state != PlayState.PrePlaying) {
                            disableMediaSession();
                        }

                        if (audioSessionId > 0) {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "Close audio effect control session, session id=" + audioSessionId);
                            }

                            Intent i = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
                            i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
                            i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
                            itsContext.sendBroadcast(i);
                        }

                        if (state == PlayState.Idle) {
                            stop();
                        }

                        break;
                    }
                }

                if (state != PlayState.Paused && state != PlayState.Idle) {
                    startMeteredConnectionListener();
                } else {
                    stopMeteredConnectionListener();
                }

                updateNotification(state);

                final Intent intent = new Intent();
                intent.setAction(PLAYER_SERVICE_STATE_CHANGE);
                intent.putExtra(PLAYER_SERVICE_STATE_EXTRA_KEY, (Parcelable) state);
                LocalBroadcastManager.getInstance(itsContext).sendBroadcast(intent);
            }
        });
    }

    @Override
    public void onPlayerWarning(int messageId) {
        onPlayerError(messageId);
    }

    @Override
    public void onPlayerError(int messageId) {
        handler.post(() -> {
            PlayerService.this.lastErrorFromPlayer = messageId;

            toastOnUi(messageId);
            updateNotification();
        });
    }

    @Override
    public void onBufferedTimeUpdate(long bufferedMs) {

    }

    @Override
    public void foundShoutcastStream(ShoutcastInfo info, boolean isHls) {
        this.streamInfo = info;
        this.isHls = isHls;
        if (info != null) {
//            if (info.audioName != null) {
//                if (!info.audioName.trim().equals("")) {
//                    currentStationName = info.audioName.trim();
//                }
//            }

            if (BuildConfig.DEBUG) {
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
    public void foundLiveStreamInfo(final StreamLiveInfo liveInfo) {
        StreamLiveInfo oldLiveInfo = this.liveInfo;
        this.liveInfo = liveInfo;

        if (BuildConfig.DEBUG) {
            Map<String, String> rawMetadata = liveInfo.getRawMetadata();
            for (String key : rawMetadata.keySet()) {
                Log.i(TAG, "INFO:" + key + "=" + rawMetadata.get(key));
            }
        }

        if (oldLiveInfo == null || !oldLiveInfo.getTitle().equals(liveInfo.getTitle())) {
            sendBroadCast(PLAYER_SERVICE_META_UPDATE);
            updateNotification();

            Calendar calendar = Calendar.getInstance();
            Date currentTime = calendar.getTime();

            trackHistoryRepository.getLastInsertedHistoryItem((trackHistoryEntry, dao) -> {
                if (trackHistoryEntry != null && trackHistoryEntry.title.equals(liveInfo.getTitle())) {
                    // Prevent from generating several same entries when rapidly doing pause and resume.
                    trackHistoryEntry.endTime = new Date(0);
                    dao.update(trackHistoryEntry);
                } else {
                    dao.setCurrentPlayingTrackEndTime(currentTime);

                    TrackHistoryEntry newTrackHistoryEntry = new TrackHistoryEntry();
                    newTrackHistoryEntry.stationUuid = currentStation.StationUuid;
                    newTrackHistoryEntry.artist = liveInfo.getArtist();
                    newTrackHistoryEntry.title = liveInfo.getTitle();
                    newTrackHistoryEntry.track = liveInfo.getTrack();
                    newTrackHistoryEntry.stationIconUrl = currentStation.IconUrl;
                    newTrackHistoryEntry.startTime = currentTime;
                    newTrackHistoryEntry.endTime = new Date(0);

                    trackHistoryRepository.insert(newTrackHistoryEntry);
                }
            });
        }
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "onHandleWork called with intent: " + intent.toString());
    }
}
