package net.programmierecke.radiodroid2;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.programmierecke.radiodroid2.interfaces.IAdapterRefreshable;
import net.programmierecke.radiodroid2.interfaces.IFragmentRefreshable;
import net.programmierecke.radiodroid2.interfaces.IFragmentSearchable;

import java.util.ArrayList;
import java.util.List;

public class FragmentTabs extends Fragment implements IFragmentRefreshable, IFragmentSearchable {
    private String itsAdressWWWTopClick = "http://www.radio-browser.info/webservice/json/stations/topclick/100";
    private String itsAdressWWWTopVote = "http://www.radio-browser.info/webservice/json/stations/topvote/100";
    private String itsAdressWWWChangedLately = "http://www.radio-browser.info/webservice/json/stations/lastchange/100";
    private String itsAdressWWWCurrentlyHeard = "http://www.radio-browser.info/webservice/json/stations/lastclick/100";
    private String itsAdressWWWTags = "http://www.radio-browser.info/webservice/json/tags";
    private String itsAdressWWWCountries = "http://www.radio-browser.info/webservice/json/countries";
    private String itsAdressWWWLanguages = "http://www.radio-browser.info/webservice/json/languages";

    // Note: the order of tabs needs to be set here as well as
    // further down when populating the ViewPagerAdapter
    private static final int IDX_STARRED = 0;
    private static final int IDX_TOP_CLICK = 1;
    private static final int IDX_TOP_VOTE = 2;
    private static final int IDX_CHANGED_LATELY = 3;
    private static final int IDX_CURRENTLY_HEARD = 4;
    private static final int IDX_TAGS = 5;
    private static final int IDX_COUNTRIES = 6;
    private static final int IDX_LANGUAGES = 7;
    private static final int IDX_SEARCH = 8;

    public static TabLayout tabLayout;
    public static ViewPager viewPager;

    Fragment[] fragments = new Fragment[9];

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View x =  inflater.inflate(R.layout.layout_tabs,null);
        tabLayout = (TabLayout) x.findViewById(R.id.tabs);
        viewPager = (ViewPager) x.findViewById(R.id.viewpager);

        setupViewPager(viewPager);

        /*
         * Now , this is a workaround ,
         * The setupWithViewPager dose't works without the runnable .
         * Maybe a Support Library Bug .
         */

        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                tabLayout.setupWithViewPager(viewPager);
            }
        });

        return x;
    }

    private void setupViewPager(ViewPager viewPager) {
        fragments[IDX_STARRED] = new FragmentStarred();
        fragments[IDX_TOP_CLICK] = new FragmentStations();
        fragments[IDX_TOP_VOTE] = new FragmentStations();
        fragments[IDX_CHANGED_LATELY] = new FragmentStations();
        fragments[IDX_CURRENTLY_HEARD] = new FragmentStations();
        fragments[IDX_TAGS] = new FragmentCategories();
        fragments[IDX_COUNTRIES] = new FragmentCategories();
        fragments[IDX_LANGUAGES] = new FragmentCategories();
        fragments[IDX_SEARCH] = new FragmentStations();

        String[] addresses = new String[]{
                "",
                itsAdressWWWTopClick,
                itsAdressWWWTopVote,
                itsAdressWWWChangedLately,
                itsAdressWWWCurrentlyHeard,
                itsAdressWWWTags,
                itsAdressWWWCountries,
                itsAdressWWWLanguages,
                ""
        };

        for (int i=0;i<fragments.length;i++) {
            Bundle bundle1 = new Bundle();
            bundle1.putString("url", addresses[i]);
            fragments[i].setArguments(bundle1);
        }

        ((FragmentCategories)fragments[IDX_TAGS]).EnableSingleUseFilter(true);
        ((FragmentCategories)fragments[IDX_TAGS]).SetBaseSearchLink("http://www.radio-browser.info/webservice/json/stations/bytagexact");
        ((FragmentCategories)fragments[IDX_COUNTRIES]).SetBaseSearchLink("http://www.radio-browser.info/webservice/json/stations/bycountryexact");
        ((FragmentCategories)fragments[IDX_LANGUAGES]).SetBaseSearchLink("http://www.radio-browser.info/webservice/json/stations/bylanguageexact");

        FragmentManager m = getChildFragmentManager();
        ViewPagerAdapter adapter = new ViewPagerAdapter(m);
        adapter.addFragment(fragments[IDX_STARRED], R.string.nav_item_starred);
        adapter.addFragment(fragments[IDX_TOP_CLICK], R.string.action_top_click);
        adapter.addFragment(fragments[IDX_TOP_VOTE], R.string.action_top_vote);
        adapter.addFragment(fragments[IDX_CHANGED_LATELY], R.string.action_changed_lately);
        adapter.addFragment(fragments[IDX_CURRENTLY_HEARD], R.string.action_currently_playing);
        adapter.addFragment(fragments[IDX_TAGS], R.string.action_tags);
        adapter.addFragment(fragments[IDX_COUNTRIES], R.string.action_countries);
        adapter.addFragment(fragments[IDX_LANGUAGES], R.string.action_languages);
        adapter.addFragment(fragments[IDX_SEARCH], R.string.action_search);
        viewPager.setAdapter(adapter);
    }

    public void Search(String query) {
        viewPager.setCurrentItem(IDX_SEARCH);
        ((FragmentBase)fragments[IDX_SEARCH]).SetDownloadUrl(query);
    }

    @Override
    public void Refresh() {
        Fragment fragment = fragments[viewPager.getCurrentItem()];
        if(fragment instanceof FragmentBase) {
            ((FragmentBase) fragment).DownloadUrl(true);
        } else if (fragment instanceof IAdapterRefreshable) {
            ((FragmentStarred)fragment).RefreshListGui();
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
