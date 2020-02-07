package net.programmierecke.radiodroid2.station;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.utils.CustomFilter;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import okhttp3.OkHttpClient;

public class StationsFilter extends CustomFilter {
    public enum FilterType {
        /**
         * Will perform search only in initially set list
         */
        LOCAL,
        /**
         * Doesn't care about what is already in the list will search in ALL stations
         */
        GLOBAL,
    }

    public enum SearchStatus {
        SUCCESS,
        ERROR
    }

    public enum SearchStyle {
        ByName,
        ByLanguageExact,
        ByCountryCodeExact,
        ByTagExact,
    }

    public interface DataProvider {
        List<DataRadioStation> getOriginalStationList();

        void notifyFilteredStationsChanged(SearchStatus status, List<DataRadioStation> filteredStations);
    }

    private final String TAG = "StationsFilter";
    private final int FUZZY_SEARCH_THRESHOLD = 55;

    private FilterType filterType;
    private Context context;
    private DataProvider dataProvider;

    private String lastRemoteQuery = "";
    private List<DataRadioStation> filteredStationsList;
    private SearchStatus lastRemoteSearchStatus = SearchStatus.SUCCESS;

    private SearchStyle searchStyle = SearchStyle.ByName;

    private class WeightedStation {
        DataRadioStation station;
        int weight;

        WeightedStation(DataRadioStation station, int weight) {
            this.station = station;
            this.weight = weight;
        }
    }

    public StationsFilter(@NonNull Context context, FilterType filterType, @NonNull DataProvider dataProvider) {
        this.context = context;
        this.filterType = filterType;
        this.dataProvider = dataProvider;
    }

    public void setSearchStyle(SearchStyle searchStyle){
        Log.d("FILTER","Changed search style:" + searchStyle);
        this.searchStyle = searchStyle;
    }

    private @NonNull
    List<DataRadioStation> searchGlobal(final @NotNull String query) {
        Log.d("FILTER", "searchGlobal 1:" + query);
        RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
        // TODO: use http client with custom timeouts
        OkHttpClient httpClient = radioDroidApp.getHttpClient();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.context);
        final boolean show_broken = sharedPref.getBoolean("show_broken", false);

        HashMap<String, String> p = new HashMap<String, String>();
        p.put("order", "clickcount");
        p.put("reverse", "true");
        p.put("hidebroken", ""+(!show_broken));

        try {
            String queryEncoded = URLEncoder.encode(query, "utf-8");
            queryEncoded = queryEncoded.replace("+", "%20");

            String searchUrl = null;
            switch (searchStyle){
                case ByName:
                    searchUrl = "json/stations/byname/" + queryEncoded;
                    break;
                case ByCountryCodeExact:
                    searchUrl = "json/stations/bycountrycodeexact/" + queryEncoded;
                    break;
                case ByLanguageExact:
                    searchUrl = "json/stations/bylanguageexact/" + queryEncoded;
                    break;
                case ByTagExact:
                    searchUrl = "json/stations/bytagexact/" + queryEncoded;
                    break;
                default:
                    Log.d("FILTER", "unknown search style: "+searchStyle);
                    lastRemoteSearchStatus = SearchStatus.ERROR;
                    return new ArrayList<>();
            }

            Log.d("FILTER", "searchGlobal 2:" + query);

            String resultString = Utils.downloadFeedRelative(httpClient, radioDroidApp, searchUrl, false, p);
            if (resultString != null) {
                Log.d("FILTER", "searchGlobal 3a:" + query);
                List<DataRadioStation> result = DataRadioStation.DecodeJson(resultString);
                lastRemoteSearchStatus = SearchStatus.SUCCESS;
                return result;
            }else{
                Log.d("FILTER", "searchGlobal 3b:" + query);
                lastRemoteSearchStatus = SearchStatus.ERROR;
                return new ArrayList<>();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            lastRemoteSearchStatus = SearchStatus.ERROR;
            return new ArrayList<>();
        }
    }

    public void clearList(){
        Log.d("FILTER", "forced refetch");
        lastRemoteQuery = "";
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        final String query = constraint.toString().toLowerCase();
        Log.d("FILTER", "performFiltering() " + query);

        if (searchStyle == SearchStyle.ByName && (query.isEmpty() || (query.length() < 3 && filterType == FilterType.GLOBAL))) {
            Log.d("FILTER", "performFiltering() 2 " + query);
            filteredStationsList = dataProvider.getOriginalStationList();
            lastRemoteQuery = "";
        } else {
            Log.d("FILTER", "performFiltering() 3 " + query);
            List<DataRadioStation> stationsToFilter;

            boolean needsFiltering = false;

            if (!lastRemoteQuery.isEmpty() && query.startsWith(lastRemoteQuery) && lastRemoteSearchStatus != SearchStatus.ERROR) {
                Log.d("FILTER", "performFiltering() 3a " + query + " lastRemoteQuery=" + lastRemoteQuery);
                // We can filter already existing list without making costly http call.
                stationsToFilter = filteredStationsList;
                needsFiltering = true;
            } else {
                Log.d("FILTER", "performFiltering() 3b " + query);
                switch (filterType) {

                    case LOCAL:
                        stationsToFilter = dataProvider.getOriginalStationList();
                        needsFiltering = true;
                        break;
                    case GLOBAL:
                        stationsToFilter = searchGlobal(query);
                        needsFiltering = false;
                        lastRemoteQuery = query;
                        break;
                    default:
                        throw new RuntimeException("performFiltering: Unknown filterType!");
                }
            }

            if (needsFiltering) {
                Log.d("FILTER", "performFiltering() 4a " + query);
                ArrayList<WeightedStation> filteredStations = new ArrayList<>();

                for (DataRadioStation station : stationsToFilter) {
                    int weight = FuzzySearch.partialRatio(query, station.Name.toLowerCase());
                    if (weight > FUZZY_SEARCH_THRESHOLD) {
                        // We will sort stations with similar weight by other metric
                        int compressedWeight = weight / 4;
                        filteredStations.add(new WeightedStation(station, compressedWeight));
                    }
                }

                Collections.sort(filteredStations, (x, y) -> {
                    if (x.weight == y.weight) {
                        return -Integer.compare(x.station.ClickCount, y.station.ClickCount);
                    }
                    return -Integer.compare(x.weight, y.weight);
                });

                filteredStationsList = new ArrayList<>();
                for (WeightedStation weightedStation : filteredStations) {
                    filteredStationsList.add(weightedStation.station);
                }
            } else {
                Log.d("FILTER", "performFiltering() 4b " + query);
                filteredStationsList = stationsToFilter;
            }
        }

        FilterResults filterResults = new FilterResults();
        filterResults.values = filteredStationsList;
        return filterResults;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        dataProvider.notifyFilteredStationsChanged(lastRemoteSearchStatus, (List<DataRadioStation>) results.values);
    }
}
