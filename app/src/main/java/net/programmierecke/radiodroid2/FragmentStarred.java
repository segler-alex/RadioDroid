package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.programmierecke.radiodroid2.station.ItemAdapterStation;
import net.programmierecke.radiodroid2.station.DataRadioStation;
import net.programmierecke.radiodroid2.station.ItemAdapterIconOnlyStation;
import net.programmierecke.radiodroid2.interfaces.IAdapterRefreshable;

import java.util.Observable;
import java.util.Observer;

public class FragmentStarred extends Fragment implements IAdapterRefreshable, Observer {
    private static final String TAG = "FragmentStarred";

    private RecyclerView rvStations;

    private FavouriteManager favouriteManager;

    void onStationClick(DataRadioStation theStation) {
        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        Utils.showPlaySelection(radioDroidApp, theStation, getActivity().getSupportFragmentManager());
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

        RadioDroidApp radioDroidApp = (RadioDroidApp) requireActivity().getApplication();
        favouriteManager = radioDroidApp.getFavouriteManager();
        favouriteManager.addObserver(this);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations, container, false);
        rvStations = (RecyclerView) view.findViewById(R.id.recyclerViewStations);

        ItemAdapterStation adapter;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (sharedPref.getBoolean("load_icons", false) && sharedPref.getBoolean("icons_only_favorites_style", false)) {
            adapter = new ItemAdapterIconOnlyStation(getActivity(), R.layout.list_item_icon_only_station);
            Context ctx = getContext();
            DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
            int itemWidth = (int) ctx.getResources().getDimension(R.dimen.regular_style_icon_container_width);
            int noOfColumns = displayMetrics.widthPixels / itemWidth;
            GridLayoutManager glm = new GridLayoutManager(ctx, noOfColumns);
            rvStations.setAdapter(adapter);
            rvStations.setLayoutManager(glm);
            ((ItemAdapterIconOnlyStation)adapter).enableItemMove(rvStations);
        } else {
            adapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station);
            LinearLayoutManager llm = new LinearLayoutManager(getContext());
            llm.setOrientation(RecyclerView.VERTICAL);

            rvStations.setAdapter(adapter);
            rvStations.setLayoutManager(llm);
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvStations.getContext(),
                    llm.getOrientation());
            rvStations.addItemDecoration(dividerItemDecoration);
            adapter.enableItemMoveAndRemoval(rvStations);
        }

        adapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
            @Override
            public void onStationClick(DataRadioStation station) {
                FragmentStarred.this.onStationClick(station);
            }

            @Override
            public void onStationSwiped(final DataRadioStation station) {
                final int removedIdx = favouriteManager.remove(station.StationUuid);

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
                snackbar.setDuration(BaseTransientBottomBar.LENGTH_LONG);
                snackbar.show();
            }

            @Override
            public void onStationMoved(int from, int to) {
                favouriteManager.move(from, to);
            }

            @Override
            public void onStationMoveFinished() {
                favouriteManager.Save();
            }
        });


        RefreshListGui();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        rvStations.setAdapter(null);

        RadioDroidApp radioDroidApp = (RadioDroidApp) requireActivity().getApplication();
        favouriteManager = radioDroidApp.getFavouriteManager();
        favouriteManager.deleteObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        RefreshListGui();
    }
}