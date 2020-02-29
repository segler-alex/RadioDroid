package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Vector;

/**
 * Created by segler on 15.02.18.
 */

public class RadioBrowserServerManager {
    static String currentServer = null;
    static String[] serverList = null;

    /**
     * Blocking: do dns request do get a list of all available servers
     */
    private static String[] doDnsServerListing() {
        Log.d("DNS", "doDnsServerListing()");
        Vector<String> listResult = new Vector<String>();
        try {
            // add all round robin servers one by one to select them separately
            InetAddress[] list = InetAddress.getAllByName("all.api.radio-browser.info");
            for (InetAddress item : list) {
                // do not use original variable, it could fall back to "all.api.radio-browser.info"
                InetAddress new_item = InetAddress.getByName(item.getHostAddress());
                Log.i("DNS", "Found: " + new_item.toString() + " -> " + new_item.getCanonicalHostName());
                String name = item.getCanonicalHostName();
                if (!name.equals("all.api.radio-browser.info")) {
                    listResult.add(item.getCanonicalHostName());
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Log.d("DNS", "doDnsServerListing() Found servers: " + listResult.size());
        return listResult.toArray(new String[0]);
    }

    /**
     * Blocking: return current cached server list. Generate list if still null.
     */
    public static String[] getServerList(boolean forceRefresh){
        if (serverList == null || serverList.length == 0 || forceRefresh){
            serverList = doDnsServerListing();
        }
        return serverList;
    }

    /**
     * Blocking: return current selected server. Select one, if there is no current server.
     */
    public static String getCurrentServer() {
        if (currentServer == null){
            String[] serverList = getServerList(false);
            if (serverList.length > 0){
                Random rand = new Random();
                currentServer = serverList[rand.nextInt(serverList.length)];
                Log.d("SRV", "Selected new default server: " + currentServer);
            }else{
                Log.e("SRV", "no servers found");
            }
        }
        return currentServer;
    }

    /**
     * Set new server as current
     */
    public static void setCurrentServer(String newServer){
        currentServer = newServer;
    }

    /**
     * Construct full url from server and path
     */
    public static String constructEndpoint(String server, String path){
        return "https://" + server + "/" + path;
    }
}
