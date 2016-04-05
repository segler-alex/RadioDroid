package net.programmierecke.radiodroid2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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

import android.util.Log;

public class Utils {
	public static RadioStation[] DecodeJson(String result) {
		List<RadioStation> aList = new ArrayList<RadioStation>();
		try {
			JSONArray jsonArray = new JSONArray(result);

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject anObject = jsonArray.getJSONObject(i);

				RadioStation aStation = new RadioStation();
				aStation.ID = anObject.getString("id");
				aStation.Name = anObject.getString("name");
				aStation.StreamUrl = anObject.getString("url");
				aStation.Votes = anObject.getInt("votes");
				aStation.HomePageUrl = anObject.getString("homepage");
				aStation.TagsAll = anObject.getString("tags");
				aStation.Country = anObject.getString("country");
				aStation.State = anObject.getString("state");
				aStation.IconUrl = anObject.getString("favicon");
				aStation.Language = anObject.getString("language");

				aList.add(aStation);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return aList.toArray(new RadioStation[0]);
	}

	public static String downloadFeed(String theURI) {
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
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				Log.e("", "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			Log.e("", "" + e);
		} catch (IOException e) {
			Log.e("", "" + e);
		}
		return builder.toString();
	}
}
