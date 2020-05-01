package net.programmierecke.radiodroid2;

import android.app.Activity;
import android.app.Application;
import android.content.*;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import net.programmierecke.radiodroid2.history.TrackHistoryRepository;
import net.programmierecke.radiodroid2.players.mpd.MPDClient;
import net.programmierecke.radiodroid2.service.PauseReason;
import net.programmierecke.radiodroid2.service.PlayerService;
import net.programmierecke.radiodroid2.service.PlayerServiceUtil;
import net.programmierecke.radiodroid2.station.DataRadioStation;
import net.programmierecke.radiodroid2.station.StationActions;
import net.programmierecke.radiodroid2.station.live.StreamLiveInfo;

public class FragmentPlayerSmall extends Fragment {
    private TrackHistoryRepository trackHistoryRepository;

    public enum Role {
        HEADER,
        PLAYER
    }

    public interface Callback {
        void onToggle();
    }

    private MPDClient mpdClient;

    private BroadcastReceiver updateUIReceiver;

    private Callback callback;

    private Role role = Role.PLAYER;

    private TextView textViewStationName;
    private TextView textViewLiveInfo;

    private TextView textViewLiveInfoBig;

    private ImageView imageViewIcon;

    private ImageButton buttonPlay;
    private ImageButton buttonMore;

    private boolean firstPlayAttempted = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_player_small, container, false);

        RadioDroidApp radioDroidApp = (RadioDroidApp) requireActivity().getApplication();
        mpdClient = radioDroidApp.getMpdClient();

        trackHistoryRepository = radioDroidApp.getTrackHistoryRepository();

        updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
