package net.programmierecke.radiodroid2.tests.utils.conditionwatcher;

import android.view.View;

import androidx.test.espresso.ViewAssertion;

import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import javax.annotation.Nonnull;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

/**
 * Inspired by https://github.com/AzimoLabs/ConditionWatcher/issues/7
 */
public class ViewMatchWaiter {
    private Matcher<View> viewMatcher;
    private ConditionWatcher.Policy policy;

    public ViewMatchWaiter(@Nonnull Matcher<View> viewMatcher) {
        this.viewMatcher = viewMatcher;
        this.policy = ConditionWatcher.SHORT_WAIT_POLICY;
    }

    public ViewMatchWaiter(@Nonnull Matcher<View> viewMatcher, @Nonnull ConditionWatcher.Policy policy) {
        this.viewMatcher = viewMatcher;
        this.policy = policy;
    }

    public static ViewMatchWaiter waitForView(@Nonnull Matcher<View> viewMatcher) {
        return new ViewMatchWaiter(viewMatcher);
    }

    public void toMatch(@Nonnull Matcher<View> viewChecker) {
        ConditionWatcher.waitForCondition(new ConditionWatcher.Condition() {
            @Override
            public boolean testCondition() {
                try {
                    onView(viewMatcher).check(matches(viewChecker));
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            }

            @Nonnull
            @Override
            public String getDescription() {
                StringDescription description = new StringDescription();
                description.appendText("Wait for view ");
                viewMatcher.describeTo(description);
                description.appendText(" to match ");
                viewChecker.describeTo(description);
                return description.toString();
            }
        }, policy);
    }

    public void toCheck(@Nonnull ViewAssertion viewAssertion) {
        ConditionWatcher.waitForCondition(new ConditionWatcher.Condition() {
            @Override
            public boolean testCondition() {
                try {
                    onView(viewMatcher).check(viewAssertion);
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            }

            @Nonnull
            @Override
            public String getDescription() {
                StringDescription description = new StringDescription();
                description.appendText("Wait for view ");
                viewMatcher.describeTo(description);
                description.appendText(" to match ");
                description.appendText(viewAssertion.toString());
                return description.toString();
            }
        }, policy);
    }
}
