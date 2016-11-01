package net.programmierecke.radiodroid2.adapters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import net.programmierecke.radiodroid2.data.DataRadioStation;
import net.programmierecke.radiodroid2.FavouriteManager;
import net.programmierecke.radiodroid2.FragmentStarred;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioAlarmManager;
import net.programmierecke.radiodroid2.TimePickerFragment;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.interfaces.IAdapterRefreshable;

public class ItemAdapterStation extends ArrayAdapter<DataRadioStation> implements Runnable, TimePickerDialog.OnTimeSetListener {
	private ProgressDialog itsProgressLoading;
	IAdapterRefreshable refreshable;

	final String TAG = "AdapterStations";

	public void setUpdate(FragmentStarred refreshableList) {
		refreshable = refreshableList;
	}

	public class QueueItem {
		public String itsURL;
		public String ID;

		public QueueItem(String ID, String theURL, ImageView theImageView) {
			itsURL = theURL;
			this.ID = ID;
		}
	}

	HashMap<String, Bitmap> itsIconCache = new HashMap<String, Bitmap>();
	BlockingQueue<QueueItem> itsQueuedDownloadJobs = new ArrayBlockingQueue<QueueItem>(1000);
	Thread itsThread;

	public ItemAdapterStation(FragmentActivity context, int textViewResourceId) {
		super(context, textViewResourceId);
		activity = context;
		itsThread = new Thread(this);
		itsThread.start();
	}

	FragmentActivity activity;
	DataRadioStation itsStation;

	class MyItem{
		public WeakReference<View> v = null;
		public DataRadioStation station;
		public int position;

