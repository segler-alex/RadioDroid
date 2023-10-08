package net.programmierecke.radiodroid2;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import net.programmierecke.radiodroid2.station.ItemAdapterStation;
import net.programmierecke.radiodroid2.station.DataRadioStation;
import net.programmierecke.radiodroid2.interfaces.IAdapterRefreshable;
import net.programmierecke.radiodroid2.station.StationActions;
import net.programmierecke.radiodroid2.station.StationsFilter;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

public class FragmentHistory extends Fragment implements IAdapterRefreshable {
    private static final String TAG = "FragmentHistory";

    private RecyclerView rvStations;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AsyncTask task = null;

    private HistoryManager historyManager;

    void onStationClick(DataRadioStation theStation) {
        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        Utils.showPlaySelection(radioDroidApp, theStation, getActivity().getSupportFragmentManager());

        RefreshListGui();
        rvStations.smoothScrollToPosition(0);
    }

    @Override
    public void RefreshListGui() {
        if (BuildConfig.DEBUG) Log.d(TAG, "refreshing the stations list.");

        ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();

        if (BuildConfig.DEBUG) Log.d(TAG, "stations count:" + historyManager.listStations.size());

        if( adapter != null )
            adapter.updateList(null, historyManager.listStations);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        historyManager = radioDroidApp.getHistoryManager();

        ItemAdapterStation adapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station, StationsFilter.FilterType.LOCAL);
        adapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
            @Override
            public void onStationClick(DataRadioStation station, int pos) {
                FragmentHistory.this.onStationClick(station);
            }

            @Override
            public void onStationSwiped(final DataRadioStation station) {
                final int removedIdx = historyManager.remove(station.StationUuid);

                RefreshListGui();

                Snackbar snackbar = Snackbar
                        .make(rvStations, R.string.notify_station_removed_from_list, 6000);
                snackbar.setAnchorView(getView().getRootView().findViewById(R.id.bottom_sheet));
                snackbar.setAction(R.string.action_station_removed_from_list_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        historyManager.restore(station, removedIdx);
                        RefreshListGui();
                    }
                });
                snackbar.show();
            }

            @Override
            public void onStationMoved(int from, int to) {
            }

            @Override
            public void onStationMoveFinished() {
            }
        });

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations, container, false);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rvStations = (RecyclerView) view.findViewById(R.id.recyclerViewStations);
        rvStations.setAdapter(adapter);
        rvStations.setLayoutManager(llm);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvStations.getContext(),
                llm.getOrientation());
        rvStations.addItemDecoration(dividerItemDecoration);

        adapter.enableItemRemoval(rvStations);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(
                    new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "onRefresh called from SwipeRefreshLayout");
                            }
                            RefreshDownloadList();
                        }
                    }
            );
        }

        RefreshListGui();

        return view;
    }

    void RefreshDownloadList(){
        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        final OkHttpClient httpClient = radioDroidApp.getHttpClient();
        ArrayList<String> listUUids = new ArrayList<String>();
        for (DataRadioStation station : historyManager.listStations){
            listUUids.add(station.StationUuid);
        }
        Log.d(TAG, "Search for items: "+listUUids.size());

        task = new AsyncTask<Void, Void, List<DataRadioStation>>() {
            @Override
            protected List<DataRadioStation> doInBackground(Void... params) {
                return Utils.getStationsByUuid(httpClient, getActivity(), listUUids);
            }

            @Override
            protected void onPostExecute(List<DataRadioStation> result) {
                DownloadFinished();
                if(getContext() != null)
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Download relativeUrl finished");
                }
                if (result != null) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Download relativeUrl OK");
                    }
                    Log.d(TAG, "Found items: "+result.size());
                    SyncList(result);
                    RefreshListGui();
                } else {
                    try {
                        Toast toast = Toast.makeText(getContext(), getResources().getText(R.string.error_list_update), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    catch(Exception e){
                        Log.e("ERR",e.toString());
                    }
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    private void SyncList(List<DataRadioStation> list_new) {
        ArrayList<String> to_remove = new ArrayList<String>();
        for (DataRadioStation station_current: historyManager.listStations){
            boolean found = false;
            for (DataRadioStation station_new: list_new){
                if (station_new.StationUuid.equals(station_current.StationUuid)){
                    found = true;
                }
            }
            if (!found){
                Log.d(TAG,"Remove station: " + station_current.StationUuid + " - " + station_current.Name);
                to_remove.add(station_current.StationUuid);
                station_current.DeletedOnServer = true;
            }
        }
        Log.d(TAG,"replace items");
        historyManager.replaceList(list_new);
        Log.d(TAG,"fin save");

        if (to_remove.size() > 0) {
            Toast toast = Toast.makeText(getContext(), getResources().getString(R.string.notify_sync_list_deleted_entries, to_remove.size(), historyManager.size()), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    protected void DownloadFinished() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rvStations.setAdapter(null);
    }
}
