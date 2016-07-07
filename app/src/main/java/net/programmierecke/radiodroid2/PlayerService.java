package net.programmierecke.radiodroid2;

import java.io.IOException;
import java.util.Map;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.interfaces.IStreamProxyEventReceiver;

public class PlayerService extends Service implements IStreamProxyEventReceiver {
	protected static final int NOTIFY_ID = 1;
	public static final String PLAYER_SERVICE_TIMER_UPDATE = "net.programmierecke.radiodroid2.timerupdate";
	public static final String PLAYER_SERVICE_STATUS_UPDATE = "net.programmierecke.radiodroid2.statusupdate";
	public static final String PLAYER_SERVICE_META_UPDATE = "net.programmierecke.radiodroid2.metaupdate";

	final String TAG = "PLAY";

	private Context itsContext;

	private String itsStationID;
	private String itsStationName;
	private String itsStationURL;
	private MediaPlayer itsMediaPlayer = null;
	private CountDownTimer timer = null;
	long seconds = 0;
	private Map<String, String> liveInfo;
	private ShoutcastInfo streamInfo;
	private StreamProxy proxy;

	PlayStatus playStatus = PlayStatus.Idle;
	private PowerManager powerManager;
	private PowerManager.WakeLock wakeLock;
	private WifiManager.WifiLock wifiLock;

	enum PlayStatus{
		Idle,
		CreateProxy,
		ClearOld,
		PrepareStream,
		PrePlaying, Playing
	}

	void sendBroadCast(String action){
		Intent local = new Intent();
		local.setAction(action);
		sendBroadcast(local);
	}

