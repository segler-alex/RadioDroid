package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class ActivityPlayerInfo extends AppCompatActivity {
	ProgressDialog itsProgressLoading;
	TextView aTextViewName;
	ImageButton buttonStop;
	ImageButton buttonAddTimeout;
	ImageButton buttonClearTimeout;
	private TextView textViewCountdown;
	private BroadcastReceiver updateUIReciver;
	private TextView textViewLiveInfo;
	private TextView textViewExtraInfo;
	private ImageButton buttonRecord;
	private Thread t;
	private LinearLayout layoutPlaying;
	private TextView textViewStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_player_status);

		Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
		setSupportActionBar(myToolbar);

		Bundle anExtras = getIntent().getExtras();

		PlayerServiceUtil.bind(this);

		InitControls();
		UpdateOutput();

		t = new Thread() {
			@Override
			public void run() {
				try {
					while (!isInterrupted()) {
						Thread.sleep(1000);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								UpdateOutput();
							}
						});
					}
				} catch (InterruptedException e) {
				}
			}
		};
		t.start();

		IntentFilter filter = new IntentFilter();

		filter.addAction(PlayerService.PLAYER_SERVICE_TIMER_UPDATE);
		filter.addAction(PlayerService.PLAYER_SERVICE_STATUS_UPDATE);
		filter.addAction(PlayerService.PLAYER_SERVICE_META_UPDATE);

		updateUIReciver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				UpdateOutput();
			}
		};
		registerReceiver(updateUIReciver,filter);
	}

	private void InitControls() {
		aTextViewName = (TextView) findViewById(R.id.detail_station_name_value);
		textViewCountdown = (TextView) findViewById(R.id.textViewCountdown);
		if (textViewCountdown != null){
			textViewCountdown.setText("");
		}
		textViewLiveInfo = (TextView) findViewById(R.id.textViewLiveInfo);
		textViewExtraInfo = (TextView) findViewById(R.id.textViewExtraStreamInfo);
		layoutPlaying = (LinearLayout) findViewById(R.id.LinearLayoutPlaying);
		textViewStatus = (TextView) findViewById(R.id.detail_status);

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

		buttonRecord = (ImageButton) findViewById(R.id.buttonRecord);
		if (buttonRecord != null){
			buttonRecord.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (PlayerServiceUtil.isRecording()) {
						PlayerServiceUtil.stopRecording();
					} else {
						if (Utils.verifyStoragePermissions(ActivityPlayerInfo.this)) {
							PlayerServiceUtil.startRecording();
						}
					}
				}
			});
		}

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
		if (!PlayerServiceUtil.isPlaying()){
			return super.onOptionsItemSelected(item);
		}
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
		if (t != null) {
			t.interrupt();
		}
		super.onDestroy();
		PlayerServiceUtil.unBind(this);
		if (updateUIReciver != null) {
			unregisterReceiver(updateUIReciver);
			updateUIReciver = null;
		}
	}

	private void UpdateOutput() {
		Log.i("ARR","UpdateOutput()");

		if (aTextViewName != null) {
			String stationName = PlayerServiceUtil.getStationName();
			String streamName = PlayerServiceUtil.getStreamName();
			if (!TextUtils.isEmpty(streamName)) {
				aTextViewName.setText(streamName);
			}else{
				aTextViewName.setText(stationName);
			}
		}

		long seconds = PlayerServiceUtil.getTimerSeconds();

		if (seconds <= 0){
			buttonClearTimeout.setVisibility(View.GONE);
			textViewCountdown.setText("");
			textViewCountdown.setVisibility(View.GONE);
		}else{
			buttonClearTimeout.setVisibility(View.VISIBLE);
			textViewCountdown.setText(getResources().getString(R.string.sleep_timer,seconds / 60, seconds % 60));
			textViewCountdown.setVisibility(View.VISIBLE);
		}

		Map<String,String> liveInfo = PlayerServiceUtil.getMetadataLive();
		if (liveInfo != null){
			String streamTitle = liveInfo.get("StreamTitle");
			if (!TextUtils.isEmpty(streamTitle)) {
				textViewLiveInfo.setVisibility(View.VISIBLE);
				textViewLiveInfo.setText(streamTitle);
			}else {
				textViewLiveInfo.setVisibility(View.GONE);
			}
		}else{
			textViewLiveInfo.setVisibility(View.GONE);
		}

		String strExtra = "";
		if (PlayerServiceUtil.getCurrentRecordFileName() != null){
			strExtra += getResources().getString(R.string.player_info_record_to,PlayerServiceUtil.getCurrentRecordFileName()) + "\n";
		}
		if (PlayerServiceUtil.getMetadataGenre() != null) {
			strExtra += PlayerServiceUtil.getMetadataGenre() + "\n";
		}
		if (PlayerServiceUtil.getMetadataHomepage() != null) {
			strExtra += PlayerServiceUtil.getMetadataHomepage() + "\n";
		}
		if (PlayerServiceUtil.getMetadataBitrate() > 0) {
			strExtra += "" + PlayerServiceUtil.getMetadataBitrate() + " kbps\n";
		}
		strExtra += getResources().getString(R.string.player_info_transfered,Utils.getReadableBytes(PlayerServiceUtil.getTransferedBytes()));
		textViewExtraInfo.setText(strExtra);

		if (!PlayerServiceUtil.isPlaying()){
			Log.i("ARR","exit..");
			textViewStatus.setText(getResources().getString(R.string.player_info_status)+getResources().getString(R.string.player_info_status_stopped));
			layoutPlaying.setVisibility(View.GONE);
		}else{
			textViewStatus.setText(getResources().getString(R.string.player_info_status)+getResources().getString(R.string.player_info_status_playing));
			layoutPlaying.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case Utils.REQUEST_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					PlayerServiceUtil.startRecording();
				} else {
					Toast toast = Toast.makeText(this, getResources().getString(R.string.error_record_needs_write), Toast.LENGTH_SHORT);
					toast.show();
				}
				return;
			}
		}
	}
}
