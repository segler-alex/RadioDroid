package net.programmierecke.radiodroid2;

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
