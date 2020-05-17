package net.programmierecke.radiodroid2;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.IIcon;

import net.programmierecke.radiodroid2.players.selector.PlayerSelectorDialog;
import net.programmierecke.radiodroid2.players.selector.PlayerType;
import net.programmierecke.radiodroid2.service.ConnectivityChecker;
import net.programmierecke.radiodroid2.service.PlayerServiceUtil;
import net.programmierecke.radiodroid2.station.DataRadioStation;

import net.programmierecke.radiodroid2.proxy.ProxySettings;
import net.programmierecke.radiodroid2.utils.Tls12SocketFactory;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Authenticator;
import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.TlsVersion;

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
        try {
            String aFileName = theURI.toLowerCase().replace("http://", "");
            aFileName = aFileName.toLowerCase().replace("https://", "");
            aFileName = sanitizeName(aFileName);

            File file = new File(ctx.getCacheDir().getAbsolutePath() + "/" + aFileName);
            Date lastModDate = new Date(file.lastModified());

            Date now = new Date();
            long millis = now.getTime() - file.lastModified();
            long secs = millis / 1000;
            long mins = secs / 60;
            long hours = mins / 60;

            if (BuildConfig.DEBUG) {
                Log.d("UTIL", "File last modified : " + lastModDate.toString() + " secs=" + secs + "  mins=" + mins + " hours=" + hours);
            }

            if (hours < 1) {
                FileInputStream aStream = new FileInputStream(file);
                BufferedReader rd = new BufferedReader(new InputStreamReader(aStream));
                String line;
                while ((line = rd.readLine()) != null) {
                    chaine.append(line);
                }
                rd.close();
                if (BuildConfig.DEBUG) {
                    Log.d("UTIL", "used cache for:" + theURI);
                }
                return chaine.toString();
            }
            if (BuildConfig.DEBUG) {
                Log.d("UTIL", "do not use cache, because too old:" + theURI);
            }
            return null;
        } catch (Exception e) {
            Log.e("UTIL", "getCacheFile() " + e);
        }
        return null;
    }

    public static void writeFileCache(Context ctx, String theURI, String content) {
        try {
            String aFileName = theURI.toLowerCase().replace("http://", "");
            aFileName = aFileName.toLowerCase().replace("https://", "");
            aFileName = sanitizeName(aFileName);

            File f = new File(ctx.getCacheDir() + "/" + aFileName);
            FileOutputStream aStream = new FileOutputStream(f);
            aStream.write(content.getBytes("utf-8"));
            aStream.close();
        } catch (Exception e) {
            Log.e("UTIL", "writeFileCache() could not write to cache file for:" + theURI);
        }
    }

    private static String downloadFeed(OkHttpClient httpClient, Context ctx, String theURI, boolean forceUpdate, Map<String, String> dictParams) {
        Log.i("DOWN", "Url=" + theURI);
        if (!forceUpdate) {
            String cache = getCacheFile(ctx, theURI);
            if (cache != null) {
                return cache;
            }
        }
        Log.i("DOWN", "Url=" + theURI + " (not cached)");

        try {
            HttpUrl url = HttpUrl.parse(theURI);
            Request.Builder requestBuilder = new Request.Builder().url(url);

            if (dictParams != null) {
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

            writeFileCache(ctx, theURI, responseStr);
            if (BuildConfig.DEBUG) {
                Log.d("UTIL", "wrote cache file for:" + theURI);
            }
            return responseStr;
        } catch (Exception e) {
            Log.e("UTIL", "downloadFeed() " + e);
        }

        return null;
    }

    public static String downloadFeedRelative(OkHttpClient httpClient, Context ctx, String theRelativeUri, boolean forceUpdate, Map<String, String> dictParams) {
        // try current server for download
        String currentServer = RadioBrowserServerManager.getCurrentServer();
        if (currentServer == null) {
            return null;
        }

        String endpoint = RadioBrowserServerManager.constructEndpoint(currentServer, theRelativeUri);
        String result = downloadFeed(httpClient, ctx, endpoint, forceUpdate, dictParams);
        if (result != null) {
            return result;
        }

        // get a list of all servers
        String[] serverList = RadioBrowserServerManager.getServerList(false);

        // try all other servers for download
        for (String newServer : serverList) {
            if (newServer.equals(currentServer)) {
                continue;
            }

            endpoint = RadioBrowserServerManager.constructEndpoint(newServer, theRelativeUri);
            result = downloadFeed(httpClient, ctx, endpoint, forceUpdate, dictParams);
            if (result != null) {
                // set the working server as new current server
                RadioBrowserServerManager.setCurrentServer(newServer);
                return result;
            }
        }

        return null;
    }

    public static String getRealStationLink(OkHttpClient httpClient, Context ctx, String stationId) {
        Log.i("UTIL", "StationUUID:" + stationId);
        String result = Utils.downloadFeedRelative(httpClient, ctx, "json/url/" + stationId, true, null);
        if (result != null) {
            Log.i("UTIL", result);
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

    @Deprecated
    public static DataRadioStation getStationById(OkHttpClient httpClient, Context ctx, String stationId) {
        Log.w("UTIL", "Search by id:" + stationId);
        String result = Utils.downloadFeed(httpClient, ctx, "json/stations/byid/" + stationId, true, null);
        if (result != null) {
            try {
                List<DataRadioStation> list = DataRadioStation.DecodeJson(result);
                if (list != null) {
                    if (list.size() == 1) {
                        return list.get(0);
                    }
                    Log.e("UTIL", "stations by id did have length:" + list.size());
                }
            } catch (Exception e) {
                Log.e("UTIL", "getStationByid() " + e);
            }
        }
        return null;
    }

    public static DataRadioStation getStationByUuid(OkHttpClient httpClient, Context ctx, String stationUuid) {
        Log.w("UTIL", "Search by uuid:" + stationUuid);
        String result = Utils.downloadFeedRelative(httpClient, ctx, "json/stations/byuuid/" + stationUuid, true, null);
        if (result != null) {
            try {
                List<DataRadioStation> list = DataRadioStation.DecodeJson(result);
                if (list != null) {
                    if (list.size() == 1) {
                        return list.get(0);
                    }
                    Log.e("UTIL", "stations by uuid did have length:" + list.size());
                }
            } catch (Exception e) {
                Log.e("UTIL", "getStationByUuid() " + e);
            }
        }
        return null;
    }

    public static @Nullable
    DataRadioStation getCurrentOrLastStation(@NonNull Context ctx) {
        DataRadioStation station = PlayerServiceUtil.getCurrentStation();
        if (station == null) {
            RadioDroidApp radioDroidApp = (RadioDroidApp) ctx.getApplicationContext();
            HistoryManager historyManager = radioDroidApp.getHistoryManager();
            station = historyManager.getFirst();
        }

        return station;
    }

    public static void showMpdServersDialog(final RadioDroidApp radioDroidApp, final FragmentManager fragmentManager, @Nullable final DataRadioStation station) {
        Fragment oldFragment = fragmentManager.findFragmentByTag(PlayerSelectorDialog.FRAGMENT_TAG);
        if (oldFragment != null && oldFragment.isVisible()) {
            return;
        }

        PlayerSelectorDialog playerSelectorDialogFragment = new PlayerSelectorDialog(radioDroidApp.getMpdClient(), station);
        playerSelectorDialogFragment.show(fragmentManager, PlayerSelectorDialog.FRAGMENT_TAG);
    }

    public static void showPlaySelection(final RadioDroidApp radioDroidApp, final DataRadioStation station, final FragmentManager fragmentManager) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(radioDroidApp);
        final boolean play_external = sharedPref.getBoolean("play_external", false);

        if (radioDroidApp.getMpdClient().isMpdEnabled() || play_external || CastHandler.isCastSessionAvailable()) {
            showMpdServersDialog(radioDroidApp, fragmentManager, station);
        } else {
            playAndWarnIfMetered(radioDroidApp, station, PlayerType.RADIODROID, () -> play(radioDroidApp, station));
        }
    }

    public static void playAndWarnIfMetered(RadioDroidApp radioDroidApp, DataRadioStation station, PlayerType playerType, Runnable playFunc) {
        playAndWarnIfMetered(radioDroidApp, station, playerType, playFunc,
                (station1, playerType1) -> {
                    // Making sure that resuming from notification or some external event will actually resume
                    // and not issue warning a second time.
                    PlayerServiceUtil.setStation(station1);
                    PlayerServiceUtil.warnAboutMeteredConnection(playerType1);
                });
    }

    public static boolean urlIndicatesHlsStream(String streamUrl) {
        final Pattern p = Pattern.compile(".*\\.m3u8([#?\\s].*)?$");
        return p.matcher(streamUrl).matches();
    }

    public interface MeteredWarningCallback {
        void warn(DataRadioStation station, PlayerType playerType);
    }

    // TODO: Sort out the indirection when PlayerService won't need aidl and we won't need to have
    //  PlayerServiceUtil as a proxy between common code and the service.
    public static void playAndWarnIfMetered(RadioDroidApp radioDroidApp, DataRadioStation station, PlayerType playerType,
                                            Runnable playFunc, MeteredWarningCallback warningCallback) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(radioDroidApp);
        final boolean warnOnMetered = sharedPref.getBoolean("warn_no_wifi", false);

        if (warnOnMetered && ConnectivityChecker.getCurrentConnectionType(radioDroidApp) == ConnectivityChecker.ConnectionType.METERED) {
            warningCallback.warn(station, playerType);
        } else {
            playFunc.run();
        }
    }

    public static void play(final RadioDroidApp radioDroidApp, final DataRadioStation station) {
        PlayerServiceUtil.play(station);
    }

    public static boolean shouldLoadIcons(final Context context) {
        switch (loadIcons) {
            case -1:
                if (PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getBoolean("load_icons", false)) {
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
        if (selectedTheme.equals(context.getResources().getString(R.string.theme_dark)))
            return R.style.MyMaterialTheme_Dark;
        else
            return R.style.MyMaterialTheme;
    }

    public static boolean isDarkTheme(final Context context) {
        return getThemeResId(context) == R.style.MyMaterialTheme_Dark;
    }

    public static int getTimePickerThemeResId(final Context context) {
        int theme;
        if (getThemeResId(context) == R.style.MyMaterialTheme_Dark)
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
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static boolean verifyStoragePermissions(Activity activity, int request_id) {
        // Check if we have write permission
        int permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    request_id
            );
            return false;
        }

        return true;
    }

    public static boolean verifyStoragePermissions(Fragment fragment, int request_id) {
        // Check if we have write permission
        int permission = ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            fragment.requestPermissions(PERMISSIONS_STORAGE, request_id);
            return false;
        }

        return true;
    }

    public static String getReadableBytes(double bytes) {
        String[] str = new String[]{"B", "KB", "MB", "GB", "TB"};
        for (String aStr : str) {
            if (bytes < 1024) {
                return String.format(Locale.getDefault(), "%1$,.1f %2$s", bytes, aStr);
            }
            bytes = bytes / 1024;
        }
        return String.format(Locale.getDefault(), "%1$,.1f %2$s", bytes * 1024, str[str.length - 1]);
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

    /**
     * Add proxy to an okhttp builder.
     *
     * @return true if successful, false otherwise
     */
    public static boolean setOkHttpProxy(@NonNull OkHttpClient.Builder builder, @NonNull final ProxySettings proxySettings) {
        if (proxySettings.type == Proxy.Type.DIRECT) {
            return true;
        }
        if (TextUtils.isEmpty(proxySettings.host)) {
            return false;
        }
        if (proxySettings.port < 1 || proxySettings.port > 65535) {
            return false;
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

        return true;
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

    public static String getMimeType(String url, String defaultMimeType) {
        String type = defaultMimeType;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder client) {
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init((KeyStore)null);
                TrustManager[] tmList = trustManagerFactory.getTrustManagers();
                Log.i("OkHttpTLSCompat", "Found trustmanagers:"+tmList.length);
                X509TrustManager tm = (X509TrustManager)tmList[0];

                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);
                client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), tm);

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);

                client.connectionSpecs(specs);
            } catch (Exception exc) {
                Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
            }
        }

        return client;
    }
}
