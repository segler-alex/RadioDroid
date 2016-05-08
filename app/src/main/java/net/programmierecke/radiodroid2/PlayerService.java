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
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.interfaces.IConnectionReady;

public class PlayerService extends Service implements IConnectionReady {
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

	PlayStatus playStatus = PlayStatus.Idle;
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

		public void Play(String theUrl, String theName, String theID) throws RemoteException {
			PlayerService.this.PlayUrl(theUrl, theName, theID);
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
		Stop();
		stopForeground(true);
	}

	public void PlayUrl(String theURL, String theName, String theID) {
		itsStationID = theID;
		itsStationName = theName;
		itsStationURL = theURL;
		liveInfo = null;
		streamInfo = null;
		SetPlayStatus(PlayStatus.Idle);

		new Thread(new Runnable() {
			@Override
			public void run() {
				SetPlayStatus(PlayStatus.CreateProxy);
				StreamProxy proxy = new StreamProxy(itsStationURL, PlayerService.this);
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
					itsMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
					itsMediaPlayer.setDataSource(proxyConnection);
					itsMediaPlayer.prepare();
					SetPlayStatus(PlayStatus.PrePlaying);
					itsMediaPlayer.start();
					SetPlayStatus(PlayStatus.Playing);
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "" + e);
					Toast toast = Toast.makeText(itsContext, "Stream url problem", Toast.LENGTH_SHORT);
					toast.show();
					Stop();
				} catch (IOException e) {
					Log.e(TAG, "" + e);
					Toast toast = Toast.makeText(itsContext, "Stream caching problem", Toast.LENGTH_SHORT);
					toast.show();
					Stop();
				} catch (Exception e) {
					Log.e(TAG, "" + e);
					Toast toast = Toast.makeText(itsContext, "Unable to play stream", Toast.LENGTH_SHORT);
					toast.show();
					Stop();
				}
			}
		}).start();
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
				SendMessage(itsStationName, "Start proxy", "Start proxy");
				break;
			case ClearOld:
				SendMessage(itsStationName, "Stop old player", "Stop old player");
				break;
			case PrepareStream:
				SendMessage(itsStationName, "Preparing stream", "Preparing stream");
				break;
			case PrePlaying:
				SendMessage(itsStationName, "Try playing", "Try playing");
				break;
			case Playing:
				if (liveInfo != null)
				{
					String title = liveInfo.get("StreamTitle");
					if (!TextUtils.isEmpty(title)) {
						Log.i(TAG, "update message:"+title);
						SendMessage(itsStationName, title, title);
					}else{
						SendMessage(itsStationName, "Playing", itsStationName);
					}
				}else{
					SendMessage(itsStationName, "Playing", itsStationName);
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
			itsStationName = info.audioName;
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
			Log.i("ABC","INFO:"+key+"="+liveInfo.get(key));
		}
		sendBroadCast(PLAYER_SERVICE_META_UPDATE);
		UpdateNotification();
	}

	public void Stop() {
		if (itsMediaPlayer != null) {
			if (itsMediaPlayer.isPlaying()) {
				itsMediaPlayer.stop();
			}
			itsMediaPlayer.release();
			itsMediaPlayer = null;
		}

		SetPlayStatus(PlayStatus.Idle);
		liveInfo = null;
		streamInfo = null;
		clearTimer();
		stopForeground(true);
		sendBroadCast(PLAYER_SERVICE_STATUS_UPDATE);
	}
}
