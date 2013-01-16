package net.programmierecke.radiodroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PlayerService extends Service {
	protected static final int NOTIFY_ID = 1;
	final String TAG = "PlayerService";
	MediaPlayer itsMediaPlayer = null;

	private final IPlayerService.Stub itsBinder = new IPlayerService.Stub() {

		public void Play(String theUrl, String theName, String theID) throws RemoteException {
			PlayerService.this.PlayUrl(theUrl, theName, theID);
		}

		public void Stop() throws RemoteException {
			PlayerService.this.Stop();
		}
	};

	@Override
	public IBinder onBind(Intent arg0) {
		return itsBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		itsContext = this;
	}

	public void SendMessage(String theTitle, String theMessage) {
		Intent notificationIntent = new Intent(itsContext, MainActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(itsContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification itsNotification = new NotificationCompat.Builder(itsContext).setContentIntent(contentIntent).setContentTitle(theTitle)
				.setContentText(theMessage).setWhen(System.currentTimeMillis()).setUsesChronometer(true).setSmallIcon(R.drawable.play).build();

		startForeground(NOTIFY_ID, itsNotification);
	}

	@Override
	public void onDestroy() {
		Stop();
		stopForeground(true);
	}

	Context itsContext;

	public void PlayUrl(String theURL, String theName, String theID) {
		new AsyncTask<String, Void, Void>() {
			@Override
			protected Void doInBackground(String... stations) {

				if (stations.length != 1)
					return null;

				String aStation = stations[0];

				Log.v(TAG, "Stream url:" + aStation);
				SendMessage(aStation, "Decoding");
				String aDecodedURL = DecodeURL(aStation);

				Log.v(TAG, "Stream url decoded:" + aDecodedURL);
				if (itsMediaPlayer == null)
					itsMediaPlayer = new MediaPlayer();
				if (itsMediaPlayer.isPlaying()) {
					itsMediaPlayer.stop();
					itsMediaPlayer.reset();
				}
				try {
					SendMessage(aStation, "Preparing");
					itsMediaPlayer.setDataSource(aDecodedURL);
					itsMediaPlayer.prepare();
					SendMessage(aStation, "Playing");
					itsMediaPlayer.start();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				Log.d(TAG, "prepare ok");

				super.onPostExecute(result);
			}

		}.execute(theURL);
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
	}

	public String downloadFeed(String theURI) {
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(theURI);
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append('\n');
				}
			} else {
				Log.e(TAG, "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, "" + e);
		} catch (IOException e) {
			Log.e(TAG, "" + e);
		}
		return builder.toString();
	}

	String DecodeURL(String theUrl) {
		try {
			URL anUrl = new URL(theUrl);
			String aFileName = anUrl.getFile();
			if (aFileName.endsWith(".pls")) {
				Log.v(TAG, "Found PLS file");
				String theFile = downloadFeed(theUrl);
				BufferedReader aReader = new BufferedReader(new StringReader(theFile));
				String str;
				while ((str = aReader.readLine()) != null) {
					Log.v(TAG, " -> " + str);
					if (str.substring(0, 4).equals("File")) {
						int anIndex = str.indexOf('=');
						if (anIndex >= 0) {
							return str.substring(anIndex + 1);
						}
					}
				}
			} else if (aFileName.endsWith(".m3u")) {
				Log.v(TAG, "Found M3U file");
				String theFile = downloadFeed(theUrl);
				BufferedReader aReader = new BufferedReader(new StringReader(theFile));
				String str;
				while ((str = aReader.readLine()) != null) {
					Log.v(TAG, " -> " + str);
					if (!str.substring(0, 1).equals("#")) {
						return str.trim();
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "" + e);
		}
		return theUrl;
	}
}
