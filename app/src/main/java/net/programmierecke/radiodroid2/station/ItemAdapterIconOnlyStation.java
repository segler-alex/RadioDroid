package net.programmierecke.radiodroid2.station;

import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.github.zawadz88.materialpopupmenu.MaterialPopupMenu;

import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.service.PlayerServiceUtil;
import net.programmierecke.radiodroid2.utils.RecyclerItemMoveAndSwipeHelper;
import net.programmierecke.radiodroid2.utils.SwipeableViewHolder;

public class ItemAdapterIconOnlyStation extends ItemAdapterStation implements RecyclerItemMoveAndSwipeHelper.MoveAndSwipeCallback<ItemAdapterStation.StationViewHolder> {
    private RecyclerItemMoveAndSwipeHelper<ItemAdapterStation.StationViewHolder> swipeAndMoveHelper = null;

    @Override
    public void onDragged(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, double dX, double dY) {
        final View foregroundView = ((SwipeableViewHolder) viewHolder).getForegroundView();
        double dismissMenuThreshold = (swipeAndMoveHelper == null ? 0.1 : swipeAndMoveHelper.getMoveThreshold(viewHolder));
        if (dX > foregroundView.getWidth() * dismissMenuThreshold ||
                dY > foregroundView.getHeight() * dismissMenuThreshold) {
            ((StationViewHolder) viewHolder).dismissContextMenu();
        }
    }

    class StationViewHolder extends ItemAdapterStation.StationViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener, SwipeableViewHolder {
        MaterialPopupMenu materialPopupMenu = null;

        StationViewHolder(View itemView) {
            super(itemView);

            viewForeground = itemView.findViewById(R.id.station_icon_foreground);
            frameLayout = itemView.findViewById(R.id.stationIconFrameLayout);

            imageViewIcon = itemView.findViewById(R.id.iconImageViewIcon);
            transparentImageView = itemView.findViewById(R.id.iconTransparentCircle);
            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);
        }

        public void dismissContextMenu() {
            if (materialPopupMenu != null) {
                materialPopupMenu.dismiss();
                materialPopupMenu = null;
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            if (materialPopupMenu != null)
                return;

            int pos = getAdapterPosition();
            DataRadioStation station = filteredStationsList.get(pos);
            materialPopupMenu = StationPopupMenu.INSTANCE.open(v, getContext(), activity, station, ItemAdapterIconOnlyStation.this);
            materialPopupMenu.setOnDismissListener(() -> {
                dismissContextMenu();
                return null;
            });
        }
    }

    public ItemAdapterIconOnlyStation(FragmentActivity fragmentActivity, int resourceId, StationsFilter.FilterType filterType) {
        super(fragmentActivity, resourceId, filterType);
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(resourceId, parent, false);

        return new StationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ItemAdapterStation.StationViewHolder holder, int position) {
        final DataRadioStation station = filteredStationsList.get(position);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        boolean useCircularIcons = Utils.useCircularIcons(getContext());

        if (station.hasIcon()) {
            setupIcon(useCircularIcons, holder.imageViewIcon, holder.transparentImageView);
            PlayerServiceUtil.getStationIcon(holder.imageViewIcon, station.IconUrl);
        } else {
            holder.imageViewIcon.setImageDrawable(stationImagePlaceholder);
        }

        TypedValue tv = new TypedValue();
        if (playingStationPosition == position) {
            getContext().getTheme().resolveAttribute(R.attr.colorAccentMy, tv, true);
            holder.frameLayout.setBackgroundColor(tv.data);
            holder.transparentImageView.setColorFilter(tv.data);
        } else {
            getContext().getTheme().resolveAttribute(R.attr.boxBackgroundColor, tv, true);
            holder.frameLayout.setBackgroundColor(tv.data);
        }

    }

    public void enableItemMove(RecyclerView recyclerView) {
        swipeAndMoveHelper = new RecyclerItemMoveAndSwipeHelper<>(getContext(), ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0, this);
        new ItemTouchHelper(swipeAndMoveHelper).attachToRecyclerView(recyclerView);
    }
}

