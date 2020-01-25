package net.programmierecke.radiodroid2.station;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.programmierecke.radiodroid2.RadioBrowserServerManager;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.utils.CustomFilter;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

        /*
        String queryEncoded = null;
        try {
            queryEncoded = URLEncoder.encode(query, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert queryEncoded != null;
        queryEncoded = queryEncoded.replace("+", "%20");
        */
        //String urlString = RadioBrowserServerManager.constructEndpoint(RadioBrowserServerManager.getCurrentServer(), "json/stations/byname/" + queryEncoded);

        //Log.i(TAG, urlString);

        //HttpUrl.Builder httpBuider = HttpUrl.parse(urlString).newBuilder();
        //httpBuider.addQueryParameter("order", "clickcount");
        //httpBuider.addQueryParameter("limit", "100");

        //Request.Builder builder = new Request.Builder().url(httpBuider.build());
        //Request request = builder.get().build();

        //try {
            //Response response = httpClient.newCall(request).execute();
            //List<DataRadioStation> result = DataRadioStation.DecodeJson(response.body().string());

            String resultString = Utils.downloadFeedRelative(httpClient, this.context, query, false, null);
            List<DataRadioStation> result = DataRadioStation.DecodeJson(resultString);
            lastRemoteSearchStatus = SearchStatus.SUCCESS;
            return result;
        //} catch (IOException e) {
        //    lastRemoteSearchStatus = SearchStatus.ERROR;
        //}

        //return new ArrayList<>();
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        final String query = constraint.toString().toLowerCase();
        Log.d("FILTER", "performFiltering() " + query);

        if (query.isEmpty() || (query.length() < 3 && filterType == FilterType.GLOBAL)) {
            filteredStationsList = dataProvider.getOriginalStationList();
            lastRemoteQuery = "";
        } else {
            List<DataRadioStation> stationsToFilter;

            boolean needsFiltering = false;

            if (!lastRemoteQuery.isEmpty() && query.startsWith(lastRemoteQuery) && lastRemoteSearchStatus != SearchStatus.ERROR) {
                // We can filter already existing list without making costly http call.
                stationsToFilter = filteredStationsList;
                needsFiltering = true;
            } else {
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
