package net.programmierecke.radiodroid;

import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class RadioDroidStationDetail extends Activity {
	ProgressDialog itsProgressLoading;
	RadioStation itsStation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("", "Oncreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.station_detail);

		Bundle anExtras = getIntent().getExtras();
		final String aStationID = anExtras.getString("stationid");
		Log.v("", "Oncreate2:" + aStationID);

		Intent anIntent = new Intent(this, PlayerService.class);
		bindService(anIntent, svcConn, BIND_AUTO_CREATE);
		startService(anIntent);

		itsProgressLoading = ProgressDialog.show(RadioDroidStationDetail.this, "", "Loading...");
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				Log.v("", "doInBackground");
				return Utils.downloadFeed(String.format(Locale.US, "http://www.radio-browser.info/webservice/json/stations/byid/%s", aStationID));
			}

			@Override
			protected void onPostExecute(String result) {
				Log.v("", "onPostExecute:" + result);
				if (!isFinishing()) {
					Log.v("", "onPostExecute2");
					if (result != null) {
						RadioStation[] aStationList = Utils.DecodeJson(result);
						Log.v("", "onPostExecute3:" + aStationList.length);
						if (aStationList.length == 1) {
							Log.v("", "onPostExecute4");
							setStation(aStationList[0]);
						}
					}
				}
				itsProgressLoading.dismiss();
				super.onPostExecute(result);
			}

		}.execute();
	}

	private void setStation(RadioStation radioStation) {
		itsStation = radioStation;

		TextView aTextViewName = (TextView) findViewById(R.id.detail_station_name_value);
		aTextViewName.setText(radioStation.Name);

		TextView aTextViewCountry = (TextView) findViewById(R.id.detail_station_country_value);
		aTextViewCountry.setText(radioStation.Country);

		TextView aTextViewLanguage = (TextView) findViewById(R.id.detail_station_language_value);
		aTextViewLanguage.setText(radioStation.Language);

		TextView aTextViewTags = (TextView) findViewById(R.id.detail_station_tags_value);
		aTextViewTags.setText(radioStation.TagsAll);

		Button aButtonPlayStop = (Button) findViewById(R.id.detail_button_play_stop);

		if (itsPlayerService != null) {
			String aStationIDCurrent;
			try {
				aStationIDCurrent = itsPlayerService.getCurrentStationID();

				if (aStationIDCurrent.equals(itsStation.ID)) {
					aButtonPlayStop.setText(R.string.detail_stop);
					aButtonPlayStop.setOnClickListener(new View.OnClickListener() {
						public void onClick(View v) {
							Stop();
						}

					});
				} else {
					aButtonPlayStop.setText(R.string.detail_play);
					aButtonPlayStop.setOnClickListener(new View.OnClickListener() {
						public void onClick(View v) {
							Play();
						}
					});
				}
			} catch (RemoteException e) {
				Log.e("", "" + e);
			}
		}
	}

	private void Play() {
		if (itsPlayerService != null) {
			try {
				itsPlayerService.Play(itsStation.StreamUrl, itsStation.Name, itsStation.ID);
			} catch (RemoteException e) {
				Log.e("", "" + e);
			}
		}
	}

	private void Stop() {
		if (itsPlayerService != null) {
			try {
				itsPlayerService.Stop();
			} catch (RemoteException e) {
				Log.e("", "" + e);
			}
		}
	}

	IPlayerService itsPlayerService;
	private ServiceConnection svcConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.v("", "Service came online");
			itsPlayerService = IPlayerService.Stub.asInterface(binder);
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.v("", "Service offline");
			itsPlayerService = null;
		}
	};
}
