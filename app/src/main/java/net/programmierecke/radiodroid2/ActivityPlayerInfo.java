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

import net.programmierecke.radiodroid2.data.StreamLiveInfo;

public class ActivityPlayerInfo extends AppCompatActivity {
	ProgressDialog itsProgressLoading;
	TextView aTextViewName;
	ImageButton buttonStop;
	ImageButton buttonPause;
	ImageButton buttonAddTimeout;
	ImageButton buttonClearTimeout;
	private TextView textViewCountdown;
	private BroadcastReceiver updateUIReciver;
	private TextView textViewLiveInfo;
	private TextView textViewExtraInfo;
	private TextView textViewRecordingInfo;
	private TextView textViewTransferredbytes;
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
		textViewRecordingInfo = (TextView) findViewById(R.id.textViewRecordingInfo);
		textViewTransferredbytes = (TextView) findViewById(R.id.textViewTransferredBytes);
		layoutPlaying = (LinearLayout) findViewById(R.id.LinearLayoutPlaying);
		textViewStatus = (TextView) findViewById(R.id.detail_status);

		buttonPause = (ImageButton) findViewById(R.id.buttonPause);
		if (buttonPause != null){
			buttonPause.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (PlayerServiceUtil.isPlaying()) {
						textViewStatus.setText(getResources().getString(R.string.player_info_status)+getResources().getString(R.string.player_info_status_stopped));
						buttonPause.setImageResource(R.drawable.mr_media_play_light);
						PlayerServiceUtil.pause();
						if (PlayerServiceUtil.isRecording()) {
							buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_green_50dp);
							String recordingInfo = getResources().getString(R.string.player_info_recorded_to, PlayerServiceUtil.getCurrentRecordFileName());
							textViewRecordingInfo.setText(recordingInfo);
							PlayerServiceUtil.stopRecording();
						}
					} else {
						buttonPause.setImageResource(R.drawable.mr_media_pause_light);
						PlayerServiceUtil.resume();
					}
				}
			});
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

		buttonRecord = (ImageButton) findViewById(R.id.buttonRecord);
		if (buttonRecord != null){
			buttonRecord.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (PlayerServiceUtil.isRecording()) {
						buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_green_50dp);
						String recordingInfo = getResources().getString(R.string.player_info_recorded_to,PlayerServiceUtil.getCurrentRecordFileName());
						textViewRecordingInfo.setText(recordingInfo);
						PlayerServiceUtil.stopRecording();
					} else {
						PlayerServiceUtil.resume();
						if (Utils.verifyStoragePermissions(ActivityPlayerInfo.this)) {
							buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_red_50dp);
							PlayerServiceUtil.startRecording();
							if (PlayerServiceUtil.getCurrentRecordFileName() != null && PlayerServiceUtil.isRecording()){
								String recordingInfo = getResources().getString(R.string.player_info_recording_to,PlayerServiceUtil.getCurrentRecordFileName());
								textViewRecordingInfo.setText(recordingInfo);
							}
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

		buttonPause = (ImageButton) findViewById(R.id.buttonPause);
		if (PlayerServiceUtil.isPlaying()) {
			buttonPause.setImageResource(R.drawable.mr_media_pause_light);
		} else {
			buttonPause.setImageResource(R.drawable.mr_media_play_light);
		}

		buttonRecord = (ImageButton) findViewById(R.id.buttonRecord);
		if (PlayerServiceUtil.isRecording()) {
			buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_red_50dp);
		} else if (android.os.Environment.getExternalStorageDirectory().canWrite()) {
			buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_green_50dp);
		} else {
			buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_black_50dp);
		}

		if(BuildConfig.DEBUG) { Log.d("ARR","UpdateOutput()"); }

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

        StreamLiveInfo liveInfo = PlayerServiceUtil.getMetadataLive();
        String streamTitle = liveInfo.getTitle();
        if (!TextUtils.isEmpty(streamTitle)) {
            textViewLiveInfo.setVisibility(View.VISIBLE);
            textViewLiveInfo.setText(streamTitle);
        } else {
            textViewLiveInfo.setVisibility(View.GONE);
        }

		if (PlayerServiceUtil.getCurrentRecordFileName() != null && PlayerServiceUtil.isRecording()){
			String recordingInfo = getResources().getString(R.string.player_info_recording_to,PlayerServiceUtil.getCurrentRecordFileName());
			textViewRecordingInfo.setText(recordingInfo);
		}

		String strExtra = "";
		if (PlayerServiceUtil.getIsHls()){
			strExtra += "HLS-Stream\n";
		}
		if (PlayerServiceUtil.getMetadataGenre() != null) {
			strExtra += PlayerServiceUtil.getMetadataGenre() + "\n";
		}
		if (PlayerServiceUtil.getMetadataHomepage() != null) {
			strExtra += PlayerServiceUtil.getMetadataHomepage();
		}
		textViewExtraInfo.setText(strExtra);

		String byteInfo = getResources().getString(R.string.player_info_transferred,Utils.getReadableBytes(PlayerServiceUtil.getTransferredBytes()));
		if (PlayerServiceUtil.getMetadataBitrate() > 0) {
			byteInfo += " (" + PlayerServiceUtil.getMetadataBitrate() + " kbps)";
		}
		if (PlayerServiceUtil.isPlaying() || PlayerServiceUtil.isRecording()) {
			textViewTransferredbytes.setText(byteInfo);
		}

		if (!PlayerServiceUtil.isPlaying() && !PlayerServiceUtil.isRecording()){
			if(BuildConfig.DEBUG) { Log.d("ARR","exit.."); }
			textViewStatus.setText(getResources().getString(R.string.player_info_status)+getResources().getString(R.string.player_info_status_stopped));
			layoutPlaying.setVisibility(View.VISIBLE);
		} else {
				if (PlayerServiceUtil.isPlaying() && !PlayerServiceUtil.isRecording()) {
					textViewStatus.setText(getResources().getString(R.string.player_info_status)+getResources().getString(R.string.player_info_status_playing));
					layoutPlaying.setVisibility(View.VISIBLE);
				} else {
					textViewStatus.setText(getResources().getString(R.string.player_info_status)+getResources().getString(R.string.player_info_status_playing_and_recording));
					layoutPlaying.setVisibility(View.VISIBLE);
				}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {

		buttonRecord = (ImageButton) findViewById(R.id.buttonRecord);
		switch (requestCode) {
			case Utils.REQUEST_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					PlayerServiceUtil.startRecording();
					buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_red_50dp);
				} else {
					buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_black_50dp);
					Toast toast = Toast.makeText(this, getResources().getString(R.string.error_record_needs_write), Toast.LENGTH_SHORT);
					toast.show();
				}
				return;
			}
		}
	}
}
