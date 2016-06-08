package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import net.programmierecke.radiodroid2.adapters.ItemAdapterStation;
import net.programmierecke.radiodroid2.data.DataRadioStation;

public class FragmentStations extends FragmentBase {
    private ListView lv;
    private DataRadioStation[] data = new DataRadioStation[0];
    private SwipeRefreshLayout mySwipeRefreshLayout;
    private SharedPreferences sharedPref;

    public FragmentStations() {
    }

    void ClickOnItem(DataRadioStation theStation) {
        ActivityMain a = (ActivityMain)getActivity();

        Utils.Play(theStation,getContext());

        HistoryManager hm = new HistoryManager(a.getApplicationContext());
        hm.add(theStation);
    }

    @Override
    protected void RefreshListGui(){
        Log.d("ABC", "RefreshListGUI()");
        Context ctx = getContext();
        if (lv != null && ctx != null) {
            if (sharedPref == null) {
                sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
            }
            boolean show_broken = sharedPref.getBoolean("show_broken", false);

            Log.d("ABC", "LV != null");
            data = DataRadioStation.DecodeJson(getUrlResult());
            ItemAdapterStation arrayAdapter = (ItemAdapterStation) lv.getAdapter();
            arrayAdapter.clear();
            Log.d("ABC", "Station count:" + data.length);
            for (DataRadioStation aStation : data) {
                if (show_broken || aStation.Working) {
                    arrayAdapter.add(aStation);
                }
            }

            lv.invalidate();
        } else {
            Log.e("NULL", "LV == null");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ItemAdapterStation arrayAdapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations_remote, container, false);

        lv = (ListView) view.findViewById(R.id.listViewStations);
        lv.setAdapter(arrayAdapter);
        lv.setTextFilterEnabled(true);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object anObject = parent.getItemAtPosition(position);
                if (anObject instanceof DataRadioStation) {
                    ClickOnItem((DataRadioStation) anObject);
                }
            }
        });

        mySwipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.swiperefresh);
        if (mySwipeRefreshLayout != null) {
            mySwipeRefreshLayout.setOnRefreshListener(
                    new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            Log.i("ABC", "onRefresh called from SwipeRefreshLayout");
                            //RefreshListGui();
                            DownloadUrl(true,false);
                        }
                    }
            );
        }

        RefreshListGui();

        return view;
    }

    @Override
    protected void DownloadFinished() {
        if (mySwipeRefreshLayout != null) {
            mySwipeRefreshLayout.setRefreshing(false);
        }
    }
}