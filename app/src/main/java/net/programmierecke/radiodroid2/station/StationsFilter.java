package net.programmierecke.radiodroid2.station;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.utils.CustomFilter;

import org.jetbrains.annotations.NotNull;

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

    private @NonNull
    List<DataRadioStation> searchGlobal(final @NotNull String query) {
        RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
        // TODO: use http client with custom timeouts
        OkHttpClient httpClient = radioDroidApp.getHttpClient();

        HashMap<String, String> p = new HashMap<String, String>();
        p.put("order", "clickcount");
        p.put("reverse", "true");

        String resultString = Utils.downloadFeedRelative(httpClient, radioDroidApp, query, false, p);
        if (resultString != null) {
            List<DataRadioStation> result = DataRadioStation.DecodeJson(resultString);
            lastRemoteSearchStatus = SearchStatus.SUCCESS;
            return result;
        }else{
            lastRemoteSearchStatus = SearchStatus.ERROR;
            return new ArrayList<>();
        }
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        final String query = constraint.toString().toLowerCase();
        Log.d("FILTER", "performFiltering() " + query);

        if (query.isEmpty() || (query.length() < 3 && filterType == FilterType.GLOBAL)) {
            Log.d("FILTER", "performFiltering() 2 " + query);
            filteredStationsList = dataProvider.getOriginalStationList();
            lastRemoteQuery = "";
        } else {
            Log.d("FILTER", "performFiltering() 3 " + query);
            List<DataRadioStation> stationsToFilter;

            boolean needsFiltering = false;

            if (!lastRemoteQuery.isEmpty() && query.startsWith(lastRemoteQuery) && lastRemoteSearchStatus != SearchStatus.ERROR) {
                Log.d("FILTER", "performFiltering() 3a " + query);
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
                        stationsToFilter = searchGlobal("/json/stations/byname/" + query);
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
