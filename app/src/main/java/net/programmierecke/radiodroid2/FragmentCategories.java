package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.programmierecke.radiodroid2.adapters.ItemAdapterCategory;
import net.programmierecke.radiodroid2.data.DataCategory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class FragmentCategories extends FragmentBase {
    private static final String TAG = "FragmentCategories";

    private RecyclerView rvCategories;
    private String baseSearchAddress = "";
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean singleUseFilter = false;
    private SharedPreferences sharedPref;

    public FragmentCategories() {
    }

    public void SetBaseSearchLink(String url) {
        this.baseSearchAddress = url;
    }

    void ClickOnItem(DataCategory theData) {
        ActivityMain m = (ActivityMain) getActivity();

        try {
            String queryEncoded = URLEncoder.encode(theData.Name, "utf-8");
            queryEncoded = queryEncoded.replace("+", "%20");
            m.Search(RadioBrowserServerManager.getWebserviceEndpoint(m,baseSearchAddress + "/" + queryEncoded));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void RefreshListGui() {
        if (rvCategories == null) {
            return;
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "refreshing the categories list.");

        Context ctx = getContext();
        if (sharedPref == null) {
            sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        }

        boolean show_single_use_tags = sharedPref.getBoolean("single_use_tags", false);

        ArrayList<DataCategory> filteredCategoriesList = new ArrayList<>();
        DataCategory[] data = DataCategory.DecodeJson(getUrlResult());

        if (BuildConfig.DEBUG) Log.d(TAG, "categories count:" + data.length);

        for (DataCategory aData : data) {
            if (!singleUseFilter || show_single_use_tags || (aData.UsedCount > 1)) {
                filteredCategoriesList.add(aData);
            }
        }

        ItemAdapterCategory adapter = (ItemAdapterCategory) rvCategories.getAdapter();
        adapter.updateList(filteredCategoriesList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ItemAdapterCategory adapterCategory = new ItemAdapterCategory(R.layout.list_item_category);
        adapterCategory.setCategoryClickListener(new ItemAdapterCategory.CategoryClickListener() {
            @Override
            public void onCategoryClick(DataCategory category) {
                ClickOnItem(category);
            }
        });

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stations_remote, container, false);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rvCategories = (RecyclerView) view.findViewById(R.id.recyclerViewStations);
        rvCategories.setAdapter(adapterCategory);
        rvCategories.setLayoutManager(llm);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(
                    new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "onRefresh called from SwipeRefreshLayout");
                            }
                            //RefreshListGui();
                            DownloadUrl(true, false);
                        }
                    }
            );
        }

        RefreshListGui();

        return view;
    }

    public void EnableSingleUseFilter(boolean b) {
        this.singleUseFilter = b;
    }

    @Override
    protected void DownloadFinished() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}