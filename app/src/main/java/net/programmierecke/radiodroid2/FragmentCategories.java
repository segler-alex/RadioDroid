package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.programmierecke.radiodroid2.adapters.ItemAdapterCategory;
import net.programmierecke.radiodroid2.data.DataCategory;
import net.programmierecke.radiodroid2.station.StationsFilter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;

public class FragmentCategories extends FragmentBase {
    private static final String TAG = "FragmentCategories";

    private RecyclerView rvCategories;
    private StationsFilter.SearchStyle searchStyle = StationsFilter.SearchStyle.ByName;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean singleUseFilter = false;
    private SharedPreferences sharedPref;

    public FragmentCategories() {
    }

    public void SetBaseSearchLink(StationsFilter.SearchStyle searchStyle) {
        this.searchStyle = searchStyle;
    }

    void ClickOnItem(DataCategory theData) {
        ActivityMain m = (ActivityMain) getActivity();
        m.Search(this.searchStyle, theData.Name);
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
        CountryCodeDictionary countryDict = CountryCodeDictionary.getInstance();
        CountryFlagsLoader flagsDict = CountryFlagsLoader.getInstance();

        for (DataCategory aData : data) {
            if (!singleUseFilter || show_single_use_tags || (aData.UsedCount > 1)) {
                if (searchStyle == StationsFilter.SearchStyle.ByCountryCodeExact){
                    aData.Label = countryDict.getCountryByCode(aData.Name);
                    aData.Icon = flagsDict.getFlag(requireContext(), aData.Name);
                }
                filteredCategoriesList.add(aData);
            }
        }

        if (searchStyle == StationsFilter.SearchStyle.ByCountryCodeExact) {
            Collections.sort(filteredCategoriesList);
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