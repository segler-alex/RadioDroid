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
import net.programmierecke.radiodroid2.interfaces.IAdapterRefreshable;

public class FragmentStarred extends Fragment implements IAdapterRefreshable {
    private static final String TAG = "FragmentStarred";

    private RecyclerView rvStations;

    void onStationClick(DataRadioStation theStation) {
        ActivityMain activity = (ActivityMain) getActivity();

        Utils.Play(theStation, getContext());

        HistoryManager hm = new HistoryManager(activity.getApplicationContext());
        hm.add(theStation);
    }

    public void RefreshListGui() {
        if (BuildConfig.DEBUG) Log.d(TAG, "refreshing the stations list.");

        FavouriteManager favouriteManager = new FavouriteManager(getActivity());
        ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();

        if (BuildConfig.DEBUG) Log.d(TAG, "stations count:" + favouriteManager.listStations.size());

        adapter.updateList(this, favouriteManager.listStations);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations, container, false);
        rvStations = (RecyclerView) view.findViewById(R.id.recyclerViewStations);

        ItemAdapterStation adapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station);
        adapter.setStationClickListener(new ItemAdapterStation.StationClickListener() {
            @Override
            public void onStationClick(DataRadioStation station) {
                FragmentStarred.this.onStationClick(station);
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rvStations.setAdapter(adapter);
        rvStations.setLayoutManager(llm);

        RefreshListGui();

        return view;
    }
}