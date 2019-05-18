package net.programmierecke.radiodroid2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;


/**
 * Credits to Alex Lockwood blogpost "Experimenting with Nested Scrolling"
 * Credits to https://stackoverflow.com/questions/31829976/onclick-method-not-working-properly-after-nestedscrollview-scrolled
 * <p>
 * Allows scroll view to have {@link RecyclerView} alongside with other content in it and be scrolled
 * as expected by user.
 * <p>
 * The NestedScrollView should steal the scroll/fling events away from
 * the RecyclerView if either is true:
 * - the user is dragging their finger down and the RecyclerView is scrolled to the top of its content
 * - the user is dragging their finger up and the NestedScrollView is not scrolled to the bottom of its content.
 */
public class RecyclerAwareNestedScrollView extends NestedScrollView {
    private OverScroller mScroller;
    public boolean isFling = false;

    public RecyclerAwareNestedScrollView(@NonNull Context context) {
        this(context, null);
    }

    public RecyclerAwareNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerAwareNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = getOverScroller();
    }

    @Override
    public void fling(int velocityY) {
        super.fling(velocityY);

        // Here we effectively extend the super class functionality for backwards compatibility and just call invalidateOnAnimation()
        if (getChildCount() > 0) {
            ViewCompat.postInvalidateOnAnimation(this);

            // Initializing isFling to true to track fling action in onScrollChanged() method
            isFling = true;
        }
    }

    @Override
    protected void onScrollChanged(int l, final int t, final int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if (isFling) {
            if (Math.abs(t - oldt) <= 3 || t == 0 || t == (getChildAt(0).getMeasuredHeight() - getMeasuredHeight())) {
                isFling = false;

                // This forces the mFinish variable in scroller to true and does the trick
                if (mScroller != null) {
                    mScroller.abortAnimation();
                }
            }
        }
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        final RecyclerView rv = (RecyclerView) target;
        if ((dy < 0 && isRvScrolledToTop(rv)) || (dy > 0 && !isNsvScrolledToBottom(this))) {
            // Scroll the NestedScrollView's content and record the number of pixels consumed
            // (so that the RecyclerView will know not to perform the scroll as well).
            scrollBy(0, dy);
            consumed[1] = dy;
            return;
        }
        super.onNestedPreScroll(target, dx, dy, consumed, type);
    }

    @Override
    public boolean onNestedPreFling(View target, float velX, float velY) {
        final RecyclerView rv = (RecyclerView) target;
        if ((velY < 0 && isRvScrolledToTop(rv)) || (velY > 0 && !isNsvScrolledToBottom(this))) {
            // Fling the NestedScrollView's content and return true (so that the RecyclerView
            // will know not to perform the fling as well).
            fling((int) velY);
            return true;
        }
        return super.onNestedPreFling(target, velX, velY);
    }

    /**
     * Returns true if the NestedScrollView is scrolled to the bottom of its
     * content (i.e. if the card's inner RecyclerView is completely visible).
     */
    private static boolean isNsvScrolledToBottom(NestedScrollView nsv) {
        return !nsv.canScrollVertically(1);
    }

    /**
     * Returns true iff the RecyclerView is scrolled to the top of its
     * content (i.e. if the RecyclerView's first item is completely visible).
     */
    private static boolean isRvScrolledToTop(RecyclerView rv) {
        final LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
        return lm.findFirstVisibleItemPosition() == 0
                && lm.findViewByPosition(0).getTop() == 0;
    }

    private OverScroller getOverScroller() {
        Field fs;
        try {
            fs = this.getClass().getSuperclass().getDeclaredField("mScroller");
            fs.setAccessible(true);
            return (OverScroller) fs.get(this);
        } catch (Throwable t) {
            return null;
        }
    }
}
