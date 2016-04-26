package net.programmierecke.radiodroid2;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
	//private TabLayout tabLayout;
	//private ViewPager viewPager;
	private SearchView mSearchView;

	private static final String TAG = "RadioDroid";
	private IPlayerService itsPlayerService;
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

	DrawerLayout mDrawerLayout;
	NavigationView mNavigationView;
	FragmentManager mFragmentManager;
	FragmentTransaction mFragmentTransaction;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_main);

		Intent anIntent = new Intent(this, PlayerService.class);
		bindService(anIntent, svcConn, BIND_AUTO_CREATE);
		startService(anIntent);

		Toolbar myToolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(myToolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		//viewPager = (ViewPager) findViewById(R.id.viewpager);

		//tabLayout = (TabLayout) findViewById(R.id.tabs);
		//tabLayout.setupWithViewPager(viewPager);

		/**
		 *Setup the DrawerLayout and NavigationView
		 */

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
		mNavigationView = (NavigationView) findViewById(R.id.shitstuff) ;

		/**
		 * Lets inflate the very first fragment
		 * Here , we are inflating the TabFragment as the first Fragment
		 */

		mFragmentManager = getSupportFragmentManager();
		mFragmentTransaction = mFragmentManager.beginTransaction();
		mFragmentTransaction.replace(R.id.containerView,new FragmentTabs()).commit();
		/**
		 * Setup click events on the Navigation View Items.
		 */

		mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(MenuItem menuItem) {
				mDrawerLayout.closeDrawers();

				if (menuItem.getItemId() == R.id.nav_item_about) {
					FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
					fragmentTransaction.replace(R.id.containerView,new FragmentAbout()).commit();

				}

				if (menuItem.getItemId() == R.id.nav_item_stations) {
					FragmentTransaction xfragmentTransaction = mFragmentManager.beginTransaction();
					xfragmentTransaction.replace(R.id.containerView,new FragmentTabs()).commit();
				}

				return false;
			}

		});

		/**
		 * Setup Drawer Toggle of the Toolbar
		 */

		//myToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.main_toolbar);
		ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout, R.string.app_name,R.string.app_name);

		mDrawerLayout.addDrawerListener(mDrawerToggle);

		mDrawerToggle.syncState();

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

	/*@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.v(TAG, "menu click");

		// check selected menu item
		if (item.getItemId() == R.id.action_refresh) {
			Log.v(TAG, "menu : refresh all");
			FragmentBase fragment = fragments[viewPager.getCurrentItem()];
			fragment.DownloadUrl();
			return true;
		}

		return false;
	}

	public void Search(String query){
		viewPager.setCurrentItem(7);
		fragments[7].SetDownloadUrl(query);
	}*/

	@Override
	public boolean onQueryTextSubmit(String query) {
		String queryEncoded = null;
		try {
			queryEncoded = URLEncoder.encode(query, "utf-8");
			//Search("http://www.radio-browser.info/webservice/json/stations/byname/"+queryEncoded);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		return false;
	}
}
