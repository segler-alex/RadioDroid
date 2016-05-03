package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class ActivityPlayerInfo extends AppCompatActivity {
	ProgressDialog itsProgressLoading;
	DataRadioStation itsStation;
	String stationId;

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

	@Override
	protected void onPause() {
		super.onPause();
		PlayerServiceUtil.unBind(this);
	}

	private void UpdateOutput() {
		TextView aTextViewName = (TextView) findViewById(R.id.detail_station_name_value);
		if (aTextViewName != null) {
			aTextViewName.setText(itsStation.Name);
		}
		ImageButton buttonStop = (ImageButton) findViewById(R.id.buttonStop);
		if (buttonStop != null){
			buttonStop.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					PlayerServiceUtil.stop();
					finish();
				}
			});
		}
	}
}
