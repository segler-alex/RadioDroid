package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.programmierecke.radiodroid2.station.DataRadioStation;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Vector;

import info.debatty.java.stringsimilarity.Cosine;
import okhttp3.OkHttpClient;

public class StationSaveManager extends Observable {
    protected interface StationStatusListener {
        void onStationStatusChanged(DataRadioStation station, boolean favourite);
    }

    Context context;
    List<DataRadioStation> listStations = new ArrayList<DataRadioStation>();

    protected StationStatusListener stationStatusListener;

    public StationSaveManager(Context ctx) {
        this.context = ctx;
        Load();
    }

    protected String getSaveId() {
        return "default";
    }

    protected void setStationStatusListener(StationStatusListener stationStatusListener) {
        this.stationStatusListener = stationStatusListener;
    }

    public void add(DataRadioStation station) {
        if (station.queue == null)
            station.queue = this;
        listStations.add(station);
        Save();

        notifyObservers();

        if (stationStatusListener != null) {
            stationStatusListener.onStationStatusChanged(station, true);
        }
    }

    public void addMultiple(List<DataRadioStation> stations) {
        for (DataRadioStation station_new: stations){
            listStations.add(station_new);
        }
        Save();

        notifyObservers();
    }

    public void replaceList(List<DataRadioStation> stations_new) {
        for (DataRadioStation station_new: stations_new) {
            for (int i = 0; i < listStations.size(); i++) {
                if (listStations.get(i).StationUuid.equals(station_new.StationUuid)){
                    listStations.set(i, station_new);
                    break;
                }
            }
        }
        Save();

        notifyObservers();
    }

    public void addFront(DataRadioStation station) {
        if (station.queue == null)
            station.queue = this;
        listStations.add(0, station);
        Save();

        notifyObservers();

        if (stationStatusListener != null) {
            stationStatusListener.onStationStatusChanged(station, true);
        }
    }

        public void addAll(List<DataRadioStation> stations) {
        if (stations == null)
            return;
        for (DataRadioStation station : stations) {
            station.queue = this;
        }
        listStations.addAll(stations);
    }
    
    public DataRadioStation getLast() {
        if (!listStations.isEmpty()) {
            return listStations.get(listStations.size() - 1);
        }

        return null;
    }

    public DataRadioStation getFirst() {
        if (!listStations.isEmpty()) {
            return listStations.get(0);
        }

        return null;
    }

    public DataRadioStation getById(String id) {
        for (DataRadioStation station : listStations) {
            if (id.equals(station.StationUuid)) {
                return station;
            }
        }
        return null;
    }

    public DataRadioStation getNextById(String id) {
        if (listStations.isEmpty())
            return null;

        for (int i = 0; i < listStations.size() - 1; i++) {
            if (listStations.get(i).StationUuid.equals(id)) {
                return listStations.get(i + 1);
            }
        }
        return listStations.get(0);
    }

    public DataRadioStation getPreviousById(String id) {
        if (listStations.isEmpty())
            return null;

        for (int i = 1; i < listStations.size(); i++) {
            if (listStations.get(i).StationUuid.equals(id)) {
                return listStations.get(i - 1);
            }
        }
        return listStations.get(listStations.size() - 1);
    }

    public void moveWithoutNotify(int fromPos, int toPos) {
        Collections.rotate(listStations.subList(Math.min(fromPos, toPos), Math.max(fromPos, toPos) + 1), Integer.signum(fromPos - toPos));
    }

    public void move(int fromPos, int toPos) {
        moveWithoutNotify(fromPos, toPos);
        notifyObservers();
    }

    public @Nullable
    DataRadioStation getBestNameMatch(String query) {
        DataRadioStation bestStation = null;
        query = query.toUpperCase();
        double smallesDistance = Double.MAX_VALUE;

        Cosine distMeasure = new Cosine(); // must be in the loop for some measures (e.g. Sift4)
        for (DataRadioStation station : listStations) {
            double distance = distMeasure.distance(station.Name.toUpperCase(), query);
            if (distance < smallesDistance) {
                bestStation = station;
                smallesDistance = distance;
            }
        }

        return bestStation;
    }

    public int remove(String id) {
        for (int i = 0; i < listStations.size(); i++) {
            DataRadioStation station = listStations.get(i);
            if (station.StationUuid.equals(id)) {
                listStations.remove(i);
                Save();
                notifyObservers();

                if (stationStatusListener != null) {
                    stationStatusListener.onStationStatusChanged(station, false);
                }

                return i;
            }
        }

        return -1;
    }

