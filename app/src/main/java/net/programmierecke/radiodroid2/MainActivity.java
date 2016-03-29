package net.programmierecke.radiodroid2;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MainActivity extends ListActivity {
	private String itsAdressWWWTopClick = "http://www.radio-browser.info/webservice/json/stations/topclick/100";
	private String itsAdressWWWTopVote25 = "http://www.radio-browser.info/webservice/json/stations/topvote/100";
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
					getListView().invalidate();
					itsProgressLoading.dismiss();
				}
				super.onPostExecute(result);
			}
		}.execute();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent anIntent = new Intent(this, PlayerService.class);
		bindService(anIntent, svcConn, BIND_AUTO_CREATE);
		startService(anIntent);

		// gui stuff
		itsArrayAdapter = new RadioItemBigAdapter(this, R.layout.list_item_big);
		setListAdapter(itsArrayAdapter);

		RefillList(itsAdressWWWTopClick);

		ListView lv = getListView();
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

	void ClickOnItem(RadioStation theStation) {
		Intent anIntent = new Intent(getBaseContext(), RadioDroidStationDetail.class);
		anIntent.putExtra("stationid", theStation.ID);
		startActivity(anIntent);

		// if (itsPlayerService != null) {
		// try {
		// itsPlayerService.Play(aStation.StreamUrl, aStation.Name, aStation.ID);
		// } catch (RemoteException e) {
		// // TODO Auto-generated catch block
		// Log.e(TAG, "" + e);
		// }
		// } else {
		// Log.v(TAG, "SERVICE NOT ONLINE");
		// }
	}

	final int MENU_STOP = 0;
	final int MENU_TOPVOTE = 1;
	final int MENU_TOPCLICK = 2;
	final int MENU_LAST_CHANGED = 3;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_STOP, Menu.NONE, "Stop");
		menu.add(Menu.NONE, MENU_TOPVOTE, Menu.NONE, "TopVote");
		menu.add(Menu.NONE, MENU_TOPCLICK, Menu.NONE, "TopClick");
		menu.add(Menu.NONE, MENU_LAST_CHANGED, Menu.NONE, "Changed lately");

		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.v(TAG, "menu click");

		if (item.getItemId() == MENU_STOP) {
			Log.v(TAG, "menu : stop");
			try {
				itsPlayerService.Stop();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "" + e);
			}
			return true;
		}
		// check selected menu item
		if (item.getItemId() == MENU_TOPVOTE) {
			Log.v(TAG, "menu : topvote");
			RefillList(itsAdressWWWTopVote25);
			setTitle("TopVote");
			return true;
		}
		if (item.getItemId() == MENU_TOPCLICK) {
			Log.v(TAG, "menu : topclick");
			RefillList(itsAdressWWWTopClick);
			setTitle("TopClick");
			return true;
		}
		if (item.getItemId() == MENU_LAST_CHANGED) {
			Log.v(TAG, "menu : topclick");
			RefillList(itsAdressWWWChangedLately);
			setTitle("Changed lately");
			return true;
		}

		return false;
	}
}