		public void SetIcon(final Bitmap anIcon) {
			if (anIcon != null) {
				if (v != null) {
					final View vHard = v.get();
					if (vHard != null) {
						vHard.post(new Runnable() {
							public void run() {
								final ImageView anImageView = (ImageView) vHard.findViewById(R.id.imageViewIcon);

								// set image in view
								anImageView.setImageBitmap(anIcon);
								anImageView.setVisibility(View.VISIBLE);
								Log.w("ICONS","replaced icon:"+station.Name);
							}
						});
					}else{
						Log.w("ICONS","vhard == null");
					}
				}else{
					Log.w("ICONS","v == null");
				}
			}else{
				Log.w("ICONS","icon == null");
			}
		}
	}
	ArrayList<MyItem> listViewItems = new ArrayList<MyItem>();

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final DataRadioStation aStation = getItem(position);

		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.list_item_station, null);

			MyItem item = new MyItem();
			item.v = new WeakReference<View>(v);
			item.position = position;
			item.station = aStation;
			listViewItems.add(item);
		}else {
			for (int i = 0; i < listViewItems.size(); i++) {
				MyItem item = listViewItems.get(i);
				View ref = item.v.get();
				if (ref != null) {
					if (convertView == ref) {
						item.station = aStation;
						item.position = position;
						break;
					}
				}
			}
		}

		if (aStation != null) {
			ImageButton buttonMore = (ImageButton) v.findViewById(R.id.buttonMore);
			buttonMore.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					showMenu(aStation, view);
				}
			});
			TextView aTextViewTop = (TextView) v.findViewById(R.id.textViewTop);
			TextView aTextViewBottom = (TextView) v.findViewById(R.id.textViewBottom);
			if (aTextViewTop != null) {
				aTextViewTop.setText("" + aStation.Name);
			}
			if (aTextViewBottom != null) {
				aTextViewBottom.setText("" + aStation.getShortDetails(getContext()));
			}
			ImageView anImageView = (ImageView) v.findViewById(R.id.imageViewIcon);

			if (itsIconCache.containsKey(aStation.IconUrl)) {
				Bitmap aBitmap = itsIconCache.get(aStation.IconUrl);
				if (aBitmap != null) {
					anImageView.setVisibility(View.VISIBLE);
					anImageView.setImageBitmap(aBitmap);
				}
				else
					anImageView.setVisibility(View.GONE);
			} else {
				try {
					// check download cache
					Log.v("ICONS", "check cache for " + aStation.IconUrl);
					if (TextUtils.isGraphic(aStation.IconUrl)) {
						String aFileNameIcon = activity.getCacheDir().getAbsolutePath() + "/" + Utils.sanitizeName(aStation.IconUrl) + ".dat";
						File f = new File(aFileNameIcon);
						Bitmap anIcon = BitmapFactory.decodeStream(new FileInputStream(f));
						itsIconCache.put(aStation.IconUrl, anIcon);
						if (anIcon != null) {
							anImageView.setImageBitmap(anIcon);
							anImageView.setVisibility(View.VISIBLE);
						}else{
							anImageView.setVisibility(View.GONE);
						}
					}else{
						anImageView.setVisibility(View.GONE);
					}
				} catch (Exception e) {
					try {
						anImageView.setVisibility(View.GONE);
						itsQueuedDownloadJobs.put(new QueueItem(aStation.ID, aStation.IconUrl, null));
					} catch (InterruptedException e2) {
						Log.e("ICONS", "" + e2.getStackTrace());
					}
				}
			}
		}
		return v;
	}

	void showMenu(final DataRadioStation station, View view)
	{
		FavouriteManager fm = new FavouriteManager(activity.getApplicationContext());

		PopupMenu popup = new PopupMenu(getContext(), view);
		popup.inflate(R.menu.menu_station_detail);

		Menu m = popup.getMenu();
		boolean isStarred = fm.has(station.ID);
		m.findItem(R.id.action_star).setVisible(!isStarred);
		m.findItem(R.id.action_unstar).setVisible(isStarred);

		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {

				switch (item.getItemId()) {
					case R.id.action_share:
						Share(station);
						return true;

					case R.id.action_homepage:
						ShowHomepage(station);
						return true;

					case R.id.action_star:
						Star(station);
						return true;

					case R.id.action_unstar:
						UnStar(station);
						return true;

					case R.id.action_set_alarm:
						setAsAlarm(station);
						return true;

					case R.id.action_play:
						Utils.Play(station, getContext());
						return true;

					default:
						return false;
				}
			}
		});
		popup.show();
	}

	private void ShowHomepage(DataRadioStation station) {
		Intent share = new Intent(Intent.ACTION_VIEW);
		share.setData(Uri.parse(station.HomePageUrl));
		getContext().startActivity(share);
	}

	private void Star(DataRadioStation station) {
		if (station != null) {
			FavouriteManager fm = new FavouriteManager(getContext().getApplicationContext());
			fm.add(station);
			Vote(station.ID);
		}else{
			Log.e(TAG,"empty station info");
		}
	}

	private void Vote(final String stationID){
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return Utils.downloadFeed(activity,"http://www.radio-browser.info/webservice/json/vote/"+stationID,true, null);
			}

			@Override
			protected void onPostExecute(String result) {
				Log.i(TAG,result);
				super.onPostExecute(result);
			}
		}.execute();
	}

	private void UnStar(DataRadioStation station) {
		if (station != null) {
			FavouriteManager fm = new FavouriteManager(getContext().getApplicationContext());
			fm.remove(station.ID);
			if (refreshable != null) {
				refreshable.RefreshListGui();
			}
		}else{
			Log.e(TAG,"empty station info");
		}
	}

	void setAsAlarm(DataRadioStation station){
		Log.w(TAG,"setAsAlarm() 1");
		if (station != null) {
			itsStation = station;
			Log.w(TAG,"setAsAlarm() 2");
			TimePickerFragment newFragment = new TimePickerFragment();
			newFragment.setCallback(this);
			newFragment.show(activity.getSupportFragmentManager(), "timePicker");
		}
	}

	private void Share(final DataRadioStation station) {
		itsProgressLoading = ProgressDialog.show(getContext(), "", getContext().getResources().getString(R.string.progress_loading));
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return Utils.getRealStationLink(getContext().getApplicationContext(), station.ID);
			}

			@Override
			protected void onPostExecute(String result) {
				itsProgressLoading.dismiss();

				if (result != null) {
					Intent share = new Intent(Intent.ACTION_VIEW);
					share.setDataAndType(Uri.parse(result), "audio/*");
					String title = getContext().getResources().getString(R.string.share_action);
					Intent chooser = Intent.createChooser(share, title);

					if (share.resolveActivity(getContext().getPackageManager()) != null) {
						getContext().startActivity(chooser);
					}
				} else {
					Toast toast = Toast.makeText(getContext().getApplicationContext(), getContext().getResources().getText(R.string.error_station_load), Toast.LENGTH_SHORT);
					toast.show();
				}
				super.onPostExecute(result);
			}
		}.execute();
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		Log.w(TAG,"onTimeSet() "+hourOfDay);
		RadioAlarmManager ram = new RadioAlarmManager(getContext().getApplicationContext(),null);
		ram.add(itsStation,hourOfDay,minute);
	}

	@Override
	public void run() {
		while (true) {
			try {
				final QueueItem anItem = itsQueuedDownloadJobs.take();
				try {
					if (!itsIconCache.containsKey(anItem.itsURL)) {
						// load image from url
						Log.v("ICONS", "download from " + anItem.itsURL);
						InputStream in = new java.net.URL(anItem.itsURL).openStream();
						final Bitmap anIcon = BitmapFactory.decodeStream(in);
						itsIconCache.put(anItem.itsURL, anIcon);

						if (anIcon != null) {
							// save image to file
							String aFileName = activity.getCacheDir().getAbsolutePath() + "/" + Utils.sanitizeName(anItem.itsURL) + ".dat";
							File f = new File(aFileName);

							Log.v("ICONS", "download finished " + anItem.itsURL + " -> "+aFileName);
							try {
								FileOutputStream aStream = new FileOutputStream(f);
								anIcon.compress(Bitmap.CompressFormat.PNG, 100, aStream);
								aStream.close();
							} catch (FileNotFoundException e) {
								Log.e("ICONS", "my1" + e);
							} catch (IOException e) {
								Log.e("ICONS", "my2" + e);
							}

							for (int i = 0; i < listViewItems.size(); i++) {
								MyItem item = listViewItems.get(i);
								if (item.station != null) {
									if (item.station.IconUrl != null) {
										if (item.station.IconUrl.equals(anItem.itsURL)) {
											Log.d("ICONS", "refresh icon " + anItem.itsURL);
											item.SetIcon(anIcon);
										}
									}
								}
							}
						}
					}
				} catch (Exception e) {
					Log.e("ICONS", "Could not load "+anItem.itsURL+" " + e);
					itsIconCache.put(anItem.itsURL, null);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.e("ICONS", "" + e);
			}
		}
	}
}
