package net.programmierecke.radiodroid2.station;

import android.util.Log;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.github.zawadz88.materialpopupmenu.MaterialPopupMenu;

import net.programmierecke.radiodroid2.utils.RecyclerItemMoveAndSwipeHelper;
import net.programmierecke.radiodroid2.utils.SwipeableViewHolder;

public class ItemAdapaterContextMenuStation extends ItemAdapterStation implements RecyclerItemMoveAndSwipeHelper.MoveAndSwipeCallback<ItemAdapterStation.StationViewHolder> {
    private static final String TAG = "IconOnlyStation";
    private static final long MIN_INTERVAL_BETWEEN_DRAG_AND_MENU_OPEN = 200;
    private final double DISMISS_MENU_DRAG_THRESHOLD = 0.15;
    private final long NEVER_IN_THE_FUTURE = Long.MAX_VALUE / 2;
    MaterialPopupMenu contextMenu = null;
    private RecyclerItemMoveAndSwipeHelper<ItemAdapterStation.StationViewHolder> swipeAndMoveHelper = null;
    private long timeLastDragEnded = 0;

    ItemAdapaterContextMenuStation(FragmentActivity fragmentActivity, int resourceId, StationsFilter.FilterType filterType) {
        super(fragmentActivity, resourceId, filterType);
    }

    @Override
    public void onDragged(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, double dX, double dY) {
        final View foregroundView = ((SwipeableViewHolder) viewHolder).getForegroundView();
        final ItemAdapterIconOnlyStation.StationViewHolder stationViewHolder = (ItemAdapterIconOnlyStation.StationViewHolder) viewHolder;

        if (Math.abs(dX) > foregroundView.getWidth() * DISMISS_MENU_DRAG_THRESHOLD ||
                Math.abs(dY) > foregroundView.getHeight() * DISMISS_MENU_DRAG_THRESHOLD) {
            stationViewHolder.dismissContextMenu();
        } else if (stationViewHolder.contextMenu == null) {
            // long-press inside distance tolerance but menu is not yet created
            if (System.currentTimeMillis() > timeLastDragEnded + MIN_INTERVAL_BETWEEN_DRAG_AND_MENU_OPEN) {
                Log.d(TAG, "Creating contextMenu from onDragged");
                stationViewHolder.onCreateContextMenu(null, foregroundView, null);
            }
        } else {
            timeLastDragEnded = NEVER_IN_THE_FUTURE;
        }
    }

    @Override
    public void onMoveEnded(ItemAdapterStation.StationViewHolder viewHolder) {
        timeLastDragEnded = System.currentTimeMillis();
        super.onMoveEnded(viewHolder);
    }
}
