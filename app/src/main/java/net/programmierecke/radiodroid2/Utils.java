package net.programmierecke.radiodroid2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.IIcon;

import net.programmierecke.radiodroid2.data.DataRadioStation;

import net.programmierecke.radiodroid2.data.MPDServer;
import net.programmierecke.radiodroid2.proxy.ProxySettings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

public class Utils {
	private static int loadIcons = -1;

	public static int parseIntWithDefault(String number, int defaultVal) {
		try {
			return Integer.parseInt(number);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	public static String getCacheFile(Context ctx, String theURI) {
		StringBuilder chaine = new StringBuilder("");
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

			if(BuildConfig.DEBUG) { Log.d("UTIL","File last modified : "+ lastModDate.toString() + " secs="+secs+"  mins="+mins+" hours="+hours); }

			if (hours < 1) {
				FileInputStream aStream = new FileInputStream(file);
				BufferedReader rd = new BufferedReader(new InputStreamReader(aStream));
				String line;
				while ((line = rd.readLine()) != null) {
					chaine.append(line);
				}
				rd.close();
				if(BuildConfig.DEBUG) { Log.d("UTIL", "used cache for:" + theURI); }
				return chaine.toString();
			}
			if(BuildConfig.DEBUG) { Log.d("UTIL", "do not use cache, because too old:" + theURI); }
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

	public static String downloadFeed(OkHttpClient httpClient, Context ctx, String theURI, boolean forceUpdate, Map<String,String> dictParams) {
		if (!forceUpdate) {
			String cache = getCacheFile(ctx, theURI);
			if (cache != null) {
				return cache;
			}
		}

		try{
			HttpUrl url = HttpUrl.parse(theURI);
			Request.Builder requestBuilder = new Request.Builder().url(url);

			if(dictParams != null) {
				MediaType jsonMediaType = MediaType.parse("application/json; charset=utf-8");

				Gson gson = new Gson();
				String json = gson.toJson(dictParams);

				okhttp3.RequestBody requestBody = RequestBody.create(jsonMediaType, json);

				requestBuilder.post(requestBody);
			} else {
				requestBuilder.get();
			}

			Request request = requestBuilder.build();
			okhttp3.Response response = httpClient.newCall(request).execute();

			String responseStr = response.body().string();

			writeFileCache(ctx,theURI,responseStr);
			if(BuildConfig.DEBUG) { Log.d("UTIL","wrote cache file for:"+theURI); }
			return responseStr;
		} catch (Exception e) {
			Log.e("UTIL","downloadFeed() "+e);
		}

		return null;
	}

	public static String getRealStationLink(OkHttpClient httpClient, Context ctx, String stationId){
		Log.i("UTIL","StationUUID:" + stationId);
		String result = Utils.downloadFeed(httpClient, ctx, RadioBrowserServerManager.getWebserviceEndpoint(ctx, "v2/json/url/" + stationId), true, null);
		if (result != null) {
			Log.i("UTIL",result);
			JSONObject jsonObj;
			try {
				jsonObj = new JSONObject(result);
				return jsonObj.getString("url");
			} catch (Exception e) {
				Log.e("UTIL", "getRealStationLink() " + e);
			}
		}
		return null;
	}

	public static DataRadioStation getStationByUuid(OkHttpClient httpClient, Context ctx, String stationUuid){
		Log.w("UTIL","Search by uuid:"+stationUuid);
		String result = Utils.downloadFeed(httpClient, ctx, RadioBrowserServerManager.getWebserviceEndpoint(ctx, "json/stations/byuuid/" + stationUuid), true, null);
		if (result != null) {
			try {
				DataRadioStation[] list = DataRadioStation.DecodeJson(result);
				if (list != null) {
					if (list.length == 1) {
						return list[0];
					}
					Log.e("UTIL", "stations by uuid did have length:" + list.length);
				}
			} catch (Exception e) {
				Log.e("UTIL", "getStationByUuid() " + e);
			}
		}
		return null;
	}

	public static void Play(final OkHttpClient httpClient, final DataRadioStation station, final Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		boolean play_external = sharedPref.getBoolean("play_external", false);

		Play(httpClient, station,context,play_external);
	}

	public static void Play(final OkHttpClient httpClient, final DataRadioStation station, final Context context, final boolean external) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		final boolean warn_no_wifi = sharedPref.getBoolean("warn_no_wifi", false);
		if (warn_no_wifi && !Utils.hasWifiConnection(context)) {
			ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
			toneG.startTone(ToneGenerator.TONE_SUP_RADIO_NOTAVAIL, 2000);
			/*Toast.makeText( getBaseContext(), Html.fromHtml( text ), Toast.LENGTH_LONG ).show();
			finish();*/
			Resources res = context.getResources();
			String appName = res.getString(R.string.app_name);
			String title = res.getString(R.string.no_wifi_title);
			String text = String.format(res.getString(R.string.no_wifi_connection),	appName);
			new AlertDialog.Builder(context)
					.setTitle(title)
					.setMessage(text)
					.setNegativeButton(android.R.string.cancel, null) // do not play on cancel
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override public void onClick(DialogInterface dialog, int which) {
							playInternal(httpClient, station, context, external);
						}
					})
					.create()
					.show();
		} else {
			playInternal(httpClient, station, context, external);
		}
	}

