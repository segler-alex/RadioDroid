package net.programmierecke.radiodroid2.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerItemMoveAndSwipeHelper<ViewHolderType extends SwipeableViewHolder> extends RecyclerItemSwipeHelper {

    public interface MoveAndSwipeCallback<ViewHolderType> extends SwipeCallback<ViewHolderType>  {
        void onMoved(ViewHolderType viewHolder, int from, int to);
        void onMoveEnded(ViewHolderType viewHolder);
    }

    private MoveAndSwipeCallback<ViewHolderType> moveAndSwipeListener;

    @SuppressWarnings("unchecked")
    public RecyclerItemMoveAndSwipeHelper(int dragDirs, int swipeDirs, MoveAndSwipeCallback<ViewHolderType> moveAndSwipeListener) {
        super(dragDirs, swipeDirs, moveAndSwipeListener);
        this.moveAndSwipeListener = moveAndSwipeListener;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return true;
    }

    @Override
    public void onMoved(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, int fromPos, @NonNull RecyclerView.ViewHolder target, int toPos, int x, int y) {
        @SuppressWarnings("unchecked")
        ViewHolderType viewHolderType = (ViewHolderType) viewHolder;
        moveAndSwipeListener.onMoved(viewHolderType, fromPos, toPos);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        @SuppressWarnings("unchecked")
        ViewHolderType viewHolderType = (ViewHolderType) viewHolder;
        super.clearView(recyclerView, viewHolder);
        moveAndSwipeListener.onMoveEnded(viewHolderType);
    }
}
