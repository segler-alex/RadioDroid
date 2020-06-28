package net.programmierecke.radiodroid2.tests;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.tests.utils.TestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.programmierecke.radiodroid2.tests.utils.OrientationChangeAction.orientationLandscape;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class UIRotationTest {

    @Rule
    public ActivityTestRule<ActivityMain> activityRule
            = new ActivityTestRule<>(ActivityMain.class);

    @Before
    public void setUp() {
        TestUtils.populateFavourites(ApplicationProvider.getApplicationContext(), 5);
        TestUtils.populateHistory(ApplicationProvider.getApplicationContext(), 5);
    }

    @Test
    public void stationsFragment_ShouldNotCrash_WhenScreenRotated() {
        onView(isRoot()).perform(orientationLandscape());
    }

    @Test
    public void historyFragment_ShouldNotCrash_WhenScreenRotated() {
        onView(ViewMatchers.withId(R.id.nav_item_history)).perform(ViewActions.click());
        onView(isRoot()).perform(orientationLandscape());
    }

    @Test
    public void favouritesFragment_ShouldNotCrash_WhenScreenRotated() {
        onView(withId(R.id.nav_item_starred)).perform(ViewActions.click());
        onView(isRoot()).perform(orientationLandscape());
    }

    @Test
    public void settingsFragment_ShouldNotCrash_WhenScreenRotated() {
        onView(withId(R.id.nav_item_starred)).perform(ViewActions.click());
        onView(isRoot()).perform(orientationLandscape());
    }

}
