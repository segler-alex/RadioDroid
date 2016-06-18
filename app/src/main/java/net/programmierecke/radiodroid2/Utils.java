package net.programmierecke.radiodroid2;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import net.programmierecke.radiodroid2.data.DataRadioStation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public class Utils {
	public static String getCacheFile(Context ctx, String theURI) {
		StringBuffer chaine = new StringBuffer("");
		try{
			String aFileName = theURI.toLowerCase().replace("http://","");
			aFileName = aFileName.toLowerCase().replace("https://","");
			aFileName = sanitizeName(aFileName);

			File file = new File(ctx.getCacheDir().getAbsolutePath() + "/"+aFileName);
			Date lastModDate = new Date(file.lastModified());

			Date now = new Date();
			long millis = now.getTime() - file.lastModified();
			long secs = millis / 1000;
			long mins = secs/60;
			long hours = mins/60;

			Log.w("UTIL","File last modified : "+ lastModDate.toString() + " secs="+secs+"  mins="+mins+" hours="+hours);

			if (hours < 1) {
				FileInputStream aStream = new FileInputStream(file);
				BufferedReader rd = new BufferedReader(new InputStreamReader(aStream));
				String line = "";
				while ((line = rd.readLine()) != null) {
					chaine.append(line);
				}
				rd.close();
				Log.w("UTIL", "used cache for:" + theURI);
				return chaine.toString();
			}
			Log.w("UTIL", "do not use cache, because too old:" + theURI);
			return null;
		}
		catch(Exception e){
			Log.e("UTIL","getCacheFile() "+e);
		}
		return null;
	}

	public static void writeFileCache(Context ctx, String theURI, String content){
		try{
			String aFileName = theURI.toLowerCase().replace("http://","");
			aFileName = aFileName.toLowerCase().replace("https://","");
			aFileName = sanitizeName(aFileName);

			File f = new File(ctx.getCacheDir() + "/" + aFileName);
			FileOutputStream aStream = new FileOutputStream(f);
			aStream.write(content.getBytes("utf-8"));
			aStream.close();
		}
		catch(Exception e){
			Log.e("UTIL","writeFileCache() could not write to cache file for:"+theURI);
		}
	}

	public static String downloadFeed(Context ctx, String theURI, boolean forceUpdate, Map<String,String> dictParams) {
		if (!forceUpdate) {
			String cache = getCacheFile(ctx, theURI);
			if (cache != null) {
				return cache;
			}
		}

		StringBuffer chaine = new StringBuffer("");
		try{
			URL url = new URL(theURI);
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setConnectTimeout(4000);
			connection.setReadTimeout(3000);
			connection.setRequestProperty("User-Agent", "RadioDroid2/"+BuildConfig.VERSION_NAME);
			connection.setDoInput(true);
			if (dictParams != null) {
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("Accept", "application/json");
				connection.setRequestMethod("POST");
			} else {
				connection.setRequestMethod("GET");
			}
			connection.connect();

			if (dictParams != null) {
				JSONObject jsonParams = new JSONObject();
				for (String key: dictParams.keySet()){
					jsonParams.put(key, dictParams.get(key));
				}

				OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
				wr.write(jsonParams.toString());
				wr.flush();
			}

			InputStream inputStream = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
			String line = "";
			while ((line = rd.readLine()) != null) {
				chaine.append(line);
			}

			String s = chaine.toString();
			writeFileCache(ctx,theURI,s);
			Log.w("UTIL","wrote cache file for:"+theURI);
			return s;
		} catch (Exception e) {
			Log.e("UTIL","downloadFeed() "+e);
		}

		return null;
	}

	public static String getRealStationLink(Context ctx, String stationId){
		String result = Utils.downloadFeed(ctx, "http://www.radio-browser.info/webservice/json/url/" + stationId, true, null);
		if (result != null) {
			JSONObject jsonObj = null;
			JSONArray jsonArr = null;
			try {
				jsonArr = new JSONArray(result);
				jsonObj = jsonArr.getJSONObject(0);
				return jsonObj.getString("url");
			} catch (Exception e) {
				Log.e("UTIL", "getRealStationLink() " + e);
			}
		}
		return null;
	}

	public static void Play(final DataRadioStation station, final Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		boolean play_external = sharedPref.getBoolean("play_external", false);

		Play(station,context,play_external);
	}

	public static void Play(final DataRadioStation station, final Context context, final boolean external) {
		final ProgressDialog itsProgressLoading = ProgressDialog.show(context, "", context.getResources().getText(R.string.progress_loading));
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return Utils.getRealStationLink(context.getApplicationContext(), station.ID);
			}

			@Override
			protected void onPostExecute(String result) {
				itsProgressLoading.dismiss();

				if (result != null) {
					if (external){
						Intent share = new Intent(Intent.ACTION_VIEW);
						share.setDataAndType(Uri.parse(result), "audio/*");
						context.startActivity(share);
					}else {
						PlayerServiceUtil.play(result, station.Name, station.ID);
					}
				} else {
					Toast toast = Toast.makeText(context.getApplicationContext(), context.getResources().getText(R.string.error_station_load), Toast.LENGTH_SHORT);
					toast.show();
				}
				super.onPostExecute(result);
			}
		}.execute();
	}

	// Storage Permissions
	public static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};

	public static boolean verifyStoragePermissions(Activity activity) {
		// Check if we have write permission
		int permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if (permission != PackageManager.PERMISSION_GRANTED) {
			// We don't have permission so prompt the user
			ActivityCompat.requestPermissions(
					activity,
					PERMISSIONS_STORAGE,
					REQUEST_EXTERNAL_STORAGE
			);
			return false;
		}

		return true;
	}

	public static String getReadableBytes(double bytes){
		String[] str = new String[]{"B","KB","MB","GB","TB"};
		for (int i=0;i<str.length;i++){
			if (bytes < 1024) {
				return String.format(Locale.getDefault(), "%1$,.1f %2$s",bytes,str[i]);
			}
			bytes = bytes/1024;
		}
		return String.format(Locale.getDefault(), "%1$,.1f %2$s",bytes*1024,str[str.length-1]);
	}

	public static String sanitizeName(String str){
		return str.replaceAll("\\W+", "_");
	}
}
