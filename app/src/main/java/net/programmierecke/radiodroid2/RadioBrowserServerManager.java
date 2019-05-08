package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

/**
 * Created by segler on 15.02.18.
 */

public class RadioBrowserServerManager {
    private static String getServerAddress(Context context){
        if (context == null){
            return "https://www.radio-browser.info/webservice";
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return "https://" + prefs.getString("radiobrowser_server", context.getString(R.string.settings_radiobrowser_servers_default));
    }

    public static String getWebserviceEndpoint(Context context, String command){
        String a = getServerAddress(context);
        if (a != null){
            return a + "/" + command;
        }
        return null;
    }
}
