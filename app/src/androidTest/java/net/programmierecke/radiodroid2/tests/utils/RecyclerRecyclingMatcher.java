package net.programmierecke.radiodroid2.tests.utils;

import android.util.DisplayMetrics;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class RecyclerRecyclingMatcher {
    private static float MAX_SIZE_K = 1.25f;

    public static Matcher<View> recyclerRecycles() {
        return new TypeSafeMatcher<View>(RecyclerView.class) {
            @Override
            protected boolean matchesSafely(View recyclerView) {
                DisplayMetrics displayMetrics = InstrumentationRegistry.getInstrumentation()
                        .getTargetContext().getResources()
                        .getDisplayMetrics();

                return !(recyclerView.getMeasuredHeight() > displayMetrics.heightPixels * MAX_SIZE_K) &&
                        !(recyclerView.getMeasuredWidth() > displayMetrics.widthPixels * MAX_SIZE_K);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("as a Recycler does recycling of elements");
            }
        };
    }
}
