package net.programmierecke.radiodroid2.utils;

import android.graphics.Canvas;
import android.view.View;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerItemSwipeHelper<ViewHolderType extends SwipeableViewHolder> extends ItemTouchHelper.SimpleCallback {

    public interface SwipeCallback<ViewHolderType> {
        void onSwiped(ViewHolderType viewHolder, int direction);
    }

    private SwipeCallback<ViewHolderType> swipeListener;

    public RecyclerItemSwipeHelper(int dragDirs, int swipeDirs, SwipeCallback<ViewHolderType> swipeListener) {
        super(dragDirs, swipeDirs);
        this.swipeListener = swipeListener;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (viewHolder != null) {
            final View foregroundView = ((SwipeableViewHolder) viewHolder).getForegroundView();
            getDefaultUIUtil().onSelected(foregroundView);
        }
    }

    @Override
    public void onChildDrawOver(Canvas c, RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
        final View foregroundView = ((SwipeableViewHolder) viewHolder).getForegroundView();
        getDefaultUIUtil().onDrawOver(c, recyclerView, foregroundView, dX, dY,
                actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        final View foregroundView = ((SwipeableViewHolder) viewHolder).getForegroundView();
        getDefaultUIUtil().clearView(foregroundView);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        final View foregroundView = ((SwipeableViewHolder) viewHolder).getForegroundView();

        getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY,
                actionState, isCurrentlyActive);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        @SuppressWarnings("unchecked")
        ViewHolderType viewHolderType = (ViewHolderType) viewHolder;
        swipeListener.onSwiped(viewHolderType, direction);
    }
}
