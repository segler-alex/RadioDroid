package net.programmierecke.radiodroid2.proxy;

import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.net.Proxy;

public class ProxySettings {
    private final static String PREFERENCES_KEY = "proxySettings";

    public String host;
    public int port;
    public String login;
    public String password;
    public Proxy.Type type;

    public static ProxySettings fromPreferences(SharedPreferences sharedPref) {
        Gson gson = new Gson();
        String jsonStr = sharedPref.getString(PREFERENCES_KEY, "");

        return gson.fromJson(jsonStr, ProxySettings.class);
    }

    public void toPreferences(SharedPreferences.Editor sharedPrefEditor) {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(this);
        sharedPrefEditor.putString(PREFERENCES_KEY, jsonStr);
    }
}
