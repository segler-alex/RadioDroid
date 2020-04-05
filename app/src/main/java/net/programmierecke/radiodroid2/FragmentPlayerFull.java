package net.programmierecke.radiodroid2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.squareup.picasso.Picasso;

import net.programmierecke.radiodroid2.history.TrackHistoryAdapter;
import net.programmierecke.radiodroid2.history.TrackHistoryEntry;
import net.programmierecke.radiodroid2.history.TrackHistoryRepository;
import net.programmierecke.radiodroid2.history.TrackHistoryViewModel;
import net.programmierecke.radiodroid2.recording.Recordable;
import net.programmierecke.radiodroid2.recording.RecordingsAdapter;
import net.programmierecke.radiodroid2.recording.RecordingsManager;
import net.programmierecke.radiodroid2.recording.RunningRecordingInfo;
import net.programmierecke.radiodroid2.service.PauseReason;
import net.programmierecke.radiodroid2.service.PlayerService;
import net.programmierecke.radiodroid2.service.PlayerServiceUtil;
import net.programmierecke.radiodroid2.station.DataRadioStation;
import net.programmierecke.radiodroid2.station.StationActions;
import net.programmierecke.radiodroid2.station.live.ShoutcastInfo;
import net.programmierecke.radiodroid2.station.live.StreamLiveInfo;
import net.programmierecke.radiodroid2.station.live.metadata.TrackMetadata;
import net.programmierecke.radiodroid2.station.live.metadata.TrackMetadataCallback;
import net.programmierecke.radiodroid2.station.live.metadata.TrackMetadataSearcher;
import net.programmierecke.radiodroid2.utils.RefreshHandler;
import net.programmierecke.radiodroid2.views.RecyclerAwareNestedScrollView;
import net.programmierecke.radiodroid2.views.TagsView;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Observable;

public class FragmentPlayerFull extends Fragment {
    private final String TAG = "FragmentPlayerFull";

    private final static int PERM_REQ_STORAGE_RECORD = 1001;

    /**
     * Fragment may be a part of another view which could be dragged/scrolled
     * and certain hacks may require the fragment to request them to stop
     * intercepting touch events to not end up confused.
     */
    public interface TouchInterceptListener {
        void requestDisallowInterceptTouchEvent(boolean disallow);
    }

    private TouchInterceptListener touchInterceptListener;

    private BroadcastReceiver updateUIReceiver;

    private boolean initialized = false;

    private RefreshHandler refreshHandler = new RefreshHandler();
    private TimedUpdateTask timedUpdateTask = new TimedUpdateTask(this);
    private static final int TIMED_UPDATE_INTERVAL = 1000; // 1 second

    private PlayerTrackMetadataCallback trackMetadataCallback;
    private TrackMetadataCallback.FailureType trackMetadataLastFailureType = null;
    private StreamLiveInfo lastLiveInfoForTrackMetadata = null;

    private RecordingsManager recordingsManager;
    private java.util.Observer recordingsObserver;

    private FavouriteManager favouriteManager;
    private FavouritesObserver favouritesObserver = new FavouritesObserver();

    private TrackHistoryRepository trackHistoryRepository;
    private TrackHistoryAdapter trackHistoryAdapter;

    private RecordingsAdapter recordingsAdapter;

    private boolean storagePermissionsDenied = false;

    private RecyclerAwareNestedScrollView scrollViewContent;

    private ViewPager pagerArtAndInfo;
    private ArtAndInfoPagerAdapter artAndInfoPagerAdapter;

    private TextView textViewGeneralInfo;
    private TextView textViewTimePlayed;
    private TextView textViewNetworkUsageInfo;
    private TextView textViewTimeCached;

    private Group groupRecordings;
    private ImageView imgRecordingIcon;
    private TextView textViewRecordingSize;
    private TextView textViewRecordingName;

    private ViewPager pagerHistoryAndRecordings;
    private HistoryAndRecordsPagerAdapter historyAndRecordsPagerAdapter;

    private TrackHistoryViewModel trackHistoryViewModel;

    private ImageButton btnPlay;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private ImageButton btnRecord;
    private ImageButton btnFavourite;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RadioDroidApp radioDroidApp = (RadioDroidApp) requireActivity().getApplication();

