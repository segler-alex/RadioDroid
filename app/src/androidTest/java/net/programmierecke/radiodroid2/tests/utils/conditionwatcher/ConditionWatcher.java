package net.programmierecke.radiodroid2.tests.utils.conditionwatcher;

import androidx.test.espresso.IdlingResource;

import javax.annotation.Nonnull;

/**
 * Inspired by https://github.com/AzimoLabs/ConditionWatcher
 * <p>
 * This is alternative to {@link IdlingResource} when we want to wait for some unique event or
 * IdlingResource is just impractical.
 */
public class ConditionWatcher {

    public interface Policy {
        long getCheckInterval();

        long getTimeoutInterval();
    }

    public static final Policy SHORT_WAIT_POLICY = new Policy() {
        @Override
        public long getCheckInterval() {
            return 50;
        }

        @Override
        public long getTimeoutInterval() {
            return 2000;
        }
    };

    public static final Policy LONG_WAIT_POLICY = new Policy() {
        @Override
        public long getCheckInterval() {
            return 50;
        }

        @Override
        public long getTimeoutInterval() {
            return 3000;
        }
    };

    public interface Condition {
        boolean testCondition();

        @Nonnull
        String getDescription();
    }

    public static void waitForCondition(@Nonnull Condition condition, @Nonnull Policy policy) {
        long elapsedTime = 0;

        while (true) {
            if (condition.testCondition()) {
                break;
            } else {
                elapsedTime += policy.getCheckInterval();
                try {
                    Thread.sleep(policy.getCheckInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (elapsedTime > policy.getTimeoutInterval()) {
                throw new RuntimeException(condition.getDescription() + " - took more than " + policy.getTimeoutInterval() + " ms.");
            }
        }
    }
}
