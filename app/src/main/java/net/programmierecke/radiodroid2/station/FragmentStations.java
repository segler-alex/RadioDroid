package net.programmierecke.radiodroid2.station;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.FavouriteManager;
import net.programmierecke.radiodroid2.FragmentBase;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.interfaces.IFragmentSearchable;
import net.programmierecke.radiodroid2.utils.CustomFilter;

import java.util.ArrayList;
import java.util.List;

public class FragmentStations extends FragmentBase implements IFragmentSearchable {
    private static final String TAG = "FragmentStations";

    public static final String KEY_SEARCH_ENABLED = "SEARCH_ENABLED";

    private RecyclerView rvStations;
    private ViewGroup layoutError;
    private MaterialButton btnRetry;
    private SwipeRefreshLayout swipeRefreshLayout;

    private SharedPreferences sharedPref;
    private FavouriteManager favouriteManager;

    private boolean searchEnabled = false;

    private CustomFilter stationsFilter;
    private String lastQuery = "";

    void onStationClick(DataRadioStation theStation, int pos) {
        Context context = getContext();

        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        Utils.showPlaySelection(radioDroidApp, theStation, getActivity().getSupportFragmentManager());

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final Boolean autoFavorite = sharedPref.getBoolean("auto_favorite", true);
        if (autoFavorite && !favouriteManager.has(theStation.StationUuid)) {
            favouriteManager.add(theStation);
            Toast toast = Toast.makeText(context, context.getString(R.string.notify_autostarred), Toast.LENGTH_SHORT);
            toast.show();

            rvStations.getAdapter().notifyItemChanged(pos);
        }
    }

    @Override
    protected void RefreshListGui() {
        if (rvStations == null || !hasUrl()) {
            return;
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "refreshing the stations list.");

        Context ctx = getContext();
        if (sharedPref == null) {
            sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        }

        boolean show_broken = sharedPref.getBoolean("show_broken", false);

        ArrayList<DataRadioStation> filteredStationsList = new ArrayList<>();
        List<DataRadioStation> radioStations = DataRadioStation.DecodeJson(getUrlResult());

        if (BuildConfig.DEBUG) Log.d(TAG, "station count:" + radioStations.size());

        for (DataRadioStation station : radioStations) {
            if (show_broken || station.Working) {
                filteredStationsList.add(station);
            }
        }

        ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();
        if (adapter != null) {
            adapter.updateList(null, filteredStationsList);
            if (searchEnabled) {
                stationsFilter.filter("");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        if (bundle != null) {
            searchEnabled = bundle.getBoolean(KEY_SEARCH_ENABLED, false);
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations_remote, container, false);
        rvStations = (RecyclerView) view.findViewById(R.id.recyclerViewStations);
        layoutError = view.findViewById(R.id.layoutError);
        btnRetry = view.findViewById(R.id.btnRefresh);

        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        favouriteManager = radioDroidApp.getFavouriteManager();

        ItemAdapterStation adapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station, StationsFilter.FilterType.GLOBAL);
        adapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
            @Override
            public void onStationClick(DataRadioStation station, int pos) {
                FragmentStations.this.onStationClick(station, pos);
            }

            @Override
            public void onStationSwiped(DataRadioStation station) {
            }

            @Override
            public void onStationMoved(int from, int to) {
            }

            @Override
            public void onStationMoveFinished() {
            }
        });

        if (searchEnabled) {
            stationsFilter = adapter.getFilter();

            stationsFilter.setDelayer(new CustomFilter.Delayer() {
                private int previousLength = 0;

                public long getPostingDelay(CharSequence constraint) {
                    if (constraint == null) {
                        return 0;
                    }

                    long delay = 0;
                    if (constraint.length() < previousLength) {
                        delay = 500;
                    }
                    previousLength = constraint.length();

                    return delay;
                }
            });

            adapter.setFilterListener(searchStatus -> {
                layoutError.setVisibility(searchStatus == StationsFilter.SearchStatus.ERROR ? View.VISIBLE : View.GONE);
                getContext().sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));
                swipeRefreshLayout.setRefreshing(false);
            });

            btnRetry.setOnClickListener(v -> Search(lastQuery));
        }

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rvStations.setLayoutManager(llm);
        rvStations.setAdapter(adapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvStations.getContext(),
                llm.getOrientation());
        rvStations.addItemDecoration(dividerItemDecoration);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(
                () -> {
                    if (hasUrl()) {
                        DownloadUrl(true, false);
                    } else if (searchEnabled) {
                        Search(lastQuery);
                    }
                }
        );

        RefreshListGui();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rvStations.setAdapter(null);
    }

    @Override
    public void Search(String query) {
        Log.d("STATIONS", "query = "+query);
        if (rvStations != null && searchEnabled) {
            if (!TextUtils.isEmpty(query)) {
                getContext().sendBroadcast(new Intent(ActivityMain.ACTION_SHOW_LOADING));
            }

            lastQuery = query;
            stationsFilter.filter(query);
        }
    }

    @Override
    protected void DownloadFinished() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}