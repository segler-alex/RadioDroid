package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import net.programmierecke.radiodroid2.data.DataRadioStation;

import java.util.Locale;

public class ActivityPlayerInfo extends AppCompatActivity {
	ProgressDialog itsProgressLoading;
	DataRadioStation itsStation;
	String stationId;
	TextView aTextViewName;
	ImageButton buttonStop;
	ImageButton buttonAddTimeout;
	ImageButton buttonClearTimeout;
	private TextView textViewCountdown;
	private BroadcastReceiver updateUIReciver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_player_status);

		Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
		setSupportActionBar(myToolbar);

		Bundle anExtras = getIntent().getExtras();
		final String aStationID = anExtras.getString("stationid");
		stationId = aStationID;

		PlayerServiceUtil.bind(this);

		InitControls();

		IntentFilter filter = new IntentFilter();

		filter.addAction(PlayerService.PLAYER_SERVICE_TIMER_UPDATE);
		filter.addAction(PlayerService.PLAYER_SERVICE_STATUS_UPDATE);

		updateUIReciver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				UpdateOutput();
			}
		};
		registerReceiver(updateUIReciver,filter);

		itsProgressLoading = ProgressDialog.show(ActivityPlayerInfo.this, "", getString(R.string.progress_loading));
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return Utils.downloadFeed(getApplicationContext(), String.format(Locale.US, "http://www.radio-browser.info/webservice/json/stations/byid/%s", aStationID),true);
			}

			@Override
			protected void onPostExecute(String result) {
				if (!isFinishing()) {
					if (result != null) {
						DataRadioStation[] aStationList = DataRadioStation.DecodeJson(result);
						if (aStationList.length == 1) {
							itsStation = aStationList[0];
							UpdateOutput();
						}
					}
				}
				itsProgressLoading.dismiss();
				super.onPostExecute(result);
			}

		}.execute();
	}

	private void InitControls() {
		aTextViewName = (TextView) findViewById(R.id.detail_station_name_value);
		textViewCountdown = (TextView) findViewById(R.id.textViewCountdown);
		if (textViewCountdown != null){
			textViewCountdown.setText("");
		}

		buttonStop = (ImageButton) findViewById(R.id.buttonStop);
		if (buttonStop != null){
			buttonStop.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					PlayerServiceUtil.stop();
					finish();
				}
			});
		}

		/*buttonAddTimeout = (ImageButton) findViewById(R.id.buttonAddTimeout);
		if (buttonAddTimeout != null){
			buttonAddTimeout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					addTime();
				}
			});
		}*/

		buttonClearTimeout = (ImageButton) findViewById(R.id.buttonCancelCountdown);
		if (buttonClearTimeout != null){
			buttonClearTimeout.setVisibility(View.GONE);
			buttonClearTimeout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					clearTime();
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_player_info, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_set_alarm:
				addTime();
				return true;

			default:
				// If we got here, the user's action was not recognized.
				// Invoke the superclass to handle it.
				return super.onOptionsItemSelected(item);
		}
	}

	private void clearTime() {
		PlayerServiceUtil.clearTimer();
	}

	private void addTime() {
		PlayerServiceUtil.addTimer(10 * 60);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PlayerServiceUtil.unBind(this);
		if (updateUIReciver != null) {
			unregisterReceiver(updateUIReciver);
			updateUIReciver = null;
		}
	}

	private void UpdateOutput() {
		Log.w("ARR","UpdateOutput()");

		if (itsStation != null) {
			if (aTextViewName != null) {
				aTextViewName.setText(itsStation.Name);
			}
		}

		long seconds = PlayerServiceUtil.getTimerSeconds();

		if (seconds <= 0){
			buttonClearTimeout.setVisibility(View.GONE);
			textViewCountdown.setText("");
		}else{
			buttonClearTimeout.setVisibility(View.VISIBLE);
			textViewCountdown.setText(getResources().getString(R.string.sleep_timer,seconds / 60, seconds % 60));
		}

		if (!PlayerServiceUtil.isPlaying()){
			Log.i("ARR","exit..");
			finish();
		}
	}
}
