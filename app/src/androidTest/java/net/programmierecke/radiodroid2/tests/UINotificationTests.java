package net.programmierecke.radiodroid2.tests;

import android.content.Context;
import android.media.AudioManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.tests.utils.TestUtils;
import net.programmierecke.radiodroid2.tests.utils.conditionwatcher.ConditionWatcher;
import net.programmierecke.radiodroid2.tests.utils.conditionwatcher.IsMusicPlayingCondition;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Nonnull;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertNotNull;

@LargeTest
@RunWith(AndroidJUnit4.class)
// UI notifications currently only work with API 26+
@SdkSuppress(minSdkVersion = 26)
public class UINotificationTests {

    @Rule
    public ActivityTestRule<ActivityMain> activityRule
            = new ActivityTestRule<>(ActivityMain.class);

    @Before
    public void setUp() {
        TestUtils.populateHistory(ApplicationProvider.getApplicationContext(), 1);
    }

    private ViewInteraction getPlayButton() {
        return onView(allOf(withId(R.id.buttonPlay), isDisplayingAtLeast(80)));
    }

    private void launchPausedNotification() {
        ViewInteraction btnPlay = getPlayButton();

        btnPlay.perform(ViewActions.click(), ViewActions.click());
    }

    private void launchPlayingNotification() {
        ViewInteraction btnPlay = getPlayButton();

        btnPlay.perform(ViewActions.click());
    }

    private static class UIObjectWaiterCondition implements ConditionWatcher.Condition {
        private UiDevice uiDevice;
        private BySelector bySelector;

        public UIObjectWaiterCondition(UiDevice uiDevice, BySelector bySelector) {
            this.uiDevice = uiDevice;
            this.bySelector = bySelector;
        }

        @Override
        public boolean testCondition() {
            return uiDevice.findObject(bySelector) != null;
        }

        @Nonnull
        @Override
        public String getDescription() {
            return "Wait for any view to match " + bySelector.toString();
        }
    }

    private void waitForObject(UiDevice uiDevice, BySelector bySelector) {
        ConditionWatcher.waitForCondition(
                new UIObjectWaiterCondition(uiDevice, bySelector),
                ConditionWatcher.SHORT_WAIT_POLICY);
    }

    @Test
    public void playback_ShouldStart_OnResumeFromNotification() {
        launchPausedNotification();

        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.openNotification();

        String expectedAppName = ApplicationProvider.getApplicationContext().getString(R.string.app_name);
        uiDevice.wait(Until.hasObject(By.textStartsWith(expectedAppName)), 250);
        uiDevice.wait(Until.hasObject(By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_resume))), 250);

        UiObject2 resumeBtn = uiDevice.findObject(By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_resume)));
        assertNotNull(resumeBtn);

        resumeBtn.click();

        waitForObject(uiDevice, By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_pause)));

        ConditionWatcher.waitForCondition(new IsMusicPlayingCondition(true), ConditionWatcher.SHORT_WAIT_POLICY);
    }

    @Test
    public void playback_ShouldPause_OnPauseFromNotification() {
        launchPlayingNotification();

        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.openNotification();

        String expectedAppName = ApplicationProvider.getApplicationContext().getString(R.string.app_name);
        uiDevice.wait(Until.hasObject(By.textStartsWith(expectedAppName)), 250);
        uiDevice.wait(Until.hasObject(By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_pause))), 250);

        UiObject2 pauseBtn = uiDevice.findObject(By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_pause)));
        assertNotNull(pauseBtn);

        pauseBtn.click();

        waitForObject(uiDevice, By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_resume)));

        ConditionWatcher.waitForCondition(new IsMusicPlayingCondition(false), ConditionWatcher.SHORT_WAIT_POLICY);
    }

    @Test
    public void notification_ShouldDisappear_OnStopFromNotification() {
        launchPlayingNotification();

        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.openNotification();

        String expectedAppName = ApplicationProvider.getApplicationContext().getString(R.string.app_name);
        uiDevice.wait(Until.hasObject(By.textStartsWith(expectedAppName)), 250);
        uiDevice.wait(Until.hasObject(By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_stop))), 250);

        UiObject2 stopBtn = uiDevice.findObject(By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_stop)));
        assertNotNull(stopBtn);

        stopBtn.click();

        ConditionWatcher.waitForCondition(new ConditionWatcher.Condition() {
            @Override
            public boolean testCondition() {
                return uiDevice.findObject(By.textStartsWith(expectedAppName)) == null;
            }

            @Nonnull
            @Override
            public String getDescription() {
                return "Wait for notification to disappear";
            }
        }, ConditionWatcher.SHORT_WAIT_POLICY);

        ConditionWatcher.waitForCondition(new IsMusicPlayingCondition(false), ConditionWatcher.SHORT_WAIT_POLICY);
    }
}
