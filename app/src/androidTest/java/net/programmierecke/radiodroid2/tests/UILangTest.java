package net.programmierecke.radiodroid2.tests;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.action.ViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.yariksoffice.lingver.Lingver;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.tests.utils.TestUtils;
import net.programmierecke.radiodroid2.tests.utils.ViewPagerIdlingResource;

import org.hamcrest.core.AllOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Locale;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static net.programmierecke.radiodroid2.tests.utils.RecyclerViewMatcher.withRecyclerView;

@LargeTest
@RunWith(Parameterized.class)
public class UILangTest {
    @Parameterized.Parameter(value = 0)
    public Locale locale = Locale.ENGLISH;

    @Parameterized.Parameters(name = "locale={0}")
    public static Iterable<Object[]> initParameters() {
        return Arrays.asList(new Object[][]{
                {new Locale("en")},
                {new Locale("ca")},
                {new Locale("cs")},
                {new Locale("da")},
                {new Locale("de")},
                {new Locale("el")},
                {new Locale("es")},
                {new Locale("fi")},
                {new Locale("fr")},
                {new Locale("hu")},
                {new Locale("in")},
                {new Locale("nl")},
                {new Locale("nn")},
                {new Locale("pl")},
                {new Locale("ru")},
                {new Locale("sk")},
                {new Locale("tr")},
                {new Locale("zh")},
        });
    }

    @Rule
    public ActivityTestRule<ActivityMain> activityRule
            = new ActivityTestRule<ActivityMain>(ActivityMain.class) {
        @Override
        protected void beforeActivityLaunched() {
            Lingver.init(ApplicationProvider.getApplicationContext(), locale);
            super.beforeActivityLaunched();
        }
    };

    private static final int STATIONS_COUNT = 20;

    @Before
    public void setUp() {
        TestUtils.populateFavourites(ApplicationProvider.getApplicationContext(), STATIONS_COUNT);
        TestUtils.populateHistory(ApplicationProvider.getApplicationContext(), STATIONS_COUNT);
    }

    @Test
    public void application_ShouldNotCrash_WithLanguage() {
        ViewPagerIdlingResource idlingResource = new ViewPagerIdlingResource(activityRule.getActivity().findViewById(R.id.viewpager), "ViewPager");
        IdlingRegistry.getInstance().register(idlingResource);

        {
            onView(AllOf.allOf(withId(R.id.buttonMore), isDescendantOfA(withRecyclerView(R.id.recyclerViewStations).atPosition(0)))).perform(ViewActions.click());
        }

        {
            onView(withId(R.id.nav_item_starred)).perform(ViewActions.click());
        }

        {
            onView(withId(R.id.nav_item_history)).perform(ViewActions.click());
        }

        {
            onView(withId(R.id.nav_item_alarm)).perform(ViewActions.click());
        }

        {
            onView(withId(R.id.nav_item_settings)).perform(ViewActions.click());
        }

        {
            onView(withId(R.id.fragment_player_small)).perform(ViewActions.swipeUp());
        }
    }
}
