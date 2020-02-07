package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import net.programmierecke.radiodroid2.interfaces.IFragmentRefreshable;
import net.programmierecke.radiodroid2.interfaces.IFragmentSearchable;
import net.programmierecke.radiodroid2.station.FragmentStations;
import net.programmierecke.radiodroid2.station.StationsFilter;

import java.util.ArrayList;
import java.util.List;

public class FragmentTabs extends Fragment implements IFragmentRefreshable, IFragmentSearchable {
    private String itsAdressWWWLocal = "json/stations/bycountryexact/internet?order=clickcount&reverse=true";
    private String itsAdressWWWTopClick = "json/stations/topclick/100";
    private String itsAdressWWWTopVote = "json/stations/topvote/100";
    private String itsAdressWWWChangedLately = "json/stations/lastchange/100";
    private String itsAdressWWWCurrentlyHeard = "json/stations/lastclick/100";
    private String itsAdressWWWTags = "json/tags";
    private String itsAdressWWWCountries = "json/countrycodes";
    private String itsAdressWWWLanguages = "json/languages";

    private ViewPager viewPager;

    private String queuedSearchQuery; // Search may be requested before onCreateView so we should wait
    private StationsFilter.SearchStyle queuedSearchStyle;

    private Fragment[] fragments = new Fragment[9];
    private String[] adresses = new String[]{
            itsAdressWWWLocal,
            itsAdressWWWTopClick,
            itsAdressWWWTopVote,
            itsAdressWWWChangedLately,
            itsAdressWWWCurrentlyHeard,
            itsAdressWWWTags,
            itsAdressWWWCountries,
            itsAdressWWWLanguages,
            ""
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View x = inflater.inflate(R.layout.layout_tabs, null);
        final TabLayout tabLayout = getActivity().findViewById(R.id.tabs);
        viewPager = (ViewPager) x.findViewById(R.id.viewpager);

        setupViewPager(viewPager);

        if (queuedSearchQuery != null) {
            Log.d("TABS", "do queued search by name:"+ queuedSearchQuery);
            Search(queuedSearchStyle, queuedSearchQuery);
            queuedSearchQuery = null;
            queuedSearchStyle = StationsFilter.SearchStyle.ByName;
        }

        /*
         * Now , this is a workaround ,
         * The setupWithViewPager doesn't works without the runnable .
         * Maybe a Support Library Bug .
         */

        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                if(getContext() != null)
                    tabLayout.setupWithViewPager(viewPager);
            }
        });

        return x;
    }

    @Override
    public void onResume() {
        super.onResume();

        final TabLayout tabLayout = getActivity().findViewById(R.id.tabs);
        tabLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();

        final TabLayout tabLayout = getActivity().findViewById(R.id.tabs);
        tabLayout.setVisibility(View.GONE);
    }

    private void setupViewPager(ViewPager viewPager) {
        Context ctx = getContext();
        String countryCode = null;
        if (ctx != null) {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            countryCode = tm.getNetworkCountryIso();
            if (countryCode == null) {
                countryCode = tm.getSimCountryIso();
            }
            if (countryCode != null) {
                if (countryCode.length() == 2) {
                    Log.d("MAIN", "Found countrycode " + countryCode);
                    adresses[0] = "json/stations/bycountrycodeexact/" + countryCode + "?order=clickcount&reverse=true";
                }else{
                    Log.e("MAIN", "countrycode length != 2");
                }
            }else{
                Log.e("MAIN", "device countrycode and sim countrycode are null");
            }
        }
        for (int i = 0; i < fragments.length; i++) {
            Bundle bundle = new Bundle();

            if (i < 5) {
                fragments[i] = new FragmentStations();
            } else if (i < 8) {
                fragments[i] = new FragmentCategories();
            } else {
                fragments[i] = new FragmentStations();
                bundle.putBoolean(FragmentStations.KEY_SEARCH_ENABLED, true);
            }

            bundle.putString("url", adresses[i]);
            fragments[i].setArguments(bundle);
        }

        ((FragmentCategories) fragments[5]).EnableSingleUseFilter(true);
        ((FragmentCategories) fragments[5]).SetBaseSearchLink(StationsFilter.SearchStyle.ByTagExact);
        ((FragmentCategories) fragments[6]).SetBaseSearchLink(StationsFilter.SearchStyle.ByCountryCodeExact);
        ((FragmentCategories) fragments[7]).SetBaseSearchLink(StationsFilter.SearchStyle.ByLanguageExact);

        FragmentManager m = getChildFragmentManager();
        ViewPagerAdapter adapter = new ViewPagerAdapter(m);
        if (countryCode != null){
            adapter.addFragment(fragments[0], R.string.action_local);
        }
        adapter.addFragment(fragments[1], R.string.action_top_click);
        adapter.addFragment(fragments[2], R.string.action_top_vote);
        adapter.addFragment(fragments[3], R.string.action_changed_lately);
        adapter.addFragment(fragments[4], R.string.action_currently_playing);
        adapter.addFragment(fragments[5], R.string.action_tags);
        adapter.addFragment(fragments[6], R.string.action_countries);
        adapter.addFragment(fragments[7], R.string.action_languages);
        adapter.addFragment(fragments[8], R.string.action_search);
        viewPager.setAdapter(adapter);
    }

    public void Search(StationsFilter.SearchStyle searchStyle, final String query) {
        Log.d("TABS","Search = "+ query + " searchStyle="+searchStyle);
        if (viewPager != null) {
            Log.d("TABS","a Search = "+ query);
            viewPager.setCurrentItem(8, false);
            ((IFragmentSearchable)fragments[8]).Search(searchStyle, query);
        } else {
            Log.d("TABS","b Search = "+ query);
            queuedSearchQuery = query;
            queuedSearchStyle = searchStyle;
        }
    }

    @Override
    public void Refresh() {
        Fragment fragment = fragments[viewPager.getCurrentItem()];
        if (fragment instanceof FragmentBase) {
            ((FragmentBase) fragment).DownloadUrl(true);
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<Integer> mFragmentTitleList = new ArrayList<Integer>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, int title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Resources res = getResources();
            return res.getString(mFragmentTitleList.get(position));
        }
    }
}
