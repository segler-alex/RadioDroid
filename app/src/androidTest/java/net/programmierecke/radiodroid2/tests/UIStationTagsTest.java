package net.programmierecke.radiodroid2.tests;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.tests.utils.ViewPagerIdlingResource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.programmierecke.radiodroid2.tests.utils.ClickableSpanViewAction.clickClickableSpan;
import static net.programmierecke.radiodroid2.tests.utils.RecyclerViewItemCountAssertion.withItemCount;
import static net.programmierecke.radiodroid2.tests.utils.RecyclerViewMatcher.withRecyclerView;
import static net.programmierecke.radiodroid2.tests.utils.conditionwatcher.ViewMatchWaiter.waitForView;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.AllOf.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class UIStationTagsTest {

    @Rule
    public ActivityTestRule<ActivityMain> activityRule
            = new ActivityTestRule<>(ActivityMain.class);

    @Test
    public void stationTag_ShouldSearchStationsByTag_WhenClicked() {
        ViewPagerIdlingResource idlingResource = new ViewPagerIdlingResource(activityRule.getActivity().findViewById(R.id.viewpager), "ViewPager");
        IdlingRegistry.getInstance().register(idlingResource);

        onView(allOf(withId(R.id.buttonMore), isDescendantOfA(withRecyclerView(R.id.recyclerViewStations).atPosition(0)))).perform(ViewActions.click());
        onView(allOf(withId(R.id.viewTags), isDescendantOfA(withRecyclerView(R.id.recyclerViewStations).atPosition(0)))).perform(clickClickableSpan(0));

        waitForView(allOf(withId(R.id.recyclerViewStations), isDisplayingAtLeast(90)))
                .toCheck(withItemCount(greaterThan(0)));

        IdlingRegistry.getInstance().unregister(idlingResource);
    }
}