	private final IPlayerService.Stub itsBinder = new IPlayerService.Stub() {

		public void Play(String theUrl, String theName, String theID, boolean isAlarm) throws RemoteException {
			PlayerService.this.PlayUrl(theUrl, theName, theID, isAlarm);
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
			if (playStatus != PlayStatus.Idle) {
				return itsStationID;
			}
			return null;
		}

		@Override
		public String getStationName() throws RemoteException {
			return itsStationName;
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
		public boolean isPlaying() throws RemoteException {
			return playStatus != PlayStatus.Idle;
		}

		@Override
		public void startRecording() throws RemoteException {
			if (proxy != null){
				proxy.record(itsStationName);
				sendBroadCast(PLAYER_SERVICE_META_UPDATE);
			}
		}

		@Override
		public void stopRecording() throws RemoteException {
			if (proxy != null){
				proxy.stopRecord();
				sendBroadCast(PLAYER_SERVICE_META_UPDATE);
			}
		}

		@Override
		public boolean isRecording() throws RemoteException {
			if (proxy != null){
				return proxy.getOutFileName() != null;
			}
			return false;
		}

		@Override
		public String getCurrentRecordFileName() throws RemoteException {
			if (proxy != null){
				return proxy.getOutFileName();
			}
			return null;
		}

		@Override
		public long getTransferedBytes() throws RemoteException {
			if (proxy != null) {
				return proxy.getTotalBytes();
			}
			return 0;
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
				Log.w(TAG,""+seconds);

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
	}

	@Override
	public int onStartCommand (Intent intent, int flags, int startId){
		if (intent != null) {
			String action = intent.getAction();
			if (action != null) {
				if (action.equals("stop")) {
					Stop();
				}
			}
		}

		return super.onStartCommand(intent,flags,startId);
	}

	public void SendMessage(String theTitle, String theMessage, String theTicker) {
		Intent notificationIntent = new Intent(itsContext, ActivityPlayerInfo.class);
		notificationIntent.putExtra("stationid", itsStationID);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		Intent stopIntent = new Intent(itsContext, PlayerService.class);
		stopIntent.setAction("stop");
		PendingIntent pendingIntentStop = PendingIntent.getService(itsContext, 0, stopIntent, 0);

		PendingIntent contentIntent = PendingIntent.getActivity(itsContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification itsNotification = new NotificationCompat.Builder(itsContext)
				.setContentIntent(contentIntent)
				.setContentTitle(theTitle)
				.setContentText(theMessage)
				.setWhen(System.currentTimeMillis())
				.setTicker(theTicker)
				.setOngoing(true)
				.setUsesChronometer(true)
				.setSmallIcon(R.drawable.ic_play_arrow_24dp)
				.setLargeIcon((((BitmapDrawable) getResources().getDrawable(R.drawable.ic_launcher)).getBitmap()))
				.addAction(R.drawable.ic_stop_24dp,"Stop",pendingIntentStop)
				.build();

		startForeground(NOTIFY_ID, itsNotification);
	}

	@Override
	public void onDestroy() {
		Log.i(TAG,"onDestroy()");
		Stop();
	}

	public void PlayUrl(String theURL, String theName, String theID, final boolean isAlarm) {
		itsStationID = theID;
		itsStationName = theName;
		itsStationURL = theURL;
		ReplayCurrent(isAlarm);
	}

	public void ReplayCurrent(final boolean isAlarm) {
		liveInfo = null;
		streamInfo = null;
		SetPlayStatus(PlayStatus.Idle);

		if (wakeLock == null) {
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PlayerService");
		}
		if (!wakeLock.isHeld()) {
			Log.i(TAG,"acquire wakelock");
			wakeLock.acquire();
		}
		WifiManager wm = (WifiManager) itsContext.getSystemService(Context.WIFI_SERVICE);
		if (wm != null) {

			if (wifiLock == null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
					wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PlayerService");
				} else {
					wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "PlayerService");
				}
			}
			if (!wifiLock.isHeld()) {
				Log.i(TAG,"acquire wifilock");
				wifiLock.acquire();
			}
		}else{
			Log.e(TAG,"could not aquire wifi lock");
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				if (proxy != null){
					Log.i(TAG,"stop old proxy");
					proxy.stop();
					proxy = null;
				}

				SetPlayStatus(PlayStatus.CreateProxy);
				proxy = new StreamProxy(PlayerService.this, itsStationURL, PlayerService.this);
				String proxyConnection = proxy.getLocalAdress();
				Log.v(TAG, "Stream url:" + proxyConnection);
				SetPlayStatus(PlayStatus.ClearOld);
				if (itsMediaPlayer == null) {
					itsMediaPlayer = new MediaPlayer();
				}
				if (itsMediaPlayer.isPlaying()) {
					itsMediaPlayer.stop();
					itsMediaPlayer.reset();
				}
				try {
					SetPlayStatus(PlayStatus.PrepareStream);
					itsMediaPlayer.setAudioStreamType(isAlarm ? AudioManager.STREAM_ALARM : AudioManager.STREAM_MUSIC);
					itsMediaPlayer.setDataSource(proxyConnection);
					itsMediaPlayer.prepare();
					SetPlayStatus(PlayStatus.PrePlaying);
					itsMediaPlayer.start();
					SetPlayStatus(PlayStatus.Playing);
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "" + e);
					ToastOnUi(R.string.error_stream_url);
					Stop();
				} catch (IOException e) {
					Log.e(TAG, "" + e);
					ToastOnUi(R.string.error_caching_stream);
					Stop();
				} catch (Exception e) {
					Log.e(TAG, "" + e);
					ToastOnUi(R.string.error_play_stream);
					Stop();
				}
			}
		}).start();
	}

	void ToastOnUi(final int messageId){
		Handler h = new Handler(itsContext.getMainLooper());

		h.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(itsContext,itsContext.getResources().getString(messageId),Toast.LENGTH_SHORT).show();
			}
		});
	}

	void SetPlayStatus(PlayStatus status){
		playStatus = status;
		UpdateNotification();
	}

	private void UpdateNotification() {
		switch (playStatus){
			case Idle:
				break;
			case CreateProxy:
				SendMessage(itsStationName, itsContext.getResources().getString(R.string.notify_start_proxy), itsContext.getResources().getString(R.string.notify_start_proxy));
				break;
			case ClearOld:
				SendMessage(itsStationName, itsContext.getResources().getString(R.string.notify_stop_player), itsContext.getResources().getString(R.string.notify_stop_player));
				break;
			case PrepareStream:
				SendMessage(itsStationName, itsContext.getResources().getString(R.string.notify_prepare_stream), itsContext.getResources().getString(R.string.notify_prepare_stream));
				break;
			case PrePlaying:
				SendMessage(itsStationName, itsContext.getResources().getString(R.string.notify_try_play), itsContext.getResources().getString(R.string.notify_try_play));
				break;
			case Playing:
				if (liveInfo != null)
				{
					String title = liveInfo.get("StreamTitle");
					if (!TextUtils.isEmpty(title)) {
						Log.i(TAG, "update message:"+title);
						SendMessage(itsStationName, title, title);
					}else{
						SendMessage(itsStationName, itsContext.getResources().getString(R.string.notify_play), itsStationName);
					}
				}else{
					SendMessage(itsStationName, itsContext.getResources().getString(R.string.notify_play), itsStationName);
				}
				break;
		}
	}

	@Override
	public void foundShoutcastStream(ShoutcastInfo info) {
		this.streamInfo = info;
		Log.i(TAG, "Metadata offset:" + info.metadataOffset);
		Log.i(TAG, "Bitrate:" + info.bitrate);
		Log.i(TAG, "Name:" + info.audioName);
		if (info.audioName != null) {
			if (!info.audioName.trim().equals("")) {
				itsStationName = info.audioName.trim();
			}
		}
		Log.i(TAG, "Server:" + info.serverName);
		Log.i(TAG, "AudioInfo:" + info.audioInfo);
		sendBroadCast(PLAYER_SERVICE_META_UPDATE);
	}

	@Override
	public void foundLiveStreamInfo(Map<String, String> liveInfo) {
		this.liveInfo = liveInfo;
		for (String key: liveInfo.keySet())
		{
			Log.i(TAG,"INFO:"+key+"="+liveInfo.get(key));
		}
		sendBroadCast(PLAYER_SERVICE_META_UPDATE);
		UpdateNotification();
	}

	@Override
	public void streamStopped() {
		Stop();
		ReplayCurrent(false);
	}

	public void Stop() {
		Log.i(TAG,"stop()");
		if (itsMediaPlayer != null) {
			if (itsMediaPlayer.isPlaying()) {
				itsMediaPlayer.stop();
			}
			itsMediaPlayer.release();
			itsMediaPlayer = null;
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				if (proxy != null) {
					try {
						proxy.stop();
					}
					catch (Exception e){
						Log.e(TAG,"Stop() "+e);
					}
					proxy = null;
				}
			}
		}).start();
		SetPlayStatus(PlayStatus.Idle);
		liveInfo = null;
		streamInfo = null;
		clearTimer();
		stopForeground(true);
		sendBroadCast(PLAYER_SERVICE_STATUS_UPDATE);

		if (wakeLock != null) {
			if (wakeLock.isHeld()) {
				wakeLock.release();
				Log.i(TAG,"release wakelock");
			}
			wakeLock = null;
		}
		if (wifiLock != null) {
			if (wifiLock.isHeld()) {
				Log.i(TAG,"release wifilock");
				wifiLock.release();
			}
			wifiLock = null;
		}
	}
}
