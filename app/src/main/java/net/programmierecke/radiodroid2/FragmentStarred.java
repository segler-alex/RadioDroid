package net.programmierecke.radiodroid2;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
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

    private FavouriteManager favouriteManager;
    private HistoryManager historyManager;

    void onStationClick(DataRadioStation theStation) {
        Utils.Play(theStation, getContext());

        historyManager.add(theStation);
    }

    public void RefreshListGui() {
        if (BuildConfig.DEBUG) Log.d(TAG, "refreshing the stations list.");

        ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();

        if (BuildConfig.DEBUG) Log.d(TAG, "stations count:" + favouriteManager.listStations.size());

        adapter.updateList(this, favouriteManager.listStations);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        historyManager = radioDroidApp.getHistoryManager();
        favouriteManager = radioDroidApp.getFavouriteManager();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations, container, false);
        rvStations = (RecyclerView) view.findViewById(R.id.recyclerViewStations);

        ItemAdapterStation adapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station);
        adapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
            @Override
            public void onStationClick(DataRadioStation station) {
                FragmentStarred.this.onStationClick(station);
            }

            @Override
            public void onStationSwiped(final DataRadioStation station) {
                final int removedIdx = favouriteManager.remove(station.ID);

                RefreshListGui();

                Snackbar snackbar = Snackbar
                        .make(rvStations, R.string.notify_station_removed_from_list, Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.action_station_removed_from_list_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        favouriteManager.restore(station, removedIdx);
                        RefreshListGui();
                    }
                });
                snackbar.setActionTextColor(Color.GREEN);
                snackbar.setDuration(Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rvStations.setAdapter(adapter);
        rvStations.setLayoutManager(llm);

        adapter.enableItemRemoval(rvStations);

        RefreshListGui();

        return view;
    }
}