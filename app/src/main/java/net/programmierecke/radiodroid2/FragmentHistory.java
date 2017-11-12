package net.programmierecke.radiodroid2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.programmierecke.radiodroid2.adapters.ItemAdapterStation;
import net.programmierecke.radiodroid2.data.DataRadioStation;

public class FragmentHistory extends Fragment {
    private static final String TAG = "FragmentStarred";

    private RecyclerView rvStations;

    void onStationClick(DataRadioStation theStation) {
        ActivityMain activity = (ActivityMain) getActivity();

        Utils.Play(theStation, getContext());

        HistoryManager hm = new HistoryManager(activity.getApplicationContext());
        hm.add(theStation);

        RefreshListGui();
        rvStations.smoothScrollToPosition(0);
    }

    protected void RefreshListGui() {
        if (BuildConfig.DEBUG) Log.d(TAG, "refreshing the stations list.");

        HistoryManager historyManager = new HistoryManager(getActivity());
        ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();

        if (BuildConfig.DEBUG) Log.d(TAG, "stations count:" + historyManager.listStations.size());

        adapter.updateList(null, historyManager.listStations);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ItemAdapterStation adapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station);
        adapter.setStationClickListener(new ItemAdapterStation.StationClickListener() {
            @Override
            public void onStationClick(DataRadioStation station) {
                FragmentHistory.this.onStationClick(station);
            }
        });

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations, container, false);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rvStations = (RecyclerView) view.findViewById(R.id.recyclerViewStations);
        rvStations.setAdapter(adapter);
        rvStations.setLayoutManager(llm);

        RefreshListGui();

        return view;
    }
}