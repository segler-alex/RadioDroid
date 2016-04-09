package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class FragmentStations extends Fragment {
    private ProgressDialog itsProgressLoading;
    private RadioItemBigAdapter itsArrayAdapter = null;
    private ListView lv;
    private String url;

    public FragmentStations() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getArguments();
        url = bundle.getString("url");

        itsArrayAdapter = new RadioItemBigAdapter(getActivity(), R.layout.list_item_big);

        DownloadStations();
    }

    void ClickOnItem(RadioStation theStation) {
        MainActivity a = (MainActivity)getActivity();
        PlayerService thisService = new PlayerService();
        thisService.unbindSafely( a, a.getSvc() );

        Intent anIntent = new Intent(getActivity().getBaseContext(), RadioDroidStationDetail.class);
        anIntent.putExtra("stationid", theStation.ID);
        startActivity(anIntent);
    }

    public void SetDownloadUrl(String theUrl) {
        Log.w("","new url "+theUrl);
        url = theUrl;
        DownloadStations();
    }

    public void DownloadStations() {
        if (TextUtils.isGraphic(url)) {
            itsProgressLoading = ProgressDialog.show(getActivity(), "", "Loading...");
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    return Utils.downloadFeed(url);
                }

                @Override
                protected void onPostExecute(String result) {
                    stations = Utils.DecodeJson(result);
                    RefreshListGui();
                    itsProgressLoading.dismiss();
                    super.onPostExecute(result);
                }
            }.execute();
        }else{
            RefreshListGui();
        }
    }

    RadioStation[] stations = new RadioStation[0];

    void RefreshListGui(){
        itsArrayAdapter.clear();
        for (RadioStation aStation : stations) {
            itsArrayAdapter.add(aStation);
        }
        if (lv != null) {
            lv.invalidate();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations, container, false);

        lv = (ListView) view.findViewById(R.id.listViewStations);
        lv.setAdapter(itsArrayAdapter);
        lv.setTextFilterEnabled(true);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object anObject = parent.getItemAtPosition(position);
                if (anObject instanceof RadioStation) {
                    ClickOnItem((RadioStation) anObject);
                }
            }
        });

        RefreshListGui();

        return view;
    }
}