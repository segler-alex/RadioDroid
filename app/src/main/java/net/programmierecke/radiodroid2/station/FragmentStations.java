package net.programmierecke.radiodroid2.station;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.FavouriteManager;
import net.programmierecke.radiodroid2.FragmentBase;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;

import java.util.ArrayList;

public class FragmentStations extends FragmentBase {
    private static final String TAG = "FragmentStations";

    private RecyclerView rvStations;
    private SwipeRefreshLayout swipeRefreshLayout;

    private SharedPreferences sharedPref;
    private FavouriteManager favouriteManager;

    void onStationClick(DataRadioStation theStation) {
        Context context = getContext();

        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        Utils.showPlaySelection(radioDroidApp, theStation, getActivity().getSupportFragmentManager());

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final Boolean autoFavorite = sharedPref.getBoolean("auto_favorite", true);
        if (autoFavorite && !favouriteManager.has(theStation.StationUuid)) {
            favouriteManager.add(theStation);
            Toast toast = Toast.makeText(context, context.getString(R.string.notify_autostarred), Toast.LENGTH_SHORT);
            toast.show();
            RefreshListGui();
        }
    }

    @Override
    protected void RefreshListGui() {
        if (rvStations == null) {
            return;
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "refreshing the stations list.");

        Context ctx = getContext();
        if (sharedPref == null) {
            sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        }

        boolean show_broken = sharedPref.getBoolean("show_broken", false);

        ArrayList<DataRadioStation> filteredStationsList = new ArrayList<>();
        DataRadioStation[] radioStations = DataRadioStation.DecodeJson(getUrlResult());

        if (BuildConfig.DEBUG) Log.d(TAG, "station count:" + radioStations.length);

        for (DataRadioStation station : radioStations) {
            if (show_broken || station.Working) {
                filteredStationsList.add(station);
            }
        }

        ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();
        if (adapter != null) {
            adapter.updateList(null, filteredStationsList);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations_remote, container, false);
        rvStations = view.findViewById(R.id.recyclerViewStations);

        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        favouriteManager = radioDroidApp.getFavouriteManager();

        ItemAdapterStation adapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station);
        adapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
            @Override
            public void onStationClick(DataRadioStation station) {
                FragmentStations.this.onStationClick(station);
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

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rvStations.setLayoutManager(llm);
        rvStations.setAdapter(adapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvStations.getContext(),
                llm.getOrientation());
        rvStations.addItemDecoration(dividerItemDecoration);

        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        if (BuildConfig.DEBUG) Log.d(TAG, "swipe to refresh.");
                        DownloadUrl(true, false);
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
    protected void DownloadFinished() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
