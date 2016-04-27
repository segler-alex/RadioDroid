package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class FragmentCategories extends FragmentBase {
    private ProgressDialog itsProgressLoading;
    private ItemAdapterCategory itsArrayAdapter = null;
    private ListView lv;
    private String url;
    private DataCategory[] data = new DataCategory[0];
    private String baseSearchAdress = "";

    public FragmentCategories() {
    }

    @Override
    protected void InitArrayAdapter(){
        itsArrayAdapter = new ItemAdapterCategory(getActivity(), R.layout.list_item_category);
    }

    public void SetBaseSearchLink(String url){
        this.baseSearchAdress = url;
    }

    void ClickOnItem(DataCategory theData) {
        MainActivity m = (MainActivity)getActivity();

        try {
            String queryEncoded = URLEncoder.encode(theData.Name, "utf-8");
            m.Search(baseSearchAdress+"/"+queryEncoded);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void RefreshListGui(){
        data = DataCategory.DecodeJson(getUrlResult());
        itsArrayAdapter.clear();
        for (DataCategory aData : data) {
            itsArrayAdapter.add(aData);
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
                if (anObject instanceof DataCategory) {
                    ClickOnItem((DataCategory) anObject);
                }
            }
        });

        RefreshListGui();

        return view;
    }
}