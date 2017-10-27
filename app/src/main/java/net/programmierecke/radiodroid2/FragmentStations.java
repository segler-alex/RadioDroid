package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.programmierecke.radiodroid2.adapters.ItemAdapterStation;
import net.programmierecke.radiodroid2.data.DataRadioStation;

import java.util.ArrayList;

public class FragmentStations extends FragmentBase {
    private static final String TAG = "FragmentStations";

    private RecyclerView rvStations;
    private DataRadioStation[] radioStations = new DataRadioStation[0];
    private SwipeRefreshLayout swipeRefreshLayout;
    private SharedPreferences sharedPref;

    void onStationClick(DataRadioStation theStation) {
        ActivityMain a = (ActivityMain) getActivity();

        Utils.Play(theStation, getContext());

        HistoryManager hm = new HistoryManager(a.getApplicationContext());
        hm.add(theStation);
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
        radioStations = DataRadioStation.DecodeJson(getUrlResult());

        if (BuildConfig.DEBUG) Log.d(TAG, "station count:" + radioStations.length);

        for (DataRadioStation station : radioStations) {
            if (show_broken || station.Working) {
                filteredStationsList.add(station);
            }
        }

        ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();
        adapter.updateList(null, filteredStationsList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations_remote, container, false);
        rvStations = (RecyclerView) view.findViewById(R.id.recyclerViewStations);

        ItemAdapterStation adapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station);
        adapter.setStationClickListener(new ItemAdapterStation.StationClickListener() {
            @Override
            public void onStationClick(DataRadioStation station) {
                FragmentStations.this.onStationClick(station);
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rvStations.setLayoutManager(llm);
        rvStations.setAdapter(adapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvStations.getContext(),
                llm.getOrientation());
        rvStations.addItemDecoration(dividerItemDecoration);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
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
    protected void DownloadFinished() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}