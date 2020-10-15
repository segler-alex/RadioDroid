package net.programmierecke.radiodroid2.tests;

import android.content.pm.ActivityInfo;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.FavouriteManager;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.tests.utils.TestUtils;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.programmierecke.radiodroid2.tests.utils.RecyclerDragAndDropAction.recyclerDragAndDrop;
import static net.programmierecke.radiodroid2.tests.utils.RecyclerRecyclingMatcher.recyclerRecycles;
import static net.programmierecke.radiodroid2.tests.utils.RecyclerViewMatcher.withRecyclerView;
import static net.programmierecke.radiodroid2.tests.utils.ScrollToRecyclerItemAction.scrollToRecyclerItem;
import static net.programmierecke.radiodroid2.tests.utils.TestUtils.getFakeRadioStationName;
import static net.programmierecke.radiodroid2.tests.utils.conditionwatcher.ViewMatchWaiter.waitForView;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

@LargeTest
@RunWith(Parameterized.class)
public class UIFavouritesFragmentTest {

    @Parameterized.Parameter(value = 0)
    public int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    @Parameterized.Parameters(name = "orientation={0}")
    public static Iterable<Object[]> initParameters() {
        return Arrays.asList(new Object[][]{
                {ActivityInfo.SCREEN_ORIENTATION_PORTRAIT},
                {ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
        });
    }

    @Rule
    public ActivityTestRule<ActivityMain> activityRule
            = new ActivityTestRule<ActivityMain>(ActivityMain.class) {
        @Override
        protected void afterActivityLaunched() {
            getActivity().setRequestedOrientation(orientation);
            super.afterActivityLaunched();
        }
    };

    private static final int STATIONS_COUNT = 20;

    private FavouriteManager favouriteManager;

    @Before
    public void setUp() {
        TestUtils.populateFavourites(ApplicationProvider.getApplicationContext(), STATIONS_COUNT);

        RadioDroidApp app = ApplicationProvider.getApplicationContext();
        favouriteManager = app.getFavouriteManager();
    }

    @Test
    public void stationsRecyclerFavourites_ShouldRecycleItems() {
        onView(ViewMatchers.withId(R.id.nav_item_starred)).perform(ViewActions.click());

        onView(withId(R.id.recyclerViewStations)).check(matches(recyclerRecycles()));
    }

    @Ignore("Disabled until drag and drop is fixed, see " +
            "https://stackoverflow.com/questions/27992427/recyclerview-adapter-notifyitemmoved0-1-scrolls-screen")
    @Test
    public void stationInFavourites_ShouldBeReordered_WithDragAndDrop() {
        onView(ViewMatchers.withId(R.id.nav_item_starred)).perform(ViewActions.click());
        // 0 1 2 3 4

        onView(withId(R.id.recyclerViewStations)).perform(recyclerDragAndDrop(4, 0));
        // 4 0 1 2 3
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(0))
                .check(matches(hasDescendant(withText(getFakeRadioStationName(4)))));
        assertEquals(getFakeRadioStationName(4), favouriteManager.getList().get(0).Name);

        onView(withId(R.id.recyclerViewStations)).perform(recyclerDragAndDrop(4, 3));
        // 4 0 1 3 2
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(3))
                .check(matches(hasDescendant(withText(getFakeRadioStationName(3)))));
        assertEquals(getFakeRadioStationName(3), favouriteManager.getList().get(3).Name);
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(4))
                .check(matches(hasDescendant(withText(getFakeRadioStationName(2)))));
        assertEquals(getFakeRadioStationName(2), favouriteManager.getList().get(4).Name);

        onView(withId(R.id.recyclerViewStations)).perform(recyclerDragAndDrop(3, 1));
        // 4 3 0 1 2
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(1))
                .check(matches(hasDescendant(withText(getFakeRadioStationName(3)))));
        assertEquals(getFakeRadioStationName(3), favouriteManager.getList().get(1).Name);
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(2))
                .check(matches(hasDescendant(withText(getFakeRadioStationName(0)))));
        assertEquals(getFakeRadioStationName(0), favouriteManager.getList().get(2).Name);
    }

    @Test
    public void stationInFavourites_ShouldBeReordered_WithSimpleDragAndDrop() {
        onView(ViewMatchers.withId(R.id.nav_item_starred)).perform(ViewActions.click());
        // 0 1

        onView(withId(R.id.recyclerViewStations)).perform(scrollToRecyclerItem(0));
        onView(withId(R.id.recyclerViewStations)).perform(recyclerDragAndDrop(1, 0));
        // 1 0
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(0))
                .check(matches(hasDescendant(withText(getFakeRadioStationName(1)))));
        assertEquals(getFakeRadioStationName(1), favouriteManager.getList().get(0).Name);
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(1))
                .check(matches(hasDescendant(withText(getFakeRadioStationName(0)))));
        assertEquals(getFakeRadioStationName(0), favouriteManager.getList().get(1).Name);
    }

    @Test
    public void stationInFavourites_ShouldBeDeleted_WithSwipeRight() {
        onView(withId(R.id.nav_item_starred)).perform(ViewActions.click());

        onView(withId(R.id.recyclerViewStations)).perform(scrollToRecyclerItem(0));
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(0)).perform(ViewActions.swipeRight());
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(0))
                .check(matches(hasDescendant(withText(getFakeRadioStationName(1)))));
        assertEquals(STATIONS_COUNT - 1, favouriteManager.getList().size());

        onView(withId(R.id.recyclerViewStations)).perform(scrollToRecyclerItem(1));
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(1)).perform(ViewActions.swipeRight());
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(1))
                .check(matches(hasDescendant(withText(getFakeRadioStationName(3)))));
        assertEquals(STATIONS_COUNT - 2, favouriteManager.getList().size());

        onView(withId(R.id.recyclerViewStations)).perform(scrollToRecyclerItem(2));
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(2)).perform(ViewActions.swipeRight());
        onView(withRecyclerView(R.id.recyclerViewStations).atPosition(1))
                .check(matches(hasDescendant(withText(getFakeRadioStationName(3)))));
        assertEquals(STATIONS_COUNT - 3, favouriteManager.getList().size());

        // Snackbar with undo action
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            // for whatever reason this often does not work on API 21 emulators
            waitForView(withId(com.google.android.material.R.id.snackbar_action))
                    .toMatch(
                            allOf(withText(R.string.action_station_removed_from_list_undo), isDisplayed()));
            onView(withId(com.google.android.material.R.id.snackbar_action)).perform(ViewActions.click());

            assertEquals(STATIONS_COUNT - 2, favouriteManager.getList().size());
            onView(withId(R.id.recyclerViewStations)).perform(scrollToRecyclerItem(2));
            onView(withRecyclerView(R.id.recyclerViewStations).atPosition(2))
                    .check(matches(hasDescendant(withText(getFakeRadioStationName(4)))));
        }
    }
}
