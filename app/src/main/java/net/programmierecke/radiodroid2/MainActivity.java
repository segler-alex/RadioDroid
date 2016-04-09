package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
	private String itsAdressWWWTopClick = "http://www.radio-browser.info/webservice/json/stations/topclick/100";
	private String itsAdressWWWTopVote = "http://www.radio-browser.info/webservice/json/stations/topvote/100";
	private String itsAdressWWWChangedLately = "http://www.radio-browser.info/webservice/json/stations/lastchange/100";

	private TabLayout tabLayout;
	private ViewPager viewPager;
	private SearchView mSearchView;

	private static final String TAG = "RadioDroid";
	IPlayerService itsPlayerService;
	private ServiceConnection svcConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.v(TAG, "Service came online");
			itsPlayerService = IPlayerService.Stub.asInterface(binder);
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.v(TAG, "Service offline");
			itsPlayerService = null;
		}
	};

	public ServiceConnection getSvc(){
		return svcConn;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_main);

		Intent anIntent = new Intent(this, PlayerService.class);
		bindService(anIntent, svcConn, BIND_AUTO_CREATE);
		startService(anIntent);

		Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
		setSupportActionBar(myToolbar);

		viewPager = (ViewPager) findViewById(R.id.viewpager);
		setupViewPager(viewPager);

		tabLayout = (TabLayout) findViewById(R.id.tabs);
		tabLayout.setupWithViewPager(viewPager);
	}

	FragmentStations[] fragments = new FragmentStations[4];

	private void setupViewPager(ViewPager viewPager) {
		fragments[0] = new FragmentStations();
		Bundle bundle1 = new Bundle();
		bundle1.putString("url", itsAdressWWWTopClick);
		fragments[0].setArguments(bundle1);

		fragments[1] = new FragmentStations();
		Bundle bundle2 = new Bundle();
		bundle2.putString("url", itsAdressWWWTopVote);
		fragments[1].setArguments(bundle2);

		fragments[2] = new FragmentStations();
		Bundle bundle3 = new Bundle();
		bundle3.putString("url", itsAdressWWWChangedLately);
		fragments[2].setArguments(bundle3);

		fragments[3] = new FragmentStations();
		Bundle bundle4 = new Bundle();
		bundle4.putString("url", "");
		fragments[3].setArguments(bundle4);

		FragmentManager m = getSupportFragmentManager();
		ViewPagerAdapter adapter = new ViewPagerAdapter(m);
		adapter.addFragment(fragments[0], R.string.action_top_click);
		adapter.addFragment(fragments[1], R.string.action_top_vote);
		adapter.addFragment(fragments[2], R.string.action_changed_lately);
		adapter.addFragment(fragments[3], R.string.action_search);
		viewPager.setAdapter(adapter);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);

		MenuItem searchItem = menu.findItem(R.id.action_search);
		mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		mSearchView.setOnQueryTextListener(this);
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.v("mainactivity","onpause");

		PlayerService thisService = new PlayerService();
		thisService.unbindSafely( this, svcConn );
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.v(TAG, "menu click");

		// check selected menu item
		if (item.getItemId() == R.id.action_refresh) {
			Log.v(TAG, "menu : refresh all");
			FragmentStations fragment = fragments[viewPager.getCurrentItem()];
			fragment.DownloadStations();
			return true;
		}

		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		viewPager.setCurrentItem(3);
		fragments[3].SetDownloadUrl("http://www.radio-browser.info/webservice/json/stations/byname/"+query);
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		return false;
	}
}
