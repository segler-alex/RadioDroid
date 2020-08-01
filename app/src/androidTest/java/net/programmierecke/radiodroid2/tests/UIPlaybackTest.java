package net.programmierecke.radiodroid2.tests;

import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.tests.utils.TestUtils;
import net.programmierecke.radiodroid2.tests.utils.conditionwatcher.ConditionWatcher;
import net.programmierecke.radiodroid2.tests.utils.conditionwatcher.IsMusicPlayingCondition;
import net.programmierecke.radiodroid2.tests.utils.http.MockHttpDispatcher;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.mockwebserver.MockResponse;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.programmierecke.radiodroid2.tests.utils.conditionwatcher.ViewMatchWaiter.waitForView;
import static org.hamcrest.core.AllOf.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class UIPlaybackTest {

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

    @Ignore("Disabled until implemented")
    @Test
    public void error_ShouldAppear_OnStreamErrorCode() {
        ((CustomTestRunner) InstrumentationRegistry.getInstrumentation()).setCustomRequestDispatcher(path -> {
            if (MockHttpDispatcher.isAudioRequest.compatible(path)) {
                return new MockResponse().setResponseCode(404);
            }
            return null;
        });

        Matcher<View> btnPlay = getPlayButton();

        onView(btnPlay).perform(ViewActions.click());

        waitForView(btnPlay).toMatch(withContentDescription(R.string.detail_play));

        ConditionWatcher.waitForCondition(new IsMusicPlayingCondition(false), ConditionWatcher.SHORT_WAIT_POLICY);
    }

    @Test
    public void error_ShouldAppear_OnStreamPayWall() {
        ((CustomTestRunner) InstrumentationRegistry.getInstrumentation()).setCustomRequestDispatcher(path -> {
            if (MockHttpDispatcher.isAudioRequest.compatible(path)) {
                return new MockResponse().setResponseCode(200).setHeader("Content-Type", "text/html");
            }
            return null;
        });

        Matcher<View> btnPlay = getPlayButton();

        onView(btnPlay).perform(ViewActions.click());

        waitForView(btnPlay).toMatch(withContentDescription(R.string.detail_play));

        ConditionWatcher.waitForCondition(new IsMusicPlayingCondition(false), ConditionWatcher.SHORT_WAIT_POLICY);
    }
}
