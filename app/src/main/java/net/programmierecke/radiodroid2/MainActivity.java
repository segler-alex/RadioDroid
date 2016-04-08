package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.MenuItemCompat;
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

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
	private String itsAdressWWWTopClick = "http://www.radio-browser.info/webservice/json/stations/topclick/100";
	private String itsAdressWWWTopVote = "http://www.radio-browser.info/webservice/json/stations/topvote/100";
	private String itsAdressWWWChangedLately = "http://www.radio-browser.info/webservice/json/stations/lastchange/100";

	ProgressDialog itsProgressLoading;
	RadioItemBigAdapter itsArrayAdapter = null;

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

	private void RefillList(final String theURL) {
		itsProgressLoading = ProgressDialog.show(MainActivity.this, "", "Loading...");
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return Utils.downloadFeed(theURL);
			}

			@Override
			protected void onPostExecute(String result) {
				if (!isFinishing()) {
					itsArrayAdapter.clear();
					for (RadioStation aStation : Utils.DecodeJson(result)) {
						itsArrayAdapter.add(aStation);
					}
					lv.invalidate();
					itsProgressLoading.dismiss();
				}
				super.onPostExecute(result);
			}
		}.execute();
	}

	private ListView lv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.layout_main);

		Intent anIntent = new Intent(this, PlayerService.class);
		bindService(anIntent, svcConn, BIND_AUTO_CREATE);
		startService(anIntent);

		Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
		setSupportActionBar(myToolbar);

		lv = (ListView) findViewById(R.id.listView);

		// gui stuff
		itsArrayAdapter = new RadioItemBigAdapter(this, R.layout.list_item_big);
		if (lv != null) {
			lv.setAdapter(itsArrayAdapter);

			RefillList(itsAdressWWWTopClick);

			lv.setTextFilterEnabled(true);
			// registerForContextMenu(lv);
			lv.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Object anObject = parent.getItemAtPosition(position);
					if (anObject instanceof RadioStation) {
						ClickOnItem((RadioStation) anObject);
					}
				}
			});
		}
	}

	void ClickOnItem(RadioStation theStation) {
		PlayerService thisService = new PlayerService();
		thisService.unbindSafely( this, svcConn );

		Intent anIntent = new Intent(getBaseContext(), RadioDroidStationDetail.class);
		anIntent.putExtra("stationid", theStation.ID);
		startActivity(anIntent);
	}

	SearchView mSearchView;

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
		if (item.getItemId() == R.id.action_top_vote) {
			Log.v(TAG, "menu : topvote");
			RefillList(itsAdressWWWTopVote);
			setTitle(R.string.action_top_vote);
			return true;
		}
		if (item.getItemId() == R.id.action_top_click) {
			Log.v(TAG, "menu : topclick");
			RefillList(itsAdressWWWTopClick);
			setTitle(R.string.action_top_click);
			return true;
		}
		if (item.getItemId() == R.id.action_changed_lately) {
			Log.v(TAG, "menu : topclick");
			RefillList(itsAdressWWWChangedLately);
			setTitle(R.string.action_changed_lately);
			return true;
		}

		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		RefillList("http://www.radio-browser.info/webservice/json/stations/byname/"+query);
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		return false;
	}
}
