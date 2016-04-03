package net.programmierecke.radiodroid2;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
		public ImageView itsImageView;

		public QueueItem(String theURL, ImageView theImageView) {
			itsURL = theURL;
			itsImageView = theImageView;
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

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) itsContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.list_item_big, null);
		}
		RadioStation aStation = getItem(position);
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

			// new DownloadImageTask(anImageView).execute(aStation.IconUrl);
			if (itsIconCache.containsKey(aStation.IconUrl)) {
				Bitmap aBitmap = itsIconCache.get(aStation.IconUrl);
				if (aBitmap != null) {
					anImageView.setVisibility(View.VISIBLE);
					anImageView.setImageBitmap(aBitmap);
				}
				else
					anImageView.setVisibility(View.GONE);
					//anImageView.setImageResource(R.drawable.empty);
			} else {
				try {
					// check download cache
					String aFileNameIcon = getBase64(aStation.IconUrl);
					Bitmap anIcon = BitmapFactory.decodeStream(itsContext.openFileInput(aFileNameIcon));
					anImageView.setVisibility(View.VISIBLE);
					anImageView.setImageBitmap(anIcon);
					itsIconCache.put(aStation.IconUrl, anIcon);
				} catch (Exception e) {
					try {
						//anImageView.setImageResource(R.drawable.empty);
						anImageView.setVisibility(View.GONE);
						itsQueuedDownloadJobs.put(new QueueItem(aStation.IconUrl, anImageView));
					} catch (InterruptedException e2) {
						Log.e("Error", "" + e2);
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
						InputStream in = new java.net.URL(anItem.itsURL).openStream();
						final Bitmap anIcon = BitmapFactory.decodeStream(in);
						itsIconCache.put(anItem.itsURL, anIcon);

						anItem.itsImageView.post(new Runnable() {
							public void run() {
								if (anIcon != null) {
									// set image in view
									anItem.itsImageView.setImageBitmap(anIcon);
									anItem.itsImageView.setVisibility(View.VISIBLE);

									// save image to file
									String aFileName = getBase64(anItem.itsURL);
									Log.v("", "" + anItem.itsURL + "->" + aFileName);
									try {
										FileOutputStream aStream = itsContext.openFileOutput(aFileName, Context.MODE_PRIVATE);
										anIcon.compress(Bitmap.CompressFormat.PNG, 100, aStream);
										aStream.close();
									} catch (FileNotFoundException e) {
										Log.e("", "" + e);
									} catch (IOException e) {
										Log.e("", "" + e);
									}
								}
							}
						});
					}
				} catch (Exception e) {
					Log.e("Error", "" + e);
					itsIconCache.put(anItem.itsURL, null);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.e("Error", "" + e);
			}
		}
	}

	public String getBase64(String theOriginal) {
		return Base64.encodeToString(theOriginal.getBytes(), Base64.URL_SAFE | Base64.NO_PADDING);
	}
}
