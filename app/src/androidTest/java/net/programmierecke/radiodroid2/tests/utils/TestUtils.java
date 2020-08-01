package net.programmierecke.radiodroid2.tests.utils;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.UiController;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.FavouriteManager;
import net.programmierecke.radiodroid2.HistoryManager;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.station.DataRadioStation;

import java.util.Objects;

public class TestUtils {

    static {
        BuildConfig.IS_TESTING.set(true);
    }

    public static String getFakeRadioStationName(int idx) {
        return String.format("Test Station %d", idx);
    }

    public static DataRadioStation generateFakeRadioStation(int idx) {
        DataRadioStation station = new DataRadioStation();

        String uuid = String.format("8fb6e56c-155c-4d70-aa72-2322e640c2d3-%d", idx);

        station.Name = getFakeRadioStationName(idx);
        station.StationUuid = uuid;
        station.ChangeUuid = uuid;
        station.StreamUrl = "";
        station.HomePageUrl = "";
        station.IconUrl = "";
        station.Country = "Angola";
        station.State = "";
        station.TagsAll = "Tag1,Tag2,Tag3";
        station.Language = "English";
        station.ClickCount = 100;
        station.ClickTrend = 1;
        station.Votes = 110;
        station.RefreshRetryCount = 0;
        station.Bitrate = 128;
        station.Codec = "mp3";
        station.Working = true;
        station.Hls = false;
        return station;
    }

    public static void populateFavourites(Context context, int stationsCount) {
        RadioDroidApp app = (RadioDroidApp) context.getApplicationContext();
        FavouriteManager favouriteManager = app.getFavouriteManager();
        favouriteManager.clear();

        for (int i = 0; i < stationsCount; i++) {
            DataRadioStation station = generateFakeRadioStation(i);
            favouriteManager.add(station);
        }
    }

    public static void populateHistory(Context context, int stationsCount) {
        RadioDroidApp app = (RadioDroidApp) context.getApplicationContext();
        HistoryManager historyManager = app.getHistoryManager();
        historyManager.clear();

        for (int i = 0; i < stationsCount; i++) {
            DataRadioStation station = generateFakeRadioStation(i);
            historyManager.add(station);
        }
    }

    /**
     * Tries make item to be in a center of RecyclerView
     * <p>
     * Q: Why do one want to center the item?
     * A: Because it's good way to ensure that it is visible.
     * <p>
     * Q: Could I just call {@link RecyclerView#scrollToPosition(int)}?
     * A: Yes, it will scroll to the item, but you won't know if this item is obstructed by other view?
     * <p>
     * Q: Is there a method to check if item is not visible?
     * A: Kinda, but it fails if the view is obstructed by view which is not an ancestor of the RecyclerView.
     *
     * @param recyclerView
     * @param idx          index of the item in adapter
     */
    public static void centerItemInRecycler(UiController uiController, RecyclerView recyclerView, int idx) {
        recyclerView.scrollToPosition(idx);
        uiController.loopMainThreadUntilIdle();

        View itemView = Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(idx)).itemView;

        int[] originalPos = new int[2];

        itemView.getLocationInWindow(originalPos);

        int scrollX = (int) (originalPos[0] - (recyclerView.getX() + recyclerView.getWidth() / 2));
        int scrollY = (int) (originalPos[1] - (recyclerView.getY() + recyclerView.getHeight() / 2));

        recyclerView.scrollBy(scrollX, scrollY);
        uiController.loopMainThreadUntilIdle();
    }
}
