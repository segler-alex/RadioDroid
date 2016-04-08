package net.programmierecke.radiodroid2;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class RadioItemBigAdapter extends ArrayAdapter<RadioStation> implements Runnable {
	public class QueueItem {
		public String itsURL;

		public QueueItem(String theURL, ImageView theImageView) {
			itsURL = theURL;
		}
	}

	HashMap<String, Bitmap> itsIconCache = new HashMap<String, Bitmap>();
	BlockingQueue<QueueItem> itsQueuedDownloadJobs = new ArrayBlockingQueue<QueueItem>(1000);
	Thread itsThread;

	public RadioItemBigAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
		itsContext = context;
		itsThread = new Thread(this);
		itsThread.start();
	}

	Context itsContext;

	class MyItem{
		public WeakReference<View> v = null;
		public RadioStation station;
		public int position;
		public String GetHash(){
			if (TextUtils.isGraphic(station.StreamUrl)) {
				return getBase64(station.StreamUrl);
			}
			return null;
		}

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
								Log.w("ok","replaced icon:"+station.Name);
							}
						});
					}else{
						Log.w("","vhard == null");
					}
				}else{
					Log.w("","v == null");
				}
			}else{
				Log.w("","icon == null");
			}
		}
	}
	ArrayList<MyItem> listViewItems = new ArrayList<MyItem>();

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RadioStation aStation = getItem(position);

		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) itsContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.list_item_big, null);

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
			TextView aTextViewTop = (TextView) v.findViewById(R.id.textViewTop);
			TextView aTextViewBottom = (TextView) v.findViewById(R.id.textViewBottom);
			if (aTextViewTop != null) {
				aTextViewTop.setText("" + aStation.Name);
			}
			if (aTextViewBottom != null) {
				aTextViewBottom.setText("" + aStation.getShortDetails());
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
					Log.v("ICONLOAD", "--URL=" + aStation.IconUrl);
					if (TextUtils.isGraphic(aStation.IconUrl)) {
						String aFileNameIcon = getBase64(aStation.IconUrl);
						Bitmap anIcon = BitmapFactory.decodeStream(itsContext.openFileInput(aFileNameIcon));
						anImageView.setVisibility(View.VISIBLE);
						anImageView.setImageBitmap(anIcon);
						itsIconCache.put(aStation.IconUrl, anIcon);
					}
				} catch (Exception e) {
					try {
						anImageView.setVisibility(View.GONE);
						itsQueuedDownloadJobs.put(new QueueItem(aStation.IconUrl, null));
					} catch (InterruptedException e2) {
						Log.e("Errorabc", "" + e2.getStackTrace());
					}
				}
			}
		}
		return v;
	}

	@Override
	public void run() {
		while (true) {
			try {
				final QueueItem anItem = itsQueuedDownloadJobs.take();
				try {
					if (!itsIconCache.containsKey(anItem.itsURL)) {
						// load image from url
						InputStream in = new java.net.URL(anItem.itsURL).openStream();
						final Bitmap anIcon = BitmapFactory.decodeStream(in);
						itsIconCache.put(anItem.itsURL, anIcon);

						// save image to file
						String aFileName = getBase64(anItem.itsURL);
						Log.v("", "" + anItem.itsURL + "->" + aFileName);
						try {
							FileOutputStream aStream = itsContext.openFileOutput(aFileName, Context.MODE_PRIVATE);
							anIcon.compress(Bitmap.CompressFormat.PNG, 100, aStream);
							aStream.close();
						} catch (FileNotFoundException e) {
							Log.e("", "my1" + e);
						} catch (IOException e) {
							Log.e("", "my2" + e);
						}

						for (int i=0;i< listViewItems.size();i++){
							MyItem item = listViewItems.get(i);
							if (item.station != null) {
								if (item.station.IconUrl != null) {
									if (item.station.IconUrl.equals(anItem.itsURL)) {
										Log.d("", "Refresh icon:"+anItem.itsURL);
										item.SetIcon(anIcon);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					Log.e("Error3", "Could not load "+anItem.itsURL+" " + e);
					itsIconCache.put(anItem.itsURL, null);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.e("Error4", "" + e);
			}
		}
	}

	public String getBase64(String theOriginal) {
		return Base64.encodeToString(theOriginal.getBytes(), Base64.URL_SAFE | Base64.NO_PADDING);
	}
}