    public void restore(DataRadioStation station, int pos) {
        station.queue = this;
        listStations.add(pos, station);
        Save();

        notifyObservers();

        if (stationStatusListener != null) {
            stationStatusListener.onStationStatusChanged(station, false);
        }
    }

    public void clear() {
        List<DataRadioStation> oldStation = listStations;
        listStations = new ArrayList<>();
        Save();

        notifyObservers();

        if (stationStatusListener != null) {
            for (DataRadioStation station : oldStation) {
                stationStatusListener.onStationStatusChanged(station, false);
            }
        }
    }

    @Override
    public boolean hasChanged() {
        return true;
    }

    public int size() {
        return listStations.size();
    }

    public boolean isEmpty() {
        return listStations.size() == 0;
    }

    public boolean has(String id) {
        DataRadioStation station = getById(id);
        return station != null;
    }

    private boolean hasInvalidUuids() {
        for (DataRadioStation station : listStations) {
            if (!station.hasValidUuid()) {
                return true;
            }
        }

        return false;
    }

    public List<DataRadioStation> getList() {
        return Collections.unmodifiableList(listStations);
    }

    private void refreshStationsFromServer() {
        final RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
        final OkHttpClient httpClient = radioDroidApp.getHttpClient();
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ActivityMain.ACTION_SHOW_LOADING));

        new AsyncTask<Void, Void, ArrayList<DataRadioStation>>() {
            private ArrayList<DataRadioStation> savedStations;

            @Override
            protected void onPreExecute() {
                savedStations = new ArrayList<>(listStations);
            }

            @Override
            protected ArrayList<DataRadioStation> doInBackground(Void... params) {
                ArrayList<DataRadioStation> stationsToRemove = new ArrayList<>();
                for (DataRadioStation station : savedStations) {
                    if (!station.refresh(httpClient, context) && !station.hasValidUuid() && station.RefreshRetryCount > DataRadioStation.MAX_REFRESH_RETRIES) {
                        stationsToRemove.add(station);
                    }
                }

                return stationsToRemove;
            }

            @Override
            protected void onPostExecute(ArrayList<DataRadioStation> stationsToRemove) {
                listStations.removeAll(stationsToRemove);

                Save();

                notifyObservers();

                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));
                super.onPostExecute(stationsToRemove);
            }
        }.execute();
    }

    void Load() {
        listStations.clear();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String str = sharedPref.getString(getSaveId(), null);
        if (str != null) {
            List<DataRadioStation> arr = DataRadioStation.DecodeJson(str);
            for (DataRadioStation station : arr) {
                station.queue = this;
            }
            listStations.addAll(arr);
            if (hasInvalidUuids() && Utils.hasAnyConnection(context)) {
                refreshStationsFromServer();
            }
        } else {
            Log.w("SAVE", "Load() no stations to load");
        }
    }

    void Save() {
        JSONArray arr = new JSONArray();
        for (DataRadioStation station : listStations) {
            arr.put(station.toJson());
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        String str = arr.toString();
        if (BuildConfig.DEBUG) {
            Log.d("SAVE", "wrote: " + str);
        }
        editor.putString(getSaveId(), str);
        editor.commit();
    }

    public static String getSaveDir() {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "";
        File folder = new File(path);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Log.e("SAVE", "could not create dir:" + path);
            }
        }
        return path;
    }

    public void SaveM3U(final String filePath, final String fileName) {
        Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_save_playlist_now, filePath, fileName), Toast.LENGTH_LONG);
        toast.show();

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return SaveM3UInternal(filePath, fileName);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result.booleanValue()) {
                    Log.i("SAVE", "OK");
                    Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_save_playlist_ok, filePath, fileName), Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    Log.i("SAVE", "NOK");
                    Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_save_playlist_nok, filePath, fileName), Toast.LENGTH_LONG);
                    toast.show();
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    public void SaveM3USimple(final String filePath, final String fileName) {
        Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_save_playlist_now, filePath, fileName), Toast.LENGTH_LONG);
        toast.show();

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return SaveM3UInternal(filePath, fileName);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result.booleanValue()) {
                    Log.i("SAVE", "OK");
                    Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_save_playlist_ok, filePath, fileName), Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    Log.i("SAVE", "NOK");
                    Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_save_playlist_nok, filePath, fileName), Toast.LENGTH_LONG);
                    toast.show();
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    public void LoadM3U(final String filePath, final String fileName) {
        Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_load_playlist_now, filePath, fileName), Toast.LENGTH_LONG);
        toast.show();

        new AsyncTask<Void, Void, List<DataRadioStation>>() {
            @Override
            protected List<DataRadioStation> doInBackground(Void... params) {
                return LoadM3UInternal(filePath, fileName);
            }

            @Override
            protected void onPostExecute(List<DataRadioStation> result) {
                if (result != null) {
                    Log.i("LOAD", "Loaded " + result.size() + "stations");
                    addMultiple(result);
                    Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_load_playlist_ok, result.size(), filePath, fileName), Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    Log.e("LOAD", "Load failed");
                    Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_load_playlist_nok, filePath, fileName), Toast.LENGTH_LONG);
                    toast.show();
                }

                notifyObservers();

                super.onPostExecute(result);
            }
        }.execute();
    }

    public void LoadM3USimple(final Reader reader) {
        Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_load_playlist_now, "", ""), Toast.LENGTH_LONG);
        toast.show();

        new AsyncTask<Void, Void, List<DataRadioStation>>() {
            @Override
            protected List<DataRadioStation> doInBackground(Void... params) {
                return LoadM3UReader(reader);
            }

            @Override
            protected void onPostExecute(List<DataRadioStation> result) {
                if (result != null) {
                    Log.i("LOAD", "Loaded " + result.size() + "stations");
                    addMultiple(result);
                    Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_load_playlist_ok, result.size(), "", ""), Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    Log.e("LOAD", "Load failed");
                    Toast toast = Toast.makeText(context, context.getResources().getString(R.string.notify_load_playlist_nok, "", ""), Toast.LENGTH_LONG);
                    toast.show();
                }

                notifyObservers();

                super.onPostExecute(result);
            }
        }.execute();
    }

    protected final String M3U_PREFIX = "#RADIOBROWSERUUID:";

    boolean SaveM3UInternal(String filePath, String fileName) {
        final RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
        final OkHttpClient httpClient = radioDroidApp.getHttpClient();

        try {
            File f = new File(filePath, fileName);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, false));
            var r = SaveM3UWriter(bw);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))));
            } else {
                MediaScannerConnection
                        .scanFile(context, new String[]{f.getAbsolutePath()}, null, null);
            }
            return r;
        } catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
            return false;
        }
    }

    public boolean SaveM3UWriter(Writer bw) {
        final RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
        final OkHttpClient httpClient = radioDroidApp.getHttpClient();

        try {
            bw.write("#EXTM3U\n");
            for (DataRadioStation station : listStations) {
                bw.write(M3U_PREFIX + station.StationUuid + "\n");
                bw.write("#EXTINF:-1," + station.Name + "\n");
                bw.write(station.StreamUrl + "\n\n");
            }
            bw.flush();
            bw.close();

            return true;
        } catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
            return false;
        }
    }

    List<DataRadioStation> LoadM3UInternal(String filePath, String fileName) {
        try {
            File f = new File(filePath, fileName);
            FileReader fr = new FileReader(f);
            return LoadM3UReader(fr);
        } catch (Exception e) {
            Log.e("LOAD", "File read failed: " + e.toString());
            return null;
        }
    }

    List<DataRadioStation> LoadM3UReader(Reader reader) {
        try {
            String line;

            final RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
            final OkHttpClient httpClient = radioDroidApp.getHttpClient();
            ArrayList<String> listUuids = new ArrayList<String>();
            ArraySet<DataRadioStation> loadedItems = null;

            BufferedReader br = new BufferedReader(reader);
            while ((line = br.readLine()) != null) {
                Log.v("LOAD", "line: "+line);
                if (line.startsWith(M3U_PREFIX)) {
                    try {
                        String uuid = line.substring(M3U_PREFIX.length()).trim();
                        DataRadioStation station = Utils.getStationByUuid(httpClient, context, uuid);
                        if (station != null) {
                            station.queue = this;
                            loadedItems.add(station);
                        }
                    } catch (Exception e) {
                        Log.e("LOAD", e.toString());
                    }
                }
            }
            br.close();

            List<DataRadioStation> listStationsNew = Utils.getStationsByUuid(httpClient, context, listUuids);

            // sort list to have the same order as the initial save file
            List<DataRadioStation> listStationsSorted = new ArrayList<DataRadioStation>();
            for (String uuid: listUuids)
            {
                for (DataRadioStation s: listStationsNew){
                    if (uuid.equals(s.StationUuid)){
                        listStationsSorted.add(s);
                        break;
                    }
                }
            }
            return listStationsSorted;
        } catch (Exception e) {
            Log.e("LOAD", "File read failed: " + e.toString());
            return null;
        }
    }
}
