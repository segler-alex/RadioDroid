package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

/**
 * Created by segler on 15.02.18.
 */

public class RadioBrowserServerManager {
    public static String getServerAddress(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        return "https://" + prefs.getString("radiobrowser_server", context.getString(R.string.settings_radiobrowser_servers_default));
        //return "https://www.radio-browser.info/webservice";
        //return "https://gb1.api.radio-browser.info";
    }

    public static String getWebserviceEndpoint(Context context, String command){
        return getServerAddress(context) + "/" + command;
    }
}
