package net.programmierecke.radiodroid2.tests.utils;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.MotionEvents;
import androidx.test.espresso.action.PrecisionDescriber;
import androidx.test.espresso.action.Swiper;
import androidx.test.espresso.util.HumanReadables;

import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.List;

import static androidx.test.espresso.core.internal.deps.guava.base.Preconditions.checkElementIndex;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;

public class GeneralDragAndDropAction implements ViewAction {

    /**
     * The minimum amount of a view that must be displayed in order to swipe across it.
     */
    private static final int VIEW_DISPLAY_PERCENTAGE = 90;

    /**
     * The number of motion events to send for each swipe.
     */
    private static final int DRAG_EVENT_COUNT = 10;

    private static final int DRAG_DURATION = 300;

    protected CoordinatesProvider startCoordinatesProvider;
    protected CoordinatesProvider endCoordinatesProvider;
    private final Swiper swiper;
    private final PrecisionDescriber precisionDescriber;

    public GeneralDragAndDropAction(
            Swiper swiper,
            CoordinatesProvider startCoordinatesProvider,
            CoordinatesProvider endCoordinatesProvider,
            PrecisionDescriber precisionDescriber) {
        this.swiper = swiper;
        this.startCoordinatesProvider = startCoordinatesProvider;
        this.endCoordinatesProvider = endCoordinatesProvider;
        this.precisionDescriber = precisionDescriber;
    }

    @Override
    public Matcher<View> getConstraints() {
        return isDisplayingAtLeast(VIEW_DISPLAY_PERCENTAGE);
    }

    private static float[][] interpolate(float[] start, float[] end, int steps) {
        checkElementIndex(1, start.length);
        checkElementIndex(1, end.length);

        float[][] res = new float[steps][2];

        for (int i = 1; i < steps + 1; i++) {
            res[i - 1][0] = start[0] + (end[0] - start[0]) * i / (steps + 2f);
            res[i - 1][1] = start[1] + (end[1] - start[1]) * i / (steps + 2f);
        }

        return res;
    }

    @Override
    public void perform(UiController uiController, View view) {
        float[] startCoordinates = startCoordinatesProvider.calculateCoordinates(view);
        float[] endCoordinates = endCoordinatesProvider.calculateCoordinates(view);
        float[] precision = precisionDescriber.describePrecision();

        List<MotionEvent> events = new ArrayList<>();
        MotionEvents.DownResultHolder downEvent = null;

        try {
            downEvent = MotionEvents.sendDown(uiController, startCoordinates, precision);

            int longPressTimeout = (int) (ViewConfiguration.getLongPressTimeout() * 1.5f);
            uiController.loopMainThreadForAtLeast(longPressTimeout);

            float[][] steps = interpolate(startCoordinates, endCoordinates, DRAG_EVENT_COUNT);

            final long intervalMS = DRAG_DURATION / steps.length;
            long eventTime = downEvent.down.getDownTime();
            for (float[] step : steps) {
                eventTime += intervalMS;
                events.add(MotionEvents.obtainMovement(downEvent.down.getDownTime(), eventTime, step));
            }

            eventTime += intervalMS;
            events.add(
                    MotionEvent.obtain(
                            downEvent.down.getDownTime(),
                            eventTime,
                            MotionEvent.ACTION_UP,
                            endCoordinates[0],
                            endCoordinates[1],
                            0));
            uiController.injectMotionEventSequence(events);
        } catch (Exception e) {
            throw new PerformException.Builder()
                    .withActionDescription(this.getDescription())
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(e)
                    .build();
        } finally {
            for (MotionEvent event : events) {
                event.recycle();
            }

            downEvent.down.recycle();
        }

        int duration = ViewConfiguration.getPressedStateDuration();
        // ensures that all work enqueued to process the swipe has been run.
        if (duration > 0) {
            uiController.loopMainThreadForAtLeast(duration);
        }
    }

    @Override
    public String getDescription() {
        return swiper.toString().toLowerCase() + " drag-and-drop";
    }
}
