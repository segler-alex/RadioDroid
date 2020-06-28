package net.programmierecke.radiodroid2.tests;

import android.content.pm.ActivityInfo;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.tests.utils.ViewPagerIdlingResource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.programmierecke.radiodroid2.tests.utils.RecyclerRecyclingMatcher.recyclerRecycles;
import static net.programmierecke.radiodroid2.tests.utils.RecyclerViewItemCountAssertion.withItemCount;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;

@LargeTest
@RunWith(Parameterized.class)
public class UIStationListsTest {

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

    @Test
    public void mainActivity_ShouldNotCrash_WhenLaunched() {
        onView(ViewMatchers.withId(R.id.my_awesome_toolbar)).check(matches(hasDescendant(withText(R.string.nav_item_stations))));
    }

    @Test
    public void stationTabs_DoWork_WhenSwiped() {
        ViewPagerIdlingResource idlingResource = new ViewPagerIdlingResource(activityRule.getActivity().findViewById(R.id.viewpager), "ViewPager");
        IdlingRegistry.getInstance().register(idlingResource);

        onView(allOf(withId(R.id.recyclerViewStations), isDisplayingAtLeast(60)))
                .check(withItemCount(greaterThan(0)))
                .check(matches(recyclerRecycles()));

        for (int i = 0; i < 7; i++) {
            // TODO: We cannot swipe on containerView because it is displayed by less than 90%,
            //       which breaks constraint in swipeLeft().
            onView(withId(R.id.main_content)).perform(ViewActions.swipeLeft());
            onView(allOf(withId(R.id.recyclerViewStations), isDisplayingAtLeast(60)))
                    .check(withItemCount(greaterThan(0)))
                    .check(matches(recyclerRecycles()));

        }

        onView(withId(R.id.main_content)).perform(ViewActions.swipeLeft());

        IdlingRegistry.getInstance().unregister(idlingResource);
    }

}