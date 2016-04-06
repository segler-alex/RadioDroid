package net.programmierecke.radiodroid2;

import android.content.pm.PackageInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
		StringBuffer chaine = new StringBuffer("");
		try{
			URL url = new URL(theURI);
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestProperty("User-Agent", "RadioDroid2 ("+BuildConfig.VERSION_NAME+")");
			connection.setRequestMethod("GET");
			connection.setDoInput(true);
			connection.connect();

			InputStream inputStream = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
			String line = "";
			while ((line = rd.readLine()) != null) {
				chaine.append(line);
			}

		} catch (IOException e) {
			// writing exception to log
			e.printStackTrace();
		}

		return chaine.toString();
	}
}