        recordingsManager = radioDroidApp.getRecordingsManager();
        recordingsObserver = (observable, o) -> updateRecordings();

        favouriteManager = radioDroidApp.getFavouriteManager();

        trackHistoryAdapter = new TrackHistoryAdapter(requireActivity());
        trackHistoryAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onItemRangeInserted(int positionStart, int itemCount) {
                final LinearLayoutManager lm = (LinearLayoutManager) historyAndRecordsPagerAdapter.recyclerViewSongHistory.getLayoutManager();
                if (lm.findFirstVisibleItemPosition() < 2) {
                    historyAndRecordsPagerAdapter.recyclerViewSongHistory.scrollToPosition(0);
                }
            }
        });

        trackHistoryRepository = radioDroidApp.getTrackHistoryRepository();

        updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
//                    case PlayerService.PLAYER_SERVICE_TIMER_UPDATE: {
//                        timerUpdate();
//                    }
                    case PlayerService.PLAYER_SERVICE_STATE_CHANGE: {

                    }
                    case PlayerService.PLAYER_SERVICE_META_UPDATE: {
                        fullUpdate();
                    }
                }
            }
        };

        View view = inflater.inflate(R.layout.layout_player_full, container, false);

        scrollViewContent = view.findViewById(R.id.scrollViewContent);

        pagerArtAndInfo = view.findViewById(R.id.pagerArtAndInfo);
        artAndInfoPagerAdapter = new ArtAndInfoPagerAdapter(requireContext(), pagerArtAndInfo);
        pagerArtAndInfo.setAdapter(artAndInfoPagerAdapter);

        /* A hack to make horizontal ViewPager play nice with vertical ScrollView
         * Credits to https://stackoverflow.com/a/16224484/1741638
         */
        pagerArtAndInfo.setOnTouchListener(new View.OnTouchListener() {
            private static final int DRAG_THRESHOLD = 30;
            private int downX;
            private int downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = (int) event.getRawX();
                        downY = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int distanceX = Math.abs((int) event.getRawX() - downX);
                        int distanceY = Math.abs((int) event.getRawY() - downY);

                        if (distanceX > distanceY && distanceX > DRAG_THRESHOLD) {
                            pagerArtAndInfo.getParent().requestDisallowInterceptTouchEvent(true);
                            scrollViewContent.getParent().requestDisallowInterceptTouchEvent(false);
                            if (touchInterceptListener != null) {
                                touchInterceptListener.requestDisallowInterceptTouchEvent(true);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        scrollViewContent.getParent().requestDisallowInterceptTouchEvent(false);
                        pagerArtAndInfo.getParent().requestDisallowInterceptTouchEvent(false);
                        if (touchInterceptListener != null) {
                            touchInterceptListener.requestDisallowInterceptTouchEvent(false);
                        }
                        break;
                }
                return false;
            }
        });

        textViewGeneralInfo = view.findViewById(R.id.textViewGeneralInfo);
        textViewTimePlayed = view.findViewById(R.id.textViewTimePlayed);
        textViewNetworkUsageInfo = view.findViewById(R.id.textViewNetworkUsageInfo);
        textViewTimeCached = view.findViewById(R.id.textViewTimeCached);

        groupRecordings = view.findViewById(R.id.group_recording_info);
        imgRecordingIcon = view.findViewById(R.id.imgRecordingIcon);
        textViewRecordingSize = view.findViewById(R.id.textViewRecordingSize);
        textViewRecordingName = view.findViewById(R.id.textViewRecordingName);

        pagerHistoryAndRecordings = view.findViewById(R.id.pagerHistoryAndRecordings);
        historyAndRecordsPagerAdapter = new HistoryAndRecordsPagerAdapter(requireContext(), pagerHistoryAndRecordings);
        pagerHistoryAndRecordings.setAdapter(historyAndRecordsPagerAdapter);

        btnPlay = view.findViewById(R.id.buttonPlay);
        btnPrev = view.findViewById(R.id.buttonPrev);
        btnNext = view.findViewById(R.id.buttonNext);
        btnRecord = view.findViewById(R.id.buttonRecord);
        btnFavourite = view.findViewById(R.id.buttonFavorite);

        historyAndRecordsPagerAdapter.recyclerViewSongHistory.setAdapter(trackHistoryAdapter);

        LinearLayoutManager llmHistory = new LinearLayoutManager(getContext());
        llmHistory.setOrientation(RecyclerView.VERTICAL);
        historyAndRecordsPagerAdapter.recyclerViewSongHistory.setLayoutManager(llmHistory);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(historyAndRecordsPagerAdapter.recyclerViewSongHistory.getContext(), llmHistory.getOrientation());
        historyAndRecordsPagerAdapter.recyclerViewSongHistory.addItemDecoration(dividerItemDecoration);

        trackHistoryViewModel = ViewModelProviders.of(this).get(TrackHistoryViewModel.class);
        trackHistoryViewModel.getAllHistoryPaged().observe(this, new Observer<PagedList<TrackHistoryEntry>>() {
            @Override
            public void onChanged(@Nullable PagedList<TrackHistoryEntry> songHistoryEntries) {
                trackHistoryAdapter.submitList(songHistoryEntries);
            }
        });

        recordingsAdapter = new RecordingsAdapter(requireContext());
        recordingsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onItemRangeInserted(int positionStart, int itemCount) {
                final LinearLayoutManager lm = (LinearLayoutManager) historyAndRecordsPagerAdapter.recyclerViewRecordings.getLayoutManager();
                if (lm.findFirstVisibleItemPosition() < 2) {
                    historyAndRecordsPagerAdapter.recyclerViewRecordings.scrollToPosition(0);
                }
            }
        });

        historyAndRecordsPagerAdapter.recyclerViewRecordings.setAdapter(recordingsAdapter);

        LinearLayoutManager llmRecordings = new LinearLayoutManager(getContext());
        llmRecordings.setOrientation(RecyclerView.VERTICAL);
        historyAndRecordsPagerAdapter.recyclerViewRecordings.setLayoutManager(llmRecordings);

        historyAndRecordsPagerAdapter.recyclerViewRecordings.addItemDecoration(dividerItemDecoration);

        // The scrollable part of the player should have the height of its parent but
        // we only can do this at the runtime.
        ViewTreeObserver viewTreeObserver = pagerHistoryAndRecordings.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(() -> {
                ViewGroup.LayoutParams layoutParams = pagerHistoryAndRecordings.getLayoutParams();
                final int newHeight = scrollViewContent.getHeight();
                if (newHeight != layoutParams.height) {
                    layoutParams.height = newHeight;
                    pagerHistoryAndRecordings.setLayoutParams(layoutParams);
                }
            });
        }

        return view;
    }

    public void init() {
        if (!initialized) {
            fullUpdate();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (PlayerServiceUtil.isPlaying()) {
                    if (PlayerServiceUtil.isRecording()) {
                        PlayerServiceUtil.stopRecording();
                        updateRunningRecording();
                    }

                    PlayerServiceUtil.pause(PauseReason.USER);
                } else {
                    playLastFromHistory();
                }

                updatePlaybackButtons(PlayerServiceUtil.isPlaying(), PlayerServiceUtil.isRecording());
            }
        });

        btnPrev.setOnClickListener(view -> PlayerServiceUtil.skipToPrevious());
        btnNext.setOnClickListener(view -> PlayerServiceUtil.skipToNext());

        btnRecord.setOnClickListener(view -> {
            if (PlayerServiceUtil.isPlaying()) {
                if (PlayerServiceUtil.isRecording()) {
                    PlayerServiceUtil.stopRecording();
                } else {
                    if (Utils.verifyStoragePermissions(FragmentPlayerFull.this, PERM_REQ_STORAGE_RECORD)) {
                        PlayerServiceUtil.startRecording();
                    }
                }

                updateRunningRecording();

                pagerHistoryAndRecordings.setCurrentItem(1, true);
            }
        });

        btnFavourite.setOnClickListener(v -> {
            DataRadioStation station = Utils.getCurrentOrLastStation(requireContext());

            if (station == null) {
                return;
            }

            if (favouriteManager.has(station.StationUuid)) {
                StationActions.removeFromFavourites(requireContext(), null, station);
            } else {
                StationActions.markAsFavourite(requireContext(), station);
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (hidden) {
            stopUpdating();
        } else {
            startUpdating();
        }

        if (touchInterceptListener != null) {
            touchInterceptListener.requestDisallowInterceptTouchEvent(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        startUpdating();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopUpdating();
    }

    public void setTouchInterceptListener(TouchInterceptListener touchInterceptListener) {
        this.touchInterceptListener = touchInterceptListener;
    }

    private void startUpdating() {
        if (!isVisible()) {
            return;
        }

        fullUpdate();

        refreshHandler.executePeriodically(timedUpdateTask, TIMED_UPDATE_INTERVAL);

        IntentFilter filter = new IntentFilter();

        filter.addAction(PlayerService.PLAYER_SERVICE_TIMER_UPDATE);
        filter.addAction(PlayerService.PLAYER_SERVICE_STATE_CHANGE);
        filter.addAction(PlayerService.PLAYER_SERVICE_META_UPDATE);

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(updateUIReceiver, filter);

        recordingsManager.getSavedRecordingsObservable().addObserver(recordingsObserver);

        favouriteManager.addObserver(favouritesObserver);
    }

    private void stopUpdating() {
        if (getView() == null) {
            return;
        }

        refreshHandler.cancel();

        if (trackMetadataCallback != null) {
            trackMetadataCallback.cancel();
        }

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(updateUIReceiver);

        recordingsManager.getSavedRecordingsObservable().deleteObserver(recordingsObserver);

        favouriteManager.deleteObserver(favouritesObserver);
    }

    public void resetScroll() {
        scrollViewContent.scrollTo(0, 0);
        historyAndRecordsPagerAdapter.recyclerViewSongHistory.scrollToPosition(0);
        historyAndRecordsPagerAdapter.recyclerViewRecordings.scrollToPosition(0);
    }

    public boolean isScrolled() {
        return scrollViewContent.getScrollY() > 0;
    }

    private void playLastFromHistory() {
        RadioDroidApp radioDroidApp = (RadioDroidApp) requireActivity().getApplication();
        DataRadioStation station = PlayerServiceUtil.getCurrentStation();

        if (station == null) {
            HistoryManager historyManager = radioDroidApp.getHistoryManager();
            station = historyManager.getFirst();
        }

        if (station != null) {
            Utils.showPlaySelection(radioDroidApp, station, getActivity().getSupportFragmentManager());
        }
    }

    private void fullUpdate() {
        DataRadioStation station = Utils.getCurrentOrLastStation(requireContext());

        if (station != null) {
            final ShoutcastInfo shoutcastInfo = PlayerServiceUtil.getShoutcastInfo();
            // TODO: add some of shoutcast info

            final StreamLiveInfo liveInfo = PlayerServiceUtil.getMetadataLive();
            String streamTitle = liveInfo.getTitle();

            if (!TextUtils.isEmpty(streamTitle)) {
                textViewGeneralInfo.setText(streamTitle);
            } else {
                textViewGeneralInfo.setText(station.Name);
            }

            Drawable flag = CountryFlagsLoader.getInstance().getFlag(requireContext(), station.CountryCode);
            if (flag != null) {
                float k = flag.getMinimumWidth() / (float) flag.getMinimumHeight();
                float viewHeight = artAndInfoPagerAdapter.textViewStationDescription.getTextSize();
                flag.setBounds(0, 0, (int) (k * viewHeight), (int) viewHeight);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                artAndInfoPagerAdapter.textViewStationDescription.setCompoundDrawablesRelative(flag, null, null, null);
            } else {
                artAndInfoPagerAdapter.textViewStationDescription.setCompoundDrawables(flag, null, null, null);
            }

            // TODO: add votes/clicks/trend

            artAndInfoPagerAdapter.textViewStationDescription.setText(station.getLongDetails(requireContext()));

            String[] tags = station.TagsAll.split(",");
            artAndInfoPagerAdapter.viewTags.setTags(Arrays.asList(tags));
            //artAndInfoPagerAdapter.viewTags.setTagSelectionCallback(tagSelectionCallback);
        }

        updateAlbumArt();
        updateRecordings();
        updatePlaybackButtons(PlayerServiceUtil.isPlaying(), PlayerServiceUtil.isRecording());
        updateFavouriteButton();

        timedUpdateTask.run();

        initialized = true;
    }

    private void updatePlaybackButtons(boolean playing, boolean recording) {
        updatePlayButton(playing);
        updateRecordButton(playing, recording);
    }

    private void updatePlayButton(boolean playing) {
        if (playing) {
            btnPlay.setImageResource(R.drawable.ic_pause_circle);
            btnPlay.setContentDescription(getResources().getString(R.string.detail_pause));
        } else {
            btnPlay.setImageResource(R.drawable.ic_play_circle);
            btnPlay.setContentDescription(getResources().getString(R.string.detail_play));
        }
    }

    private void updateRecordButton(boolean playing, boolean recording) {
        btnRecord.setEnabled(playing);

        if (recording) {
            btnRecord.setImageResource(R.drawable.ic_stop_recording);
            btnRecord.setContentDescription(getResources().getString(R.string.detail_stop));
        } else {
            btnRecord.setImageResource(R.drawable.ic_start_recording);

            if (!storagePermissionsDenied) {
                btnRecord.setContentDescription(getResources().getString(R.string.image_button_record));
            } else {
                btnRecord.setContentDescription(getResources().getString(R.string.image_button_record_request_permission));
            }
        }
    }

    private void updateRecordings() {
        recordingsAdapter.setRecordings(recordingsManager.getSavedRecordings());
        updateRunningRecording();
    }

    private void updateRunningRecording() {
        if (PlayerServiceUtil.isRecording()) {
            final Map<Recordable, RunningRecordingInfo> runningRecordings = recordingsManager.getRunningRecordings();
            final RunningRecordingInfo recordingInfo = runningRecordings.entrySet().iterator().next().getValue();

            groupRecordings.setVisibility(View.VISIBLE);
            imgRecordingIcon.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.blink_recording));
            textViewRecordingSize.setText(Utils.getReadableBytes(recordingInfo.getBytesWritten()));
            textViewRecordingName.setText(recordingInfo.getFileName());
        } else {
            groupRecordings.setVisibility(View.GONE);
            imgRecordingIcon.clearAnimation();
        }
    }

    private void updateAlbumArt() {
        DataRadioStation station = PlayerServiceUtil.getCurrentStation();
        if (station == null) {
            return;
        }

        final StreamLiveInfo liveInfo = PlayerServiceUtil.getMetadataLive();

        if (lastLiveInfoForTrackMetadata != null &&
                TextUtils.equals(lastLiveInfoForTrackMetadata.getArtist(), liveInfo.getArtist()) &&
                TextUtils.equals(lastLiveInfoForTrackMetadata.getTrack(), liveInfo.getTrack()) &&
                !TrackMetadataCallback.FailureType.RECOVERABLE.equals(trackMetadataLastFailureType)) {
            return;
        }

        if (TextUtils.isEmpty(liveInfo.getArtist()) || TextUtils.isEmpty(liveInfo.getTrack()) ||
                BuildConfig.LastFMAPIKey.isEmpty()) {
            if (station.hasIcon()) {
                // TODO: Check if we already have this station's icon loaded into image view
                Picasso.get()
                        .load(station.IconUrl)
                        .error(R.drawable.ic_launcher)
                        .into(artAndInfoPagerAdapter.imageViewArt);
            } else {
                artAndInfoPagerAdapter.imageViewArt.setImageResource(R.drawable.ic_launcher);
            }
            return;
        }

        trackMetadataLastFailureType = null;
        lastLiveInfoForTrackMetadata = liveInfo;

        if (trackMetadataCallback != null) {
            trackMetadataCallback.cancel();
        }

        final RadioDroidApp radioDroidApp = (RadioDroidApp) requireActivity().getApplication();
        TrackMetadataSearcher trackMetadataSearcher = radioDroidApp.getTrackMetadataSearcher();

        final WeakReference<FragmentPlayerFull> fragmentWeakReference = new WeakReference<>(this);
        trackHistoryRepository.getLastInsertedHistoryItem((trackHistoryEntry, dao) -> {
            if (trackHistoryEntry == null) {
                Log.e(TAG, "trackHistoryEntry is null in updateAlbumArt which should not happen.");
                return;
            }

            if (!TextUtils.isEmpty(trackHistoryEntry.artUrl)) {
                return;
            }

            FragmentPlayerFull fragment = fragmentWeakReference.get();
            if (fragment != null) {
                fragment.requireActivity().runOnUiThread(() -> {
                    if (fragment.isResumed()) {
                        fragment.trackMetadataCallback = new PlayerTrackMetadataCallback(fragmentWeakReference, trackHistoryEntry);
                        trackMetadataSearcher.fetchTrackMetadata(liveInfo.getArtist(), liveInfo.getTrack(), fragment.trackMetadataCallback);
                    }
                });
            }
        });
    }

    private void updateFavouriteButton() {
        DataRadioStation station = Utils.getCurrentOrLastStation(requireContext());

        if (station != null && favouriteManager.has(station.StationUuid)) {
            btnFavourite.setImageResource(R.drawable.ic_star_24dp);
            btnFavourite.setContentDescription(requireContext().getApplicationContext().getString(R.string.detail_unstar));
        } else {
            btnFavourite.setImageResource(R.drawable.ic_star_border_24dp);
            btnFavourite.setContentDescription(requireContext().getApplicationContext().getString(R.string.detail_star));
        }
    }

    private class FavouritesObserver implements java.util.Observer {

        @Override
        public void update(Observable o, Object arg) {
            updateFavouriteButton();
        }
    }

    private static class PlayerTrackMetadataCallback implements TrackMetadataCallback {
        private boolean canceled = false;
        private WeakReference<FragmentPlayerFull> fragmentWeakReference;
        private TrackHistoryEntry trackHistoryEntry;

        private PlayerTrackMetadataCallback(@NonNull WeakReference<FragmentPlayerFull> fragmentWeakReference, TrackHistoryEntry trackHistoryEntry) {
            this.fragmentWeakReference = fragmentWeakReference;
            this.trackHistoryEntry = trackHistoryEntry;
        }

        public void cancel() {
            canceled = true;
        }

        @Override
        public void onFailure(@NonNull FailureType failureType) {
            FragmentPlayerFull fragment = fragmentWeakReference.get();
            if (fragment != null) {
                fragment.requireActivity().runOnUiThread(() -> {
                    if (canceled) {
                        return;
                    }

                    fragment.trackMetadataLastFailureType = failureType;

                    DataRadioStation station = Utils.getCurrentOrLastStation(fragment.requireContext());

                    if (station != null && station.hasIcon()) {
                        Picasso.get()
                                .load(station.IconUrl)
                                .error(R.drawable.ic_launcher)
                                .into(fragment.artAndInfoPagerAdapter.imageViewArt);
                    } else {
                        fragment.artAndInfoPagerAdapter.imageViewArt.setImageResource(R.drawable.ic_launcher);
                    }

                    fragment.trackMetadataCallback = null;
                });
            }
        }

        @Override
        public void onSuccess(@NonNull final TrackMetadata trackMetadata) {
            FragmentPlayerFull fragment = fragmentWeakReference.get();
            if (fragment != null) {
                fragment.requireActivity().runOnUiThread(() -> {
                    if (canceled) {
                        return;
                    }

                    final List<TrackMetadata.AlbumArt> albumArts = trackMetadata.getAlbumArts();
                    if (!albumArts.isEmpty()) {
                        final String albumArtUrl = albumArts.get(0).url;

                        if (!TextUtils.isEmpty(albumArtUrl)) {
                            Picasso.get()
                                    .load(albumArtUrl)
                                    .into(fragment.artAndInfoPagerAdapter.imageViewArt);

                            if (!albumArtUrl.equals(trackHistoryEntry.stationIconUrl)) {
                                fragment.trackHistoryRepository.setTrackArtUrl(trackHistoryEntry.uid, albumArtUrl);
                            }

                            fragment.trackMetadataCallback = null;

                            return;
                        }
                    }

                    onFailure(FailureType.UNRECOVERABLE);
                });
            }
        }
    }

    private class ArtAndInfoPagerAdapter extends PagerAdapter {
        private ViewGroup layoutAlbumArt;
        private ViewGroup layoutStationInfo;

        private String[] titles;

        ImageView imageViewArt;
        TextView textViewStationDescription;
        TagsView viewTags;

        ArtAndInfoPagerAdapter(@NonNull Context context, @NonNull ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);

            layoutAlbumArt = (ViewGroup) inflater.inflate(R.layout.page_player_album_art, parent, false);
            layoutStationInfo = (ViewGroup) inflater.inflate(R.layout.page_player_station_info, parent, false);

            titles = new String[]{getResources().getString(R.string.tab_player_art), getResources().getString(R.string.tab_player_info)};

            imageViewArt = layoutAlbumArt.findViewById(R.id.imageViewArt);

            textViewStationDescription = layoutStationInfo.findViewById(R.id.textViewStationDescription);
            viewTags = layoutStationInfo.findViewById(R.id.viewTags);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            if (position == 0) {
                collection.addView(layoutAlbumArt);
                return layoutAlbumArt;
            } else {
                collection.addView(layoutStationInfo);
                return layoutStationInfo;
            }
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object view) {
            container.removeView((View) view);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }
    }

    private class HistoryAndRecordsPagerAdapter extends PagerAdapter {
        private ViewGroup layoutSongHistory;
        private ViewGroup layoutRecordings;

        private String[] titles;

        RecyclerView recyclerViewSongHistory;
        RecyclerView recyclerViewRecordings;

        HistoryAndRecordsPagerAdapter(@NonNull Context context, @NonNull ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);

            layoutSongHistory = (ViewGroup) inflater.inflate(R.layout.page_player_history, parent, false);
            layoutRecordings = (ViewGroup) inflater.inflate(R.layout.page_player_recordings, parent, false);

            titles = new String[]{getResources().getString(R.string.tab_player_history), getResources().getString(R.string.tab_player_recordings)};

            recyclerViewSongHistory = layoutSongHistory.findViewById(R.id.recyclerViewSongHistory);
            recyclerViewRecordings = layoutRecordings.findViewById(R.id.recyclerViewRecordings);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            if (position == 0) {
                collection.addView(layoutSongHistory);
                return layoutSongHistory;
            } else {
                collection.addView(layoutRecordings);
                return layoutRecordings;
            }
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object view) {
            container.removeView((View) view);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }
    }

    private static class TimedUpdateTask extends RefreshHandler.ObjectBoundRunnable<FragmentPlayerFull> {
        TimedUpdateTask(FragmentPlayerFull obj) {
            super(obj);
        }

        @Override
        protected void run(FragmentPlayerFull fragmentPlayerFull) {
            final ShoutcastInfo shoutcastInfo = PlayerServiceUtil.getShoutcastInfo();

            if (PlayerServiceUtil.isPlaying()) {
                String networkUsageInfo = Utils.getReadableBytes(PlayerServiceUtil.getTransferredBytes());
                if (shoutcastInfo != null && shoutcastInfo.bitrate > 0) {
                    networkUsageInfo += " (" + shoutcastInfo.bitrate + " kbps)";
                }

                fragmentPlayerFull.textViewNetworkUsageInfo.setText(networkUsageInfo);

                final long now = System.currentTimeMillis();
                final long startTime = PlayerServiceUtil.getLastPlayStartTime();
                long deltaSeconds = startTime > 0 ? ((now - startTime) / 1000) : 0;
                deltaSeconds = Math.max(deltaSeconds, 0);
                fragmentPlayerFull.textViewTimePlayed.setText(DateUtils.formatElapsedTime(deltaSeconds));

                fragmentPlayerFull.textViewTimeCached.setText(DateUtils.formatElapsedTime(PlayerServiceUtil.getBufferedSeconds()));

                fragmentPlayerFull.updateRunningRecording();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == PERM_REQ_STORAGE_RECORD) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                storagePermissionsDenied = false;
                PlayerServiceUtil.startRecording();
            } else {
                storagePermissionsDenied = true;
                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.error_record_needs_write), Toast.LENGTH_SHORT);
                toast.show();
            }

            updatePlaybackButtons(PlayerServiceUtil.isPlaying(), PlayerServiceUtil.isRecording());
            updateRecordings();
        }
    }
}
