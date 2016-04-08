package net.programmierecke.radiodroid2;

import java.util.Locale;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RadioDroidStationDetail extends AppCompatActivity {
	ProgressDialog itsProgressLoading;
	RadioStation itsStation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("", "Oncreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_station_detail);

		Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
		setSupportActionBar(myToolbar);

		// Get a support ActionBar corresponding to this toolbar
		ActionBar ab = getSupportActionBar();

		// Enable the Up button
		ab.setDisplayHomeAsUpEnabled(true);

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

	void UpdateMenu(){
		if (m_Menu_Stop != null) {
			m_Menu_Stop.setVisible(IsPlaying());
		}
	}

	Menu m_Menu;
	MenuItem m_Menu_Stop;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		m_Menu = menu;
		getMenuInflater().inflate(R.menu.menu_station_detail, menu);
		m_Menu_Stop = m_Menu.findItem(R.id.action_stop);
		UpdateMenu();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_play:
				Play();
				return true;

			case R.id.action_stop:
				Stop();
				return true;

			case R.id.action_share:
				Share();
				return true;

			default:
				// If we got here, the user's action was not recognized.
				// Invoke the superclass to handle it.
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.v("stationdetail", "onpause");
		PlayerService thisService = new PlayerService();
		thisService.unbindSafely(this, svcConn);
	}

	private void setStation(RadioStation radioStation) {
		itsStation = radioStation;

		TextView aTextViewName = (TextView) findViewById(R.id.detail_station_name_value);
		aTextViewName.setText(radioStation.Name);

		TextView aTextViewCountry = (TextView) findViewById(R.id.detail_station_country_value);
		if (TextUtils.isEmpty(radioStation.State))
			aTextViewCountry.setText(radioStation.Country);
		else
			aTextViewCountry.setText(radioStation.Country+"/"+radioStation.State);

		TextView aTextViewLanguage = (TextView) findViewById(R.id.detail_station_language_value);
		aTextViewLanguage.setText(radioStation.Language);

		TextView aTextViewTags = (TextView) findViewById(R.id.detail_station_tags_value);
		aTextViewTags.setText(radioStation.TagsAll);

		TextView aTextViewWWW = (TextView) findViewById(R.id.detail_station_www_value);
		aTextViewWWW.setText(radioStation.HomePageUrl);

		final String aLink = itsStation.HomePageUrl;
		LinearLayout aLinLayoutWWW = (LinearLayout) findViewById(R.id.detail_station_www_clickable);
		aLinLayoutWWW.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (aLink.toLowerCase(Locale.US).startsWith("http")) {
					Intent aWWWIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(aLink));
					startActivity(aWWWIntent);
				}
			}
		});
	}

	private boolean IsPlaying(){
		if (itsPlayerService != null){
			try {
				return itsPlayerService.getCurrentStationID() != null;
			} catch (RemoteException e) {
			}
		}
		return false;
	}

	private void Share() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return Utils.downloadFeed("http://www.radio-browser.info/webservice/json/url/" + itsStation.ID);
			}

			@Override
			protected void onPostExecute(String result) {
				if (!isFinishing()) {
					String aDecodedURL = null;
					JSONObject jsonObj = null;
					JSONArray jsonArr = null;
					try {
						jsonArr = new JSONArray(result);
						jsonObj = jsonArr.getJSONObject(0);
						aDecodedURL = jsonObj.getString("url");

						Intent share = new Intent(Intent.ACTION_VIEW);
						share.setDataAndType(Uri.parse(aDecodedURL), "audio/*");
						startActivity(share);
					} catch (JSONException e) {
						Log.e("",""+e);
					}
					itsProgressLoading.dismiss();
				}
				super.onPostExecute(result);
			}
		}.execute();


	}

	private void Play() {
		if (itsPlayerService != null) {
			try {
				itsPlayerService.Play(itsStation.StreamUrl, itsStation.Name, itsStation.ID);
			} catch (RemoteException e) {
				Log.e("", "" + e);
			}
			if (m_Menu_Stop != null) {
				m_Menu_Stop.setVisible(true);
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
			if (m_Menu_Stop != null) {
				m_Menu_Stop.setVisible(false);
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
