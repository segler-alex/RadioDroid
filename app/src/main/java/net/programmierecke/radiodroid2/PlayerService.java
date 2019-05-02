package net.programmierecke.radiodroid2;

import java.util.List;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.programmierecke.radiodroid2.data.DataRadioStation;
import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.data.StreamLiveInfo;
import net.programmierecke.radiodroid2.players.RadioPlayer;
import net.programmierecke.radiodroid2.recording.RecordingsManager;
import net.programmierecke.radiodroid2.recording.RunningRecordingInfo;
import net.programmierecke.radiodroid2.utils.GetRealLinkAndPlayTask;

import static android.content.Intent.ACTION_MEDIA_BUTTON;

public class PlayerService extends Service implements RadioPlayer.PlayerListener {
    protected static final int NOTIFY_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = "default";

    public static final String PLAYER_SERVICE_TIMER_UPDATE = "net.programmierecke.radiodroid2.timerupdate";
    public static final String PLAYER_SERVICE_STATUS_UPDATE = "net.programmierecke.radiodroid2.statusupdate";
    public static final String PLAYER_SERVICE_META_UPDATE = "net.programmierecke.radiodroid2.metaupdate";

    private final String TAG = "PLAY";

    private final String ACTION_PAUSE = "pause";
    private final String ACTION_RESUME = "resume";
    private final String ACTION_SKIP_TO_NEXT = "next";
    private final String ACTION_SKIP_TO_PREVIOUS = "previous";
    private final String ACTION_STOP = "stop";

    private static final float FULL_VOLUME = 100f;
    private static final float DUCK_VOLUME = 40f;

    private Context itsContext;

    private String currentStationID;
    private String currentStationName;
    private String currentStationURL;
    private String currentStationIconUrl;

    private BitmapDrawable radioIcon;

    private RadioPlayer radioPlayer;

    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private BecomingNoisyReceiver becomingNoisyReceiver = new BecomingNoisyReceiver();

    private boolean resumeOnFocusGain = false;

    private CountDownTimer timer;
    private long seconds = 0;

    private StreamLiveInfo liveInfo = new StreamLiveInfo(null);
    private ShoutcastInfo streamInfo;

    private boolean isHls = false;

    void sendBroadCast(String action) {
        Intent local = new Intent();
        local.setAction(action);
        sendBroadcast(local);
    }

    private final IPlayerService.Stub itsBinder = new IPlayerService.Stub() {
        // This method exist because we need to set information about current radio station
        // and then use it in playerFragment when MPD player is working.
        public void SaveInfo(String theUrl, String theName, String theID, String theIconUrl) {
            PlayerService.this.saveInfo(theUrl, theName, theID, theIconUrl);
        }

        public void SkipToNext() throws RemoteException {
            PlayerService.this.next();
        }

        public void SkipToPrevious() throws RemoteException {
            PlayerService.this.previous();
        }

        public void Play(boolean isAlarm) throws RemoteException {
            PlayerService.this.playUrl(isAlarm);
        }

        public void Pause() throws RemoteException {
            PlayerService.this.pause();
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
            return currentStationID;
        }

        @Override
        public String getStationName() throws RemoteException {
            return currentStationName;
        }

        @Override
        public String getStationIconUrl() throws RemoteException {
            return currentStationIconUrl;
        }

        @Override
        public StreamLiveInfo getMetadataLive() throws RemoteException {
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
    };

    private MediaSessionCompat.Callback mediaSessionCallback = null;

    private AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (BuildConfig.DEBUG) Log.d(TAG, "audio focus gain");

                            if (resumeOnFocusGain) {
                                enableMediaSession();
                                resume();
                            }

                            radioPlayer.setVolume(FULL_VOLUME);
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            if (BuildConfig.DEBUG) Log.d(TAG, "audio focus loss");

                            stop();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (BuildConfig.DEBUG) Log.d(TAG, "audio focus loss transient");

                            boolean resume = radioPlayer.isPlaying() || resumeOnFocusGain;

                            pause();

                            resumeOnFocusGain = resume;
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "audio focus loss transient can duck");

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
                if (BuildConfig.DEBUG) Log.d(TAG, "" + seconds);

