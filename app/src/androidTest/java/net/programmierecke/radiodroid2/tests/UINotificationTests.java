package net.programmierecke.radiodroid2.tests;

import android.content.Context;
import android.media.AudioManager;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.tests.utils.TestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@LargeTest
@RunWith(AndroidJUnit4.class)
// UiDevice can be used only with API 18+
@SdkSuppress(minSdkVersion = 18)
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

    private boolean isMusicPlaying() {
        AudioManager manager = (AudioManager) ApplicationProvider.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        return manager.isMusicActive();
    }

    private void launchPausedNotification() {
        ViewInteraction btnPlay = getPlayButton();

        btnPlay.perform(ViewActions.click(), ViewActions.click());
    }

    private void launchPlayingNotification() {
        ViewInteraction btnPlay = getPlayButton();

        btnPlay.perform(ViewActions.click());
    }

    @Test
    public void playback_ShouldStart_OnResumeFromNotification() {
        launchPausedNotification();

        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.openNotification();

        String expectedAppName = ApplicationProvider.getApplicationContext().getString(R.string.app_name);
        uiDevice.wait(Until.hasObject(By.textStartsWith(expectedAppName)), 100);

        UiObject2 resumeBtn = uiDevice.findObject(By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_resume)));
        assertNotNull(resumeBtn);

        resumeBtn.click();

        SystemClock.sleep(200);

        UiObject2 pauseBtn = uiDevice.findObject(By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_pause)));
        assertNotNull(pauseBtn);
        assertTrue(isMusicPlaying());
    }

    @Test
    public void playback_ShouldPause_OnPauseFromNotification() {
        launchPlayingNotification();

        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.openNotification();

        String expectedAppName = ApplicationProvider.getApplicationContext().getString(R.string.app_name);
        uiDevice.wait(Until.hasObject(By.textStartsWith(expectedAppName)), 100);

        UiObject2 pauseBtn = uiDevice.findObject(By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_pause)));
        assertNotNull(pauseBtn);

        pauseBtn.click();

        SystemClock.sleep(200);

        UiObject2 resumeBtn = uiDevice.findObject(By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_resume)));
        assertNotNull(resumeBtn);

        assertFalse(isMusicPlaying());
    }

    @Test
    public void notification_ShouldDisappear_OnStopFromNotification() {
        launchPlayingNotification();

        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.openNotification();

        String expectedAppName = ApplicationProvider.getApplicationContext().getString(R.string.app_name);
        uiDevice.wait(Until.hasObject(By.textStartsWith(expectedAppName)), 100);

        UiObject2 stopBtn = uiDevice.findObject(By.desc(ApplicationProvider.getApplicationContext().getString(R.string.action_stop)));
        assertNotNull(stopBtn);

        stopBtn.click();

        assertNull(uiDevice.findObject(By.textStartsWith(expectedAppName)));
        assertFalse(isMusicPlaying());
    }
}
