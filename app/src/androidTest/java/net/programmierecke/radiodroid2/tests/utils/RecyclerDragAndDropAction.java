package net.programmierecke.radiodroid2.tests.utils;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.MotionEvents;
import androidx.test.espresso.action.PrecisionDescriber;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.espresso.action.Swiper;
import androidx.test.espresso.util.HumanReadables;

import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static androidx.test.espresso.core.internal.deps.guava.base.Preconditions.checkElementIndex;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;

public class RecyclerDragAndDropAction implements ViewAction {

    /**
     * The minimum amount of a view that must be displayed in order to swipe across it.
     */
    private static final int VIEW_DISPLAY_PERCENTAGE = 50;

    /**
     * The number of motion events to send for each swipe.
     */
    private static final int DRAG_EVENT_COUNT = 10;

    private static final int DRAG_DURATION = 600;

    private final Swiper swiper;
    private final int idxFrom;
    private final int idxTo;
    private final PrecisionDescriber precisionDescriber;

    public static ViewAction recyclerDragAndDrop(int idxFrom, int idxTo) {
        return new RecyclerDragAndDropAction(Swipe.FAST, idxFrom, idxTo, Press.FINGER);
    }

    public RecyclerDragAndDropAction(Swiper swiper, int idxFrom, int idxTo, PrecisionDescriber precisionDescriber) {
        this.swiper = swiper;
        this.idxFrom = idxFrom;
        this.idxTo = idxTo;
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
            res[i - 1][0] = start[0] + (end[0] - start[0]) * i / (steps);
            res[i - 1][1] = start[1] + (end[1] - start[1]) * i / (steps);
        }

        return res;
    }

    @Override
    public void perform(UiController uiController, View view) {
        final RecyclerView recyclerView = (RecyclerView) view;

        TestUtils.centerItemInRecycler(uiController, recyclerView, idxFrom);
        uiController.loopMainThreadUntilIdle();

        final View fromView = Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(idxFrom)).itemView;
        final float[] fromViewPosition = GeneralLocation.VISIBLE_CENTER.calculateCoordinates(fromView);

        final float[] precision = precisionDescriber.describePrecision();

        final List<MotionEvent> events = new ArrayList<>();
        MotionEvents.DownResultHolder downEvent = null;

        boolean success = false;
        for (int i = 0; i < 3 && !success; i++) {
            try {
                downEvent = MotionEvents.sendDown(uiController, fromViewPosition, precision);

                final int longPressTimeout = (int) (ViewConfiguration.getLongPressTimeout() * 1.5f);
                uiController.loopMainThreadForAtLeast(longPressTimeout);

                TestUtils.centerItemInRecycler(uiController, recyclerView, idxTo);
                uiController.loopMainThreadUntilIdle();

                final View toView = Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(idxTo)).itemView;
                float[] toViewPosition = GeneralLocation.TOP_CENTER.calculateCoordinates(toView);

                float[][] steps = interpolate(fromViewPosition, toViewPosition, DRAG_EVENT_COUNT);

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
                                toViewPosition[0],
                                toViewPosition[1],
                                0));
                uiController.injectMotionEventSequence(events);
                success = true;
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

                if (downEvent != null) {
                    downEvent.down.recycle();
                }
            }
        }

        int duration = ViewConfiguration.getPressedStateDuration();
        // ensures that all work enqueued to process the swipe has been run.
        if (duration > 0) {
            uiController.loopMainThreadForAtLeast(duration);
        }
    }

    @Override
    public String getDescription() {
        return swiper.toString().toLowerCase() + " recycler-drag-and-drop";
    }
}