                Intent local = new Intent();
                local.setAction(PLAYER_SERVICE_TIMER_UPDATE);
                sendBroadcast(local);
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

        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) Log.d(TAG, "PlayService should be destroyed.");

        stop();

        mediaSession.release();

        radioPlayer.destroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
                        break;
                    case ACTION_PAUSE:
                        pause();
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
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void saveInfo(String theURL, String theName, String theID, String theIconUrl) {
        currentStationID = theID;
        currentStationName = theName;
        currentStationURL = theURL;
        currentStationIconUrl = theIconUrl;
    }

    public void playUrl(final boolean isAlarm) {
        Log.i(TAG, String.format("playing url '%s'.", currentStationURL));

        if (Utils.shouldLoadIcons(itsContext))
            downloadRadioIcon();

        int result = acquireAudioFocus();
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Start playback.
            enableMediaSession();
            replayCurrent(isAlarm);
        }
    }

    public void pause() {
        if (BuildConfig.DEBUG) Log.d(TAG, "pausing playback.");

        resumeOnFocusGain = false;

        releaseWakeLockAndWifiLock();
        radioPlayer.pause();
    }

    public void next() {
        RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
        DataRadioStation station = radioDroidApp.getFavouriteManager().getNextById(currentStationID);
        if (station != null)
            new GetRealLinkAndPlayTask(itsContext, station, itsBinder).execute();
    }

    public void previous() {
        RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
        DataRadioStation station = radioDroidApp.getFavouriteManager().getPreviousById(currentStationID);
        if (station != null)
            new GetRealLinkAndPlayTask(itsContext, station, itsBinder).execute();
    }

    public void resume() {
        if (BuildConfig.DEBUG) Log.d(TAG, "resuming playback.");

        resumeOnFocusGain = false;

        if (!radioPlayer.isPlaying()) {
            acquireAudioFocus();
            if(currentStationURL != null) {
                replayCurrent(false);
            } else {
                DataRadioStation lastStation;
                RadioDroidApp radioDroidApp = (RadioDroidApp) getApplication();
                HistoryManager historyManager = radioDroidApp.getHistoryManager();
                List<DataRadioStation> history = historyManager.getList();

                if (history.size() > 0) {
                    lastStation = history.get(0);
                    new GetRealLinkAndPlayTask(itsContext, lastStation, itsBinder).execute();
                }
            }
        }
    }

    public void stop() {
        if (BuildConfig.DEBUG) Log.d(TAG, "stopping playback.");

        resumeOnFocusGain = false;

        liveInfo = new StreamLiveInfo(null);
        streamInfo = null;

        releaseAudioFocus();
        disableMediaSession();
        radioPlayer.stop();
        releaseWakeLockAndWifiLock();
        clearTimer();

        stopForeground(true);

        sendBroadCast(PLAYER_SERVICE_STATUS_UPDATE);
    }

    public void replayCurrent(final boolean isAlarm) {
        if (BuildConfig.DEBUG) Log.d(TAG, "replaying current.");

        liveInfo = new StreamLiveInfo(null);
        streamInfo = null;

        acquireWakeLockAndWifiLock();

        radioPlayer.play(currentStationURL, currentStationName, isAlarm);
    }

    private void setMediaPlaybackState(int state) {
        if (mediaSession == null) {
            return;
        }

        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_PAUSE);

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

        if(mediaSession.isActive())
        {
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
        notificationIntent.putExtra("stationid", currentStationID);
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

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "RadioDroid2 Player", NotificationManager.IMPORTANCE_LOW);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(itsContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(itsContext,NOTIFICATION_CHANNEL_ID)
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

        RadioPlayer.PlayState currentPlayerState = radioPlayer.getPlayState();

        if (currentPlayerState == RadioPlayer.PlayState.Playing || currentPlayerState == RadioPlayer.PlayState.PrePlaying) {
            Intent pauseIntent = new Intent(itsContext, PlayerService.class);
            pauseIntent.setAction(ACTION_PAUSE);
            PendingIntent pendingIntentPause = PendingIntent.getService(itsContext, 0, pauseIntent, 0);

            notificationBuilder.addAction(R.drawable.ic_pause_white_24dp, getString(R.string.action_pause), pendingIntentPause);
            notificationBuilder.setUsesChronometer(true)
                    .setOngoing(true);
        } else if (currentPlayerState == RadioPlayer.PlayState.Paused || currentPlayerState == RadioPlayer.PlayState.Idle) {
            Intent resumeIntent = new Intent(itsContext, PlayerService.class);
            resumeIntent.setAction(ACTION_RESUME);
            PendingIntent pendingIntentResume = PendingIntent.getService(itsContext, 0, resumeIntent, 0);

            notificationBuilder.addAction(R.drawable.ic_play_arrow_white_24dp, getString(R.string.action_resume), pendingIntentResume);
            notificationBuilder.setUsesChronometer(false)
                    .setDeleteIntent(pendingIntentStop)
                    .setOngoing(false);
        }

        notificationBuilder.addAction(R.drawable.ic_skip_next_24dp, getString(R.string.action_skip_to_next), pendingIntentNext)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(1, 2, 3 /* previous, play/pause, next */)
                        .setCancelButtonIntent(pendingIntentStop)
                        .setShowCancelButton(true));
        Notification notification = notificationBuilder.build();

        startForeground(NOTIFY_ID, notification);

        if (currentPlayerState == RadioPlayer.PlayState.Paused) {
            stopForeground(false); // necessary to make notification dismissible
        }
    }

    private void toastOnUi(final int messageId) {
        Handler h = new Handler(itsContext.getMainLooper());

        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(itsContext, itsContext.getResources().getString(messageId), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNotification() {
        updateNotification(radioPlayer.getPlayState());
    }

    private void updateNotification(RadioPlayer.PlayState playState) {
        switch (playState) {
            case Idle:
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                notificationManager.cancel(NOTIFY_ID);
                break;
            case PrePlaying:
                sendMessage(currentStationName, itsContext.getResources().getString(R.string.notify_pre_play), itsContext.getResources().getString(R.string.notify_pre_play));
                break;
            case Playing:
                final String title = liveInfo.getTitle();
                if (!TextUtils.isEmpty(title)) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "update message:" + title);
                    sendMessage(currentStationName, title, title);
                } else {
                    sendMessage(currentStationName, itsContext.getResources().getString(R.string.notify_play), currentStationName);
                }

                if (mediaSession != null) {
                    final MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
                    builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, liveInfo.getArtist());
                    builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, liveInfo.getTrack());
                    mediaSession.setMetadata(builder.build());
                }
                break;
            case Paused:
                sendMessage(currentStationName, itsContext.getResources().getString(R.string.notify_paused), currentStationName);
                break;
        }
    }

    private void downloadRadioIcon() {
        final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, getResources().getDisplayMetrics());

        if (currentStationIconUrl == null) return;
        if (currentStationIconUrl.trim().equals("")) return;

        Picasso.with(getApplicationContext())
                .load(currentStationIconUrl)
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
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });
    }

    @Override
    public void onStateChanged(final RadioPlayer.PlayState state, final int audioSessionId) {
        // State changed can be called from the player's thread.

        Handler h = new Handler(itsContext.getMainLooper());

        h.post(new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case Paused:
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                        break;
                    case Playing: {
                        enableMediaSession();

                        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Open audio effect control session, session id=" + audioSessionId);
                        }

                        Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
                        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
                        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
                        sendBroadcast(i);
                        break;
                    }
                    default: {
                        setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);

                        if( state != RadioPlayer.PlayState.PrePlaying) {
                            disableMediaSession();
                        }

                        if (audioSessionId > 0) {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "Close audio effect control session, session id=" + audioSessionId);
                            }

                            Intent i = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
                            i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
                            i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
                            sendBroadcast(i);
                        }

                        if (state == RadioPlayer.PlayState.Idle) {
                            stop();
                        }

                        break;
                    }
                }

                updateNotification(state);
            }
        });
    }

    @Override
    public void onPlayerError(int messageId) {
        toastOnUi(messageId);
    }

    @Override
    public void onBufferedTimeUpdate(long bufferedMs) {

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
    public void foundLiveStreamInfo(StreamLiveInfo liveInfo) {
        this.liveInfo = liveInfo;

        if (BuildConfig.DEBUG) {
            Map<String, String> rawMetadata = liveInfo.getRawMetadata();
            for (String key : rawMetadata.keySet()) {
                Log.i(TAG, "INFO:" + key + "=" + rawMetadata.get(key));
            }
        }

        sendBroadCast(PLAYER_SERVICE_META_UPDATE);
        updateNotification();
    }
}
