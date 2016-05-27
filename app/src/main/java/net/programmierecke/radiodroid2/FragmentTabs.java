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

    public static TabLayout tabLayout;
    public static ViewPager viewPager;

    FragmentBase[] fragments = new FragmentBase[8];
    String[] adresses = new String[]{
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
        View x =  inflater.inflate(R.layout.layout_tabs,null);
        tabLayout = (TabLayout) x.findViewById(R.id.tabs);
        viewPager = (ViewPager) x.findViewById(R.id.viewpager);

        setupViewPager(viewPager);

        /**
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
        for (int i=0;i<fragments.length;i++) {
            if (i < 4)
                fragments[i] = new FragmentStations();
            else if (i < 7)
                fragments[i] = new FragmentCategories();
            else
                fragments[i] = new FragmentStations();
            Bundle bundle1 = new Bundle();
            bundle1.putString("url", adresses[i]);
            fragments[i].setArguments(bundle1);
        }

        ((FragmentCategories)fragments[4]).EnableSingleUseFilter(true);
        ((FragmentCategories)fragments[4]).SetBaseSearchLink("http://www.radio-browser.info/webservice/json/stations/bytagexact");
        ((FragmentCategories)fragments[5]).SetBaseSearchLink("http://www.radio-browser.info/webservice/json/stations/bycountryexact");
        ((FragmentCategories)fragments[6]).SetBaseSearchLink("http://www.radio-browser.info/webservice/json/stations/bylanguageexact");

        FragmentManager m = getChildFragmentManager();
        ViewPagerAdapter adapter = new ViewPagerAdapter(m);
        adapter.addFragment(fragments[0], R.string.action_top_click);
        adapter.addFragment(fragments[1], R.string.action_top_vote);
        adapter.addFragment(fragments[2], R.string.action_changed_lately);
        adapter.addFragment(fragments[3], R.string.action_currently_playing);
        adapter.addFragment(fragments[4], R.string.action_tags);
        adapter.addFragment(fragments[5], R.string.action_countries);
        adapter.addFragment(fragments[6], R.string.action_languages);
        adapter.addFragment(fragments[7], R.string.action_search);
        viewPager.setAdapter(adapter);
    }

    public void Search(String query) {
        viewPager.setCurrentItem(7);
        fragments[7].SetDownloadUrl(query);
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
