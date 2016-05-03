package net.programmierecke.radiodroid2;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PlayerService extends Service implements OnBufferingUpdateListener, MediaPlayer.OnInfoListener {
	protected static final int NOTIFY_ID = 1;
	public static final String PLAYER_SERVICE_TIMER_UPDATE = "net.programmierecke.radiodroid2.timerupdate";
	public static final String PLAYER_SERVICE_STATUS_UPDATE = "net.programmierecke.radiodroid2.statusupdate";
	final String TAG = "PLAY";

	private Context itsContext;

	private String itsStationID;
	private String itsStationName;
	private String itsStationURL;
	private MediaPlayer itsMediaPlayer = null;
	private CountDownTimer timer = null;
	long seconds = -1;

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
			if (itsMediaPlayer == null)
				return null;
			if (!itsMediaPlayer.isPlaying())
				return null;
			return itsStationID;
		}
	};

	private long getTimerSeconds() {
		return seconds;
	}

	private void clearTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;

			seconds = -1;

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
				Log.w("ABC",""+seconds);

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
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... stations) {
				Log.v(TAG, "Stream url:" + itsStationURL);
				SendMessage(itsStationName, "Decoding URL", "Decoding URL");
				Log.v(TAG, "Stream url decoded:" + itsStationURL);
				if (itsMediaPlayer == null) {
					itsMediaPlayer = new MediaPlayer();
					itsMediaPlayer.setOnBufferingUpdateListener(PlayerService.this);
					itsMediaPlayer.setOnInfoListener(PlayerService.this);
				}
				if (itsMediaPlayer.isPlaying()) {
					itsMediaPlayer.stop();
					itsMediaPlayer.reset();
				}
				try {
					SendMessage(itsStationName, "Preparing stream", "Preparing stream");
					itsMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
					itsMediaPlayer.setDataSource(itsStationURL);
					itsMediaPlayer.prepare();
					SendMessage(itsStationName, "Playing", "Playing '" + itsStationName + "'");
					itsMediaPlayer.start();
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "" + e);
					SendMessage(itsStationName, "Stream url problem", "Stream url problem");
					Stop();
				} catch (IOException e) {
					Log.e(TAG, "" + e);
					SendMessage(itsStationName, "Stream caching problem", "Stream caching problem");
					Stop();
				} catch (Exception e) {
					Log.e(TAG, "" + e);
					SendMessage(itsStationName, "Unable to play stream", "Unable to play stream");
					Stop();
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				Log.d(TAG, "Play task finished");
				super.onPostExecute(result);
			}

		}.execute();
	}

	public void Stop() {
		if (itsMediaPlayer != null) {
			if (itsMediaPlayer.isPlaying()) {
				itsMediaPlayer.stop();
			}
			itsMediaPlayer.release();
			itsMediaPlayer = null;
		}
		stopForeground(true);
		sendBroadCast(PLAYER_SERVICE_STATUS_UPDATE);
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.w(TAG, "Buffering:" + percent);
		// SendMessage(itsStationName, "Buffering..", "Buffering .. (" + percent +
		// "%)");
	}

	public void unbindSafely(Context appContext, ServiceConnection connection) {
		try {
			appContext.unbindService(connection);
		} catch (Exception e) {
		}
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		Log.e(TAG,"info:"+what);
		return false;
	}
}
