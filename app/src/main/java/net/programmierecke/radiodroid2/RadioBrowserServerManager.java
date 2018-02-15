package net.programmierecke.radiodroid2;

/**
 * Created by segler on 15.02.18.
 */

public class RadioBrowserServerManager {
    public static String getServerAddress(){
        return "https://www.radio-browser.info/webservice";
    }

    public static String getWebserviceEndpoint(String command){
        return getServerAddress() + "/" + command;
    }
}
