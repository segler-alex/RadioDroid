package com.programmierecke.radiodroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends ListActivity {
	private String itsAdressWWWTop25 = "http://www.radio-browser.info/webservice/json/stations/topvote/25";
	ProgressDialog itsProgressLoading;
	ArrayAdapter<RadioStation> itsArrayAdapter = null;
	MediaPlayer itsMediaPlayer = new MediaPlayer();

	private static final String TAG = "RadioDroid";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_main);

		// gui stuff
		itsArrayAdapter = new ArrayAdapter<RadioStation>(this,
				R.layout.list_item);
		setListAdapter(itsArrayAdapter);

		itsProgressLoading = ProgressDialog.show(MainActivity.this, "",
				"Loading...");

		// new DownloadFilesTask().execute(AdressWWWTop5);
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return downloadFeed(itsAdressWWWTop25);
			}

			@Override
			protected void onPostExecute(String result) {
				if (!isFinishing()) {
					Log.d(TAG, result);

					DecodeJson(result);
					itsProgressLoading.dismiss();
				}
				super.onPostExecute(result);
			}
		}.execute();

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		registerForContextMenu(lv);
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, show a toast with the TextView text
				Object anObject = parent.getItemAtPosition(position);
				if (anObject instanceof RadioStation) {
					RadioStation aStation = (RadioStation) anObject;
					itsProgressLoading = ProgressDialog.show(MainActivity.this,
							"", "Loading...");

					new AsyncTask<RadioStation, Void, Void>() {

						@Override
						protected Void doInBackground(RadioStation... stations) {

							if (stations.length != 1)
								return null;

							RadioStation aStation = stations[0];

							Log.v(TAG, "Stream url:" + aStation.StreamUrl);

							String aDecodedURL = DecodeURL(aStation.StreamUrl);

							Log.v(TAG, "Stream url decoded:" + aDecodedURL);

							itsMediaPlayer.stop();
							itsMediaPlayer.reset();
							try {
								itsMediaPlayer.setDataSource(aDecodedURL);
								itsMediaPlayer.prepare();
								itsMediaPlayer.start();
							} catch (IllegalArgumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SecurityException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalStateException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							return null;
						}

						@Override
						protected void onPostExecute(Void result) {
							if (!isFinishing()) {
								Log.d(TAG, "prepare ok");

								itsProgressLoading.dismiss();
							}
							super.onPostExecute(result);
						}

					}.execute(aStation);

				}
			}
		});

	}

	protected void DecodeJson(String result) {
		try {
			JSONArray jsonArray = new JSONArray(result);
			Log.v(TAG, "Found entries:" + jsonArray.length());

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject anObject = jsonArray.getJSONObject(i);
				Log.v(TAG, "found station:" + anObject.getString("name"));

				RadioStation aStation = new RadioStation();
				aStation.Name = anObject.getString("name");
				aStation.StreamUrl = anObject.getString("url");
				aStation.Votes = anObject.getInt("votes");

				itsArrayAdapter.add(aStation);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public String downloadFeed(String theURI) {
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(theURI);
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append('\n');
				}
			} else {
				Log.e(TAG, "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, "" + e);
		} catch (IOException e) {
			Log.e(TAG, "" + e);
		}
		return builder.toString();
	}

	String DecodeURL(String theUrl) {
		try {
			URL anUrl = new URL(theUrl);
			String aFileName = anUrl.getFile();
			if (aFileName.endsWith(".pls")) {
				Log.v(TAG, "Found PLS file");
				String theFile = downloadFeed(theUrl);
				BufferedReader aReader = new BufferedReader(new StringReader(
						theFile));
				String str;
				while ((str = aReader.readLine()) != null) {
					Log.e(TAG, " -> " + str);
					if (str.substring(0, 4).equals("File")) {
						int anIndex = str.indexOf('=');
						if (anIndex >= 0) {
							return str.substring(anIndex + 1);
						}
					}
				}
			} else if (aFileName.endsWith(".m3u")) {
				Log.v(TAG, "Found M3U file");
				String theFile = downloadFeed(theUrl);
				BufferedReader aReader = new BufferedReader(new StringReader(
						theFile));
				String str;
				while ((str = aReader.readLine()) != null) {
					Log.e(TAG, " -> " + str);
					if (!str.substring(0, 1).equals("#")) {
						return str.trim();
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "" + e);
		}
		return theUrl;
	}
}
