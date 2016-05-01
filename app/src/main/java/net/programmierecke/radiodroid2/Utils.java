package net.programmierecke.radiodroid2;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Utils {
	public static String downloadFeed(String theURI) {
		StringBuffer chaine = new StringBuffer("");
		try{
			URL url = new URL(theURI);
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setConnectTimeout(4000);
			connection.setReadTimeout(3000);
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

			return chaine.toString();
		} catch (Exception e) {
			Log.e("UTILS",""+e);
		}

		return null;
	}

	public static String getRealStationLink(String stationId){
		String result = Utils.downloadFeed("http://www.radio-browser.info/webservice/json/url/" + stationId);
		if (result != null) {
			JSONObject jsonObj = null;
			JSONArray jsonArr = null;
			try {
				jsonArr = new JSONArray(result);
				jsonObj = jsonArr.getJSONObject(0);
				return jsonObj.getString("url");
			} catch (Exception e) {
				Log.e("UTILS", "" + e);
			}
		}
		return null;
	}
}
