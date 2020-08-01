package net.programmierecke.radiodroid2.tests;

import android.content.Context;
import android.media.AudioManager;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.tests.utils.TestUtils;
import net.programmierecke.radiodroid2.tests.utils.conditionwatcher.ConditionWatcher;
import net.programmierecke.radiodroid2.tests.utils.conditionwatcher.IsMusicPlayingCondition;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.programmierecke.radiodroid2.tests.utils.RecyclerViewMatcher.withRecyclerView;
import static net.programmierecke.radiodroid2.tests.utils.conditionwatcher.ViewMatchWaiter.waitForView;
import static org.hamcrest.core.AllOf.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class UISmallPlayerTest {

    @Rule
    public ActivityTestRule<ActivityMain> activityRule
            = new ActivityTestRule<>(ActivityMain.class);

    @Before
    public void setUp() {
        TestUtils.populateHistory(ApplicationProvider.getApplicationContext(), 1);
    }

    private Matcher<View> getPlayButton() {
        return allOf(withId(R.id.buttonPlay), isDisplayingAtLeast(80));
    }

    private boolean isMusicPlaying() {
        AudioManager manager = (AudioManager) ApplicationProvider.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        return manager.isMusicActive();
    }

    @Test
    public void stationListItem_ShouldStartPlayBack_WhenClicked() {
        onView(allOf(withId(R.id.layoutMain), isDescendantOfA(withRecyclerView(R.id.recyclerViewStations).atPosition(0)))).perform(ViewActions.click());

        onView(getPlayButton()).check(matches(withContentDescription(R.string.detail_pause)));

        ConditionWatcher.waitForCondition(new IsMusicPlayingCondition(true), ConditionWatcher.SHORT_WAIT_POLICY);
    }

    @Test
    public void playBackState_ShouldBeCorrect_AfterRapidToggling() {
        // TODO: Make clicking more rapid. there is a visible delay as of now.

        Matcher<View> btnPlay = getPlayButton();
        for (int i = 0; i < 7; i++) {
            onView(btnPlay).perform(ViewActions.click());
        }

        waitForView(btnPlay).toMatch(withContentDescription(R.string.detail_pause));

        ConditionWatcher.waitForCondition(new IsMusicPlayingCondition(true), ConditionWatcher.SHORT_WAIT_POLICY);

        for (int i = 0; i < 7; i++) {
            onView(btnPlay).perform(ViewActions.click());
        }

        ConditionWatcher.waitForCondition(new IsMusicPlayingCondition(false), ConditionWatcher.SHORT_WAIT_POLICY);
    }
}
