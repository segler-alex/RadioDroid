package net.programmierecke.radiodroid2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import net.programmierecke.radiodroid2.adapters.ItemAdapterStation;
import net.programmierecke.radiodroid2.data.DataRadioStation;
import net.programmierecke.radiodroid2.interfaces.IAdapterRefreshable;

public class FragmentStarred extends Fragment implements IAdapterRefreshable {
    private ListView lv;
    private DataRadioStation[] data = new DataRadioStation[0];

    public FragmentStarred() {
    }

    void ClickOnItem(DataRadioStation theStation) {
        ActivityMain activity = (ActivityMain)getActivity();

        Utils.Play(theStation,getContext());

        HistoryManager hm = new HistoryManager(activity.getApplicationContext());
        hm.add(theStation);
    }

    public void RefreshListGui(){
        Log.d("ABC", "RefreshListGUI()");

        if (lv != null) {
            Log.d("ABC","LV != null");
            FavouriteManager favouriteManager = new FavouriteManager(getActivity());
            ItemAdapterStation arrayAdapter = (ItemAdapterStation) lv.getAdapter();
            arrayAdapter.clear();
            Log.d("ABC","Station count:"+data.length);
            for (DataRadioStation aStation : favouriteManager.getList()) {
                arrayAdapter.add(aStation);
            }

            lv.invalidate();
        }else{
            Log.e("NULL","LV == null");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ItemAdapterStation arrayAdapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station);
        arrayAdapter.setUpdate(this);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations, container, false);

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

        RefreshListGui();

        return view;
    }
}