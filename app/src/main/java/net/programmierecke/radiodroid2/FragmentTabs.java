package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.programmierecke.radiodroid2.interfaces.IFragmentRefreshable;
import net.programmierecke.radiodroid2.interfaces.IFragmentSearchable;

import java.util.ArrayList;
import java.util.List;

public class FragmentTabs extends Fragment implements IFragmentRefreshable, IFragmentSearchable {
    private String itsAdressWWWLocal = "json/stations/bycountryexact/internet?order=clickcount&reverse=true";
    private String itsAdressWWWTopClick = "json/stations/topclick/100";
    private String itsAdressWWWTopVote = "json/stations/topvote/100";
    private String itsAdressWWWChangedLately = "json/stations/lastchange/100";
    private String itsAdressWWWCurrentlyHeard = "json/stations/lastclick/100";
    private String itsAdressWWWTags = "json/tags";
    private String itsAdressWWWCountries = "json/countries";
    private String itsAdressWWWLanguages = "json/languages";

    private ViewPager viewPager;

    private String searchQuery; // Search may be requested before onCreateView so we should wait

    FragmentBase[] fragments = new FragmentBase[9];
    String[] adresses = new String[]{
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
        final TabLayout tabLayout = (TabLayout) x.findViewById(R.id.tabs);
        viewPager = (ViewPager) x.findViewById(R.id.viewpager);

        setupViewPager(viewPager);

        if (searchQuery != null) {
            Search(searchQuery);
            searchQuery = null;
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

    private void setupViewPager(ViewPager viewPager) {
        Context ctx = getContext();
        String countryCode = null;
        String country = null;
        if (ctx != null) {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            countryCode = tm.getNetworkCountryIso();
            if (countryCode == null) {
                countryCode = tm.getSimCountryIso();
            }
            Log.e("YYY", "Found countrycode " + countryCode);
            country = CountryCodeDictionary.getInstance().getCountryByCode(countryCode);
            Log.e("YYY", "Found country " + country);

            adresses[0] = "json/stations/bycountryexact/" + country + "?order=clickcount&reverse=true";
        }
        for (int i = 0; i < fragments.length; i++) {
            if (i < 5)
                fragments[i] = new FragmentStations();
            else if (i < 8)
                fragments[i] = new FragmentCategories();
            else
                fragments[i] = new FragmentStations();
            Bundle bundle1 = new Bundle();
            bundle1.putString("url", RadioBrowserServerManager.getWebserviceEndpoint(getContext(),adresses[i]));
            fragments[i].setArguments(bundle1);
        }

        ((FragmentCategories) fragments[5]).EnableSingleUseFilter(true);
        ((FragmentCategories) fragments[5]).SetBaseSearchLink("json/stations/bytagexact");
        ((FragmentCategories) fragments[6]).SetBaseSearchLink("json/stations/bycountryexact");
        ((FragmentCategories) fragments[7]).SetBaseSearchLink("json/stations/bylanguageexact");

        FragmentManager m = getChildFragmentManager();
        ViewPagerAdapter adapter = new ViewPagerAdapter(m);
        if (country != null){
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

    public void Search(final String query) {
        if (viewPager != null) {
            viewPager.setCurrentItem(8, false);
            fragments[8].SetDownloadUrl(query);
        } else {
            searchQuery = query;
        }
    }

    @Override
    public void Refresh() {
        FragmentBase fragment = fragments[viewPager.getCurrentItem()];
        fragment.DownloadUrl(true);
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
