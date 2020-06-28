package net.programmierecke.radiodroid2.tests.utils;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;

public class ScrollToRecyclerItemAction implements ViewAction {
    private static final int VIEW_DISPLAY_PERCENTAGE = 50;

    private int itemIdx;

    public static ScrollToRecyclerItemAction scrollToRecyclerItem(int itemIdx) {
        return new ScrollToRecyclerItemAction(itemIdx);
    }

    public ScrollToRecyclerItemAction(int itemIdx) {
        this.itemIdx = itemIdx;
    }

    @Override
    public Matcher<View> getConstraints() {
        return isDisplayingAtLeast(VIEW_DISPLAY_PERCENTAGE);
    }

    @Override
    public String getDescription() {
        return String.format("scroll to item %d in recycler", itemIdx);
    }

    @Override
    public void perform(UiController uiController, View view) {
        RecyclerView recyclerView = (RecyclerView) view;
        TestUtils.centerItemInRecycler(uiController, recyclerView, itemIdx);
    }
}
