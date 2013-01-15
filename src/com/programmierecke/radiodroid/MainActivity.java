package com.programmierecke.radiodroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
					Log.d("", result);

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
							itsMediaPlayer.stop();
							itsMediaPlayer.reset();
							try {
								itsMediaPlayer
										.setDataSource(aStation.StreamUrl);
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
								Log.d("", "prepare ok");

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
			Log.v("", "Found entries:" + jsonArray.length());

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject anObject = jsonArray.getJSONObject(i);
				Log.v("", "found station:" + anObject.getString("name"));

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
				}
			} else {
				Log.e("tag", "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
}
