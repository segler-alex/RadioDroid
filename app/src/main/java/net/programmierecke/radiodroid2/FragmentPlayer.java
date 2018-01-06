package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;

import net.programmierecke.radiodroid2.data.DataRadioStation;
import net.programmierecke.radiodroid2.data.StreamLiveInfo;

public class FragmentPlayer extends Fragment {
	ProgressDialog itsProgressLoading;
	TextView aTextViewName;
	ImageButton buttonPause;
	private BroadcastReceiver updateUIReciver;
	private TextView textViewLiveInfo;
	private TextView textViewExtraInfo;
	private TextView textViewRecordingInfo;
	private TextView textViewTransferredbytes;
	private ImageButton buttonRecord;
	private ImageView imageViewIcon;
	private Thread t;
	private RelativeLayout layoutPlaying;
	private RelativeLayout layoutRecording;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.layout_player_status, container, false);

        PlayerServiceUtil.bind(getContext());

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
        getContext().registerReceiver(updateUIReciver,filter);

		return view;
	}

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

		InitControls();
		SetInfoFromHistory(false);
		UpdateOutput();
		setupIcon();

		t = new Thread() {
			@Override
			public void run() {
				try {
					while (!isInterrupted()) {
						Thread.sleep(1000);
						getActivity().runOnUiThread(new Runnable() {
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
	}

	private void InitControls() {
		aTextViewName = (TextView) getActivity().findViewById(R.id.detail_station_name_value);
		textViewLiveInfo = (TextView) getActivity().findViewById(R.id.textViewLiveInfo);
		textViewExtraInfo = (TextView) getActivity().findViewById(R.id.textViewExtraStreamInfo);
		textViewRecordingInfo = (TextView) getActivity().findViewById(R.id.textViewRecordingInfo);
		textViewTransferredbytes = (TextView) getActivity().findViewById(R.id.textViewTransferredBytes);
		layoutPlaying = (RelativeLayout) getActivity().findViewById(R.id.RelativeLayout1);
        layoutRecording = (RelativeLayout) getActivity().findViewById(R.id.RelativeLayout2);
		imageViewIcon = (ImageView) getActivity().findViewById(R.id.playerRadioImage);

		buttonPause = (ImageButton) getActivity().findViewById(R.id.buttonPause);
		if (buttonPause != null){
			buttonPause.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (PlayerServiceUtil.isPlaying() || MPDClient.isPlaying) {
						buttonPause.setImageResource(R.drawable.ic_play_circle);
						if (PlayerServiceUtil.isRecording()) {
							buttonRecord.setImageResource(R.drawable.ic_start_recording);
							String recordingInfo = getResources().getString(R.string.player_info_recorded_to, PlayerServiceUtil.getCurrentRecordFileName());
							textViewRecordingInfo.setText(recordingInfo);
							PlayerServiceUtil.stopRecording();
							layoutPlaying.setVisibility(View.GONE);
						}
						if(PlayerServiceUtil.isPlaying())
							PlayerServiceUtil.stop();
						// Don't stop MPD playback when a user is listening in the app
						else if(MPDClient.isPlaying)
							MPDClient.Stop(getContext());
					} else {
						buttonPause.setImageResource(R.drawable.ic_pause_circle);
						SetInfoFromHistory(true);
					}
				}
			});
		}

		buttonRecord = (ImageButton) getActivity().findViewById(R.id.buttonRecord);
		if (buttonRecord != null){
			buttonRecord.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (PlayerServiceUtil.isRecording()) {
						buttonRecord.setImageResource(R.drawable.ic_start_recording);
						String recordingInfo = getResources().getString(R.string.player_info_recorded_to,PlayerServiceUtil.getCurrentRecordFileName());
						textViewRecordingInfo.setText(recordingInfo);
						PlayerServiceUtil.stopRecording();
					} else if(PlayerServiceUtil.isPlaying()) {
						if (Utils.verifyStoragePermissions(getActivity())) {
							buttonRecord.setImageResource(R.drawable.ic_stop_recording);
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

		View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentVisibility = layoutPlaying.getVisibility();
                layoutPlaying.setVisibility(currentVisibility == View.GONE ? View.VISIBLE : View.GONE);
            }
        };

        layoutPlaying.setOnClickListener(onClickListener);
        layoutRecording.setOnClickListener(onClickListener);
	}

	private void SetInfoFromHistory(boolean startPlaying) {
        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        HistoryManager historyManager = radioDroidApp.getHistoryManager();
        DataRadioStation[] history = historyManager.getList();

        if(history.length > 0) {
            DataRadioStation lastStation = history[0];
            if(startPlaying)
                Utils.Play(lastStation, getContext());
            else {
                aTextViewName.setText(lastStation.Name);

                if (!Utils.shouldLoadIcons(getContext()))
                    imageViewIcon.setVisibility(View.GONE);
                else
                    PlayerServiceUtil.getStationIcon(imageViewIcon, lastStation.IconUrl);
            }
        }
    }

    private void setupIcon() {
        boolean useCircularIcons = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext()).getBoolean("circular_icons", false);
        if(useCircularIcons) {
            imageViewIcon.setBackgroundColor(getContext().getResources().getColor(android.R.color.black));
            ImageView transparentCircle = (ImageView) getView().findViewById(R.id.transparentCircle);
            transparentCircle.setVisibility(View.VISIBLE);
        }
    }

	@Override
	public void onDestroy() {
		if (t != null) {
			t.interrupt();
		}
		super.onDestroy();
		PlayerServiceUtil.unBind(getContext());
		if (updateUIReciver != null) {
			getContext().unregisterReceiver(updateUIReciver);
			updateUIReciver = null;
		}
	}

	private void UpdateOutput() {
	if(getView() == null || PlayerServiceUtil.getStationName() == null) return;

		buttonPause = (ImageButton) getActivity().findViewById(R.id.buttonPause);
		if (PlayerServiceUtil.isPlaying() || MPDClient.isPlaying) {
			buttonPause.setImageResource(R.drawable.ic_pause_circle);
		} else {
			buttonPause.setImageResource(R.drawable.ic_play_circle);
		}

		buttonRecord = (ImageButton) getActivity().findViewById(R.id.buttonRecord);
		if (PlayerServiceUtil.isRecording()) {
			buttonRecord.setImageResource(R.drawable.ic_stop_recording);
		} else if (android.os.Environment.getExternalStorageDirectory().canWrite()) {
			buttonRecord.setImageResource(R.drawable.ic_start_recording);
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

        StreamLiveInfo liveInfo = PlayerServiceUtil.getMetadataLive();
        String streamTitle = liveInfo.getTitle();
        if (!TextUtils.isEmpty(streamTitle)) {
            textViewLiveInfo.setVisibility(View.VISIBLE);
            textViewLiveInfo.setText(streamTitle);
            aTextViewName.setGravity(Gravity.BOTTOM);
        } else {
            textViewLiveInfo.setVisibility(View.GONE);
            aTextViewName.setGravity(Gravity.CENTER_VERTICAL);
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

		String byteInfo = Utils.getReadableBytes(PlayerServiceUtil.getTransferredBytes());
		if (PlayerServiceUtil.getMetadataBitrate() > 0) {
			byteInfo += " (" + PlayerServiceUtil.getMetadataBitrate() + " kbps)";
		}

		if (PlayerServiceUtil.isPlaying() || PlayerServiceUtil.isRecording()) {
			textViewTransferredbytes.setText(byteInfo);
		}

			if (!Utils.shouldLoadIcons(getContext())) {
                imageViewIcon.setVisibility(View.GONE);
			} else {
                PlayerServiceUtil.getStationIcon(imageViewIcon, null);
			}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {

		buttonRecord = (ImageButton) getActivity().findViewById(R.id.buttonRecord);
		switch (requestCode) {
			case Utils.REQUEST_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					PlayerServiceUtil.startRecording();
					buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_red_50dp);
				} else {
					buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_black_50dp);
					Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.error_record_needs_write), Toast.LENGTH_SHORT);
					toast.show();
				}
				return;
			}
		}
	}
}