//                    case PlayerService.PLAYER_SERVICE_TIMER_UPDATE: {
//                        timerUpdate();
//                    }
                    case PlayerService.PLAYER_SERVICE_STATE_CHANGE: {
                        fullUpdate();
                    }
                    case PlayerService.PLAYER_SERVICE_META_UPDATE: {
                        fullUpdate();
                    }
                    case PlayerService.PLAYER_SERVICE_BOUND: {
                        tryPlayAtStart();
                    }
                }
            }
        };

        textViewStationName = view.findViewById(R.id.textViewStationName);
        textViewLiveInfo = view.findViewById(R.id.textViewLiveInfo);
        textViewLiveInfoBig = view.findViewById(R.id.textViewLiveInfoBig);
        imageViewIcon = view.findViewById(R.id.playerRadioImage);

        buttonPlay = view.findViewById(R.id.buttonPlay);
        buttonMore = view.findViewById(R.id.buttonMore);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        requireActivity().getApplication().registerActivityLifecycleCallbacks(new LifecycleCallbacks());

        buttonPlay.setOnClickListener(v -> {
            if (PlayerServiceUtil.isPlaying()) {
                if (PlayerServiceUtil.isRecording()) {
                    PlayerServiceUtil.stopRecording();
                }

                PlayerServiceUtil.pause(PauseReason.USER);
            } else {
                playLastFromHistory();
            }
        });

        buttonMore.setOnClickListener(view -> {
            DataRadioStation station = Utils.getCurrentOrLastStation(requireContext());
            if (station == null) {
                return;
            }

            RadioDroidApp radioDroidApp = (RadioDroidApp) requireActivity().getApplication();
            FavouriteManager favouriteManager = radioDroidApp.getFavouriteManager();
            boolean isInFavorites = favouriteManager.has(station.StationUuid);

            showPlayerMenu(station, isInFavorites);
        });

        requireView().setOnClickListener(view -> {
            if (callback != null) {
                callback.onToggle();
            }
        });

        tryPlayAtStart();
        fullUpdate();
        setupStationIcon();
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();

        filter.addAction(PlayerService.PLAYER_SERVICE_STATE_CHANGE);
        filter.addAction(PlayerService.PLAYER_SERVICE_META_UPDATE);
        filter.addAction(PlayerService.PLAYER_SERVICE_BOUND);

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(updateUIReceiver, filter);

        fullUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(updateUIReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setRole(Role role) {
        this.role = role;
        fullUpdate();
    }

    private void playLastFromHistory() {
        RadioDroidApp radioDroidApp = (RadioDroidApp) requireActivity().getApplication();
        DataRadioStation station = PlayerServiceUtil.getCurrentStation();

        if (station == null) {
            HistoryManager historyManager = radioDroidApp.getHistoryManager();
            station = historyManager.getFirst();
        }

        if (station != null && !PlayerServiceUtil.isPlaying()) {
            Utils.showPlaySelection(radioDroidApp, station, getActivity().getSupportFragmentManager());
        }
    }

    private void tryPlayAtStart() {
        boolean play = false;

        if (!firstPlayAttempted && PlayerServiceUtil.isServiceBound()) {
            firstPlayAttempted = true;

            if (!PlayerServiceUtil.isPlaying()) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext());
                play = sharedPreferences.getBoolean("auto_play_on_startup", false);
            }
        }

        if (play) {
            playLastFromHistory();
        }
    }

    private void setupStationIcon() {
        boolean useCircularIcons = PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext()).getBoolean("circular_icons", false);
        if (useCircularIcons) {
            imageViewIcon.setBackgroundColor(requireContext().getResources().getColor(android.R.color.black));
        }

        ImageView transparentCircle = requireView().findViewById(R.id.transparentCircle);
        transparentCircle.setVisibility(useCircularIcons ? View.VISIBLE : View.GONE);
    }

    private void fullUpdate() {
        if (PlayerServiceUtil.isPlaying()) {
            buttonPlay.setImageResource(R.drawable.ic_pause_circle);
            buttonPlay.setContentDescription(getResources().getString(R.string.detail_pause));
        } else {
            buttonPlay.setImageResource(R.drawable.ic_play_circle);
            buttonPlay.setContentDescription(getResources().getString(R.string.detail_play));
        }

        DataRadioStation station = Utils.getCurrentOrLastStation(requireContext());

        final String stationName = station != null ? station.Name : "";

        textViewStationName.setText(stationName);

        StreamLiveInfo liveInfo = PlayerServiceUtil.getMetadataLive();
        String streamTitle = liveInfo.getTitle();
        if (!TextUtils.isEmpty(streamTitle)) {
            textViewLiveInfo.setVisibility(View.VISIBLE);
            textViewLiveInfo.setText(streamTitle);
            textViewStationName.setGravity(Gravity.BOTTOM);
        } else {
            textViewLiveInfo.setVisibility(View.GONE);
            textViewStationName.setGravity(Gravity.CENTER_VERTICAL);
        }

        textViewLiveInfoBig.setText(stationName);

        if (!Utils.shouldLoadIcons(getContext())) {
            imageViewIcon.setVisibility(View.GONE);
        } else if (station != null && station.hasIcon()) {
            imageViewIcon.setVisibility(View.VISIBLE);
            PlayerServiceUtil.getStationIcon(imageViewIcon, station.IconUrl);
        } else {
            imageViewIcon.setVisibility(View.VISIBLE);
            imageViewIcon.setImageResource(R.drawable.ic_launcher);
        }

        if (role == Role.PLAYER) {
            buttonPlay.setVisibility(View.VISIBLE);
            buttonMore.setVisibility(View.GONE);

            textViewStationName.setVisibility(View.VISIBLE);
            textViewLiveInfoBig.setVisibility(View.GONE);
        } else if (role == Role.HEADER) {
            buttonPlay.setVisibility(View.GONE);
            buttonMore.setVisibility(View.VISIBLE);

            textViewLiveInfo.setVisibility(View.GONE);
            textViewStationName.setVisibility(View.GONE);
            textViewLiveInfoBig.setVisibility(View.VISIBLE);
        }
    }

    private void showPlayerMenu(@NonNull final DataRadioStation currentStation, final boolean stationIsInFavourites) {
        final PopupMenu dropDownMenu = new PopupMenu(getContext(), buttonMore);
        dropDownMenu.getMenuInflater().inflate(R.menu.menu_player, dropDownMenu.getMenu());
        dropDownMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.action_homepage: {
                    StationActions.showWebLinks(requireActivity(), currentStation);
                    break;
                }
                case R.id.action_share: {
                    StationActions.share(requireContext(), currentStation);
                    break;
                }
                case R.id.action_set_alarm: {
                    StationActions.setAsAlarm(requireActivity(), currentStation);
                    break;
                }
                case R.id.action_delete_stream_history: {
                    trackHistoryRepository.deleteHistory();
                    break;
                }
            }

            return true;
        });

        dropDownMenu.show();
    }

    class LifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {
            Context ctx = getContext();
            if (ctx == null) {
                return;
            }

            tryPlayAtStart();
        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }
}
