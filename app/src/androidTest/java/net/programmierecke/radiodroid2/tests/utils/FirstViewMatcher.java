// https://stackoverflow.com/questions/29378552/in-espresso-how-to-avoid-ambiguousviewmatcherexception-when-multiple-views-matc

package net.programmierecke.radiodroid2.tests.utils;

import android.view.View;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class FirstViewMatcher extends BaseMatcher<View> {


    public static boolean matchedBefore = false;

    public FirstViewMatcher() {
        matchedBefore = false;
    }

    public static <T> Matcher<View> firstView() {
        return new FirstViewMatcher();
    }

    @Override
    public boolean matches(Object o) {
        if (matchedBefore) {
            return false;
        } else {
            matchedBefore = true;
            return true;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(" is the first view that comes along ");
    }
}
