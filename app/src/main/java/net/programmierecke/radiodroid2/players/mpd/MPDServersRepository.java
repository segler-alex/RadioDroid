package net.programmierecke.radiodroid2.players.mpd;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * MPD servers repository which is serialized into preferences.
 * It is NOT thread safe.
 * In future should be backed up by database.
 */
public class MPDServersRepository {
    private List<MPDServerData> servers;
    private final MutableLiveData<List<MPDServerData>> serversLiveData = new MutableLiveData<>();
    private int lastServerId = -1;

    private final Context context;

    public MPDServersRepository(Context context) {
        this.context = context;
        servers = getMPDServers(context);
        for (MPDServerData server : servers) {
            if (server.id > lastServerId) {
                lastServerId = server.id;
            }
        }

        serversLiveData.setValue(servers);
    }

    public LiveData<List<MPDServerData>> getAllServers() {
        return serversLiveData;
    }

    public void addServer(@NonNull MPDServerData mpdServerData) {
        mpdServerData.id = ++lastServerId;
        servers.add(mpdServerData);

        saveMPDServers(servers, context);

        serversLiveData.postValue(servers);
    }

    public boolean isEmpty(){
        if (serversLiveData != null){
            List<MPDServerData> list = serversLiveData.getValue();
            if (list != null){
                return list.size() == 0;
            }
        }
        return true;
    }

    public void removeServer(@NonNull MPDServerData mpdServerData) {
        boolean changed = false;
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).id == mpdServerData.id) {
                servers.remove(i);
                changed = true;
                break;
            }
        }

        if (changed) {
            saveMPDServers(servers, context);
            serversLiveData.postValue(servers);
        }
    }

    public void resetAllConnectionStatus() {
        for (MPDServerData serverData : servers) {
            serverData.connected = false;
        }

        serversLiveData.postValue(serversLiveData.getValue());
    }

    public void updatePersistentData(@NonNull MPDServerData mpdServerData) {
        boolean changed = false;
        for (int i = 0; i < servers.size(); i++) {
            MPDServerData data = servers.get(i);
            if (data.id == mpdServerData.id && !data.contentEquals(mpdServerData)) {
                servers.set(i, mpdServerData);
                changed = true;
                break;
            }
        }

        if (changed) {
            saveMPDServers(servers, context);
            serversLiveData.postValue(serversLiveData.getValue());
        }
    }

    public void updateRuntimeData(@NonNull MPDServerData mpdServerData) {
        boolean changed = false;
        for (int i = 0; i < servers.size(); i++) {
            MPDServerData data = servers.get(i);
            if (data.id == mpdServerData.id && !data.contentEquals(mpdServerData)) {
                servers.set(i, mpdServerData);
                changed = true;
                break;
            }
        }

        if (changed) {
            serversLiveData.postValue(serversLiveData.getValue());
        }
    }

    private static List<MPDServerData> getMPDServers(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String serversFromPrefs = sharedPref.getString("mpd_servers", "");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<MPDServerData>>() {
        }.getType();
        List<MPDServerData> serversList = gson.fromJson(serversFromPrefs, type);
        return serversList != null ? serversList : new ArrayList<>();
    }

    private static void saveMPDServers(List<MPDServerData> servers, Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = new Gson();
        String serversJson = gson.toJson(servers);
        editor.putString("mpd_servers", serversJson);
        editor.apply();
    }
}
