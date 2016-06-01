package net.programmierecke.radiodroid2;

import java.util.Locale;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import net.programmierecke.radiodroid2.data.DataRadioStation;

public class ActivityRadioStationDetail extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener {
	private ProgressDialog itsProgressLoading;
	private DataRadioStation itsStation;
	private MenuItem m_Menu_Star;
	private MenuItem m_Menu_UnStar;
	private Menu m_Menu;
	private String stationId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_station_detail);

		Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
		setSupportActionBar(myToolbar);

		Bundle anExtras = getIntent().getExtras();
		final String aStationID = anExtras.getString("stationid");
		stationId = aStationID;

		PlayerServiceUtil.bind(this);

		UpdateMenu();

		itsProgressLoading = ProgressDialog.show(ActivityRadioStationDetail.this, "", getResources().getText(R.string.progress_loading));
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return Utils.downloadFeed(getApplicationContext(), String.format(Locale.US, "http://www.radio-browser.info/webservice/json/stations/byid/%s", aStationID),true,null);
			}

			@Override
			protected void onPostExecute(String result) {
				if (!isFinishing()) {
					if (result != null) {
						DataRadioStation[] aStationList = DataRadioStation.DecodeJson(result);
						if (aStationList.length == 1) {
							setStation(aStationList[0]);
						}
					}
				}
				itsProgressLoading.dismiss();
				super.onPostExecute(result);
			}

		}.execute();
	}

	void UpdateMenu() {
		FavouriteManager fm = new FavouriteManager(getApplicationContext());

		if (stationId != null) {
			if (m_Menu_Star != null) {
				m_Menu_Star.setVisible(!fm.has(stationId));
			}
			if (m_Menu_UnStar != null) {
				m_Menu_UnStar.setVisible(fm.has(stationId));
			}
		} else {
			if (m_Menu_Star != null) {
				m_Menu_Star.setVisible(false);
				m_Menu_UnStar.setVisible(false);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		m_Menu = menu;
		getMenuInflater().inflate(R.menu.menu_station_detail, menu);
		m_Menu_Star = m_Menu.findItem(R.id.action_star);
		m_Menu_UnStar = m_Menu.findItem(R.id.action_unstar);
		UpdateMenu();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_play:
				Utils.Play(itsStation,this,false);
				return true;

			case R.id.action_share:
				Utils.Play(itsStation,this,true);
				return true;

			case R.id.action_star:
				Star();
				return true;

			case R.id.action_unstar:
				UnStar();
				return true;

			case R.id.action_set_alarm:
				setAsAlarm();
				return true;

			default:
				// If we got here, the user's action was not recognized.
				// Invoke the superclass to handle it.
				return super.onOptionsItemSelected(item);
		}
	}

	private void UnStar() {
		if (itsStation != null) {
			FavouriteManager fm = new FavouriteManager(getApplicationContext());
			fm.remove(itsStation.ID);
			UpdateMenu();
		}else{
			Log.e("ABC","empty station info");
		}
	}

	private void Star() {
		if (itsStation != null) {
			FavouriteManager fm = new FavouriteManager(getApplicationContext());
			fm.add(itsStation);
			UpdateMenu();
		}else{
			Log.e("ABC","empty station info");
		}
	}

	void setAsAlarm(){
		Log.w("DETAIL","setAsAlarm() 1");
		if (itsStation != null) {
			Log.w("DETAIL","setAsAlarm() 2");
			TimePickerFragment newFragment = new TimePickerFragment();
			newFragment.setCallback(this);
			newFragment.show(getSupportFragmentManager(), "timePicker");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		PlayerServiceUtil.unBind(this);
	}

	private void setStation(DataRadioStation dataRadioStation) {
		itsStation = dataRadioStation;

		TextView aTextViewName = (TextView) findViewById(R.id.detail_station_name_value);
		if (aTextViewName != null) {
			aTextViewName.setText(dataRadioStation.Name);
		}

		TextView aTextViewCountry = (TextView) findViewById(R.id.detail_station_country_value);
		if (aTextViewCountry != null) {
			if (TextUtils.isEmpty(dataRadioStation.State)) {
				aTextViewCountry.setText(dataRadioStation.Country);
			} else {
				aTextViewCountry.setText(dataRadioStation.Country + "/" + dataRadioStation.State);
			}
		}

		TextView aTextViewLanguage = (TextView) findViewById(R.id.detail_station_language_value);
		if (aTextViewLanguage != null) {
			aTextViewLanguage.setText(dataRadioStation.Language);
		}

		TextView aTextViewTags = (TextView) findViewById(R.id.detail_station_tags_value);
		if (aTextViewTags != null) {
			aTextViewTags.setText(dataRadioStation.TagsAll);
		}

		TextView aTextViewWWW = (TextView) findViewById(R.id.detail_station_www_value);
		if (aTextViewWWW != null) {
			aTextViewWWW.setText(dataRadioStation.HomePageUrl);
		}

		final String aLink = itsStation.HomePageUrl;
		LinearLayout aLinLayoutWWW = (LinearLayout) findViewById(R.id.detail_station_www_clickable);
		if (aLinLayoutWWW != null) {
			aLinLayoutWWW.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if (aLink.toLowerCase(Locale.US).startsWith("http")) {
						Intent aWWWIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(aLink));
						startActivity(aWWWIntent);
					}
				}
			});
		}

		UpdateMenu();
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		Log.w("DETAIL","onTimeSet() "+hourOfDay);
		RadioAlarmManager ram = new RadioAlarmManager(getApplicationContext(),null);
		ram.add(itsStation,hourOfDay,minute);
	}
}