	private static void playInternal(final OkHttpClient httpClient, final DataRadioStation station, final Context context, final boolean external) {
        context.sendBroadcast(new Intent(ActivityMain.ACTION_SHOW_LOADING));
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return Utils.getRealStationLink(httpClient, context.getApplicationContext(), station.StationUuid);
			}

			@Override
			protected void onPostExecute(String result) {
                context.sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));

				if (result != null) {
					boolean externalActive = false;
					if (MPDClient.Connected() && MPDClient.Discovered()){
						MPDClient.Play(result, context);
						PlayerServiceUtil.saveInfo(result, station.Name, station.StationUuid, station.IconUrl);
						externalActive = true;
					}
					if (CastHandler.isCastSessionAvailable()){
						if (!externalActive) {
							PlayerServiceUtil.stop(); // stop internal player and not continue playing
						}
						CastHandler.PlayRemote(station.Name, result, station.IconUrl);
						externalActive = true;
					}

					if (!externalActive){
						if (external){
							Intent share = new Intent(Intent.ACTION_VIEW);
							share.setDataAndType(Uri.parse(result), "audio/*");
							context.startActivity(share);
						}else {
							PlayerServiceUtil.play(result, station.Name, station.StationUuid, station.IconUrl);
						}
					}
				} else {
					Toast toast = Toast.makeText(context.getApplicationContext(), context.getResources().getText(R.string.error_station_load), Toast.LENGTH_SHORT);
					toast.show();
				}
				super.onPostExecute(result);
			}
		}.execute();
	}

	public static boolean shouldLoadIcons(final Context context) {
		switch(loadIcons) {
			case -1:
				if(PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getBoolean("load_icons", false)) {
					loadIcons = 1;
					return true;
				} else {
					loadIcons = 0;
					return true;
				}
			case 0:
				return false;
			case 1:
				return true;
		}
		return false;
	}

	public static String getTheme(final Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString("theme_name", context.getResources().getString(R.string.theme_light));
	}

    public static int getThemeResId(final Context context) {
        String selectedTheme = getTheme(context);
	    if(selectedTheme.equals(context.getResources().getString(R.string.theme_dark)))
            return R.style.MyMaterialTheme_Dark;
	    else
	        return R.style.MyMaterialTheme;
    }

    public static int getTimePickerThemeResId(final Context context) {
        int theme;
        if(getThemeResId(context) == R.style.MyMaterialTheme_Dark)
            theme = R.style.DialogTheme_Dark;
        else
            theme = R.style.DialogTheme;
        return theme;
    }

    public static boolean useCircularIcons(final Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("circular_icons", false);
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
		for (String aStr : str) {
			if (bytes < 1024) {
				return String.format(Locale.getDefault(), "%1$,.1f %2$s", bytes, aStr);
			}
			bytes = bytes / 1024;
		}
		return String.format(Locale.getDefault(), "%1$,.1f %2$s",bytes*1024,str[str.length-1]);
	}

	public static String sanitizeName(String str) {
		return str.replaceAll("\\W+", "_").replaceAll("^_+", "").replaceAll("_+$", "");
	}

	public static boolean hasWifiConnection(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		return mWifi.isConnected();
	}

	public static boolean hasAnyConnection(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connManager.getActiveNetworkInfo();
		//should check null because in airplane mode it will be null
		return (netInfo != null && netInfo.isConnected());
	}

	public static List<MPDServer> getMPDServers(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		String serversFromPrefs = sharedPref.getString("mpd_servers", "");
		Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<MPDServer>>(){}.getType();
		List <MPDServer> serversList = gson.fromJson(serversFromPrefs, type);
		return serversList != null? serversList : new ArrayList<MPDServer>();
	}

    public static void saveMPDServers(List<MPDServer> servers, Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = new Gson();
        String serversJson = gson.toJson(servers);
        editor.putString("mpd_servers", serversJson);
        editor.commit();
    }

    public static boolean bottomNavigationEnabled(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("bottom_navigation", true);
    }

    public static String formatStringWithNamedArgs(String format, Map<String, String> args) {
	    StringBuilder builder = new StringBuilder(format);
		for (Map.Entry<String, String> entry : args.entrySet()) {
		    final String key = "${" + entry.getKey() + "}";
		    int startIdx = 0;
		    while (true) {
                final int keyIdx = builder.indexOf(key, startIdx);

                if (keyIdx == -1) {
                    break;
                }

                builder.replace(keyIdx, keyIdx + key.length(), entry.getValue());
                startIdx = keyIdx + entry.getValue().length();
            }
		}

		return builder.toString();
	}

    public static int themeAttributeToColor(int themeAttributeId, Context context, int fallbackColorId) {
        TypedValue outValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        boolean wasResolved = theme.resolveAttribute(themeAttributeId, outValue, true);
        if (wasResolved) {
            return outValue.resourceId == 0 ? outValue.data : ContextCompat.getColor(context, outValue.resourceId);
        } else {
            return fallbackColorId;
        }
    }

    public static int getIconColor(Context context) {
        return themeAttributeToColor(R.attr.menuTextColorDefault, context, Color.LTGRAY);
    }

	public static int getAccentColor(Context context) {
		return themeAttributeToColor(R.attr.colorAccent, context, Color.LTGRAY);
	}

	public static void setOkHttpProxy(@NonNull OkHttpClient.Builder builder, @NonNull final ProxySettings proxySettings) {
		if (TextUtils.isEmpty(proxySettings.host)) {
			return;
		}

		if (proxySettings.type == Proxy.Type.DIRECT) {
			return;
		}

		InetSocketAddress proxyAddress = InetSocketAddress.createUnresolved(proxySettings.host, proxySettings.port);
		Proxy proxy = new Proxy(proxySettings.type, proxyAddress);

		builder.proxy(proxy);

		if (!proxySettings.login.isEmpty()) {
			Authenticator proxyAuthenticator = new Authenticator() {
				@Override
				public Request authenticate(Route route, Response response) throws IOException {
					String credential = Credentials.basic(proxySettings.login, proxySettings.password);
					return response.request().newBuilder()
							.header("Proxy-Authorization", credential)
							.build();
				}
			};

			builder.authenticator(proxyAuthenticator);
		}
	}

    public static Uri resourceToUri(Resources resources, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                resources.getResourcePackageName(resID) + '/' +
                resources.getResourceTypeName(resID) + '/' +
                resources.getResourceEntryName(resID));
    }

    public static IconicsDrawable IconicsIcon(Context context, IIcon icon) {
        return new IconicsDrawable(context, icon).size(IconicsSize.TOOLBAR_ICON_SIZE).padding(IconicsSize.TOOLBAR_ICON_PADDING).color(IconicsColor.colorInt(getIconColor(context)));
    }
}
