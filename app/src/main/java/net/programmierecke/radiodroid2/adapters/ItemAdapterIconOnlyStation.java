package net.programmierecke.radiodroid2.adapters;

import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.programmierecke.radiodroid2.PlayerServiceUtil;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.data.DataRadioStation;
import net.programmierecke.radiodroid2.utils.RecyclerItemMoveAndSwipeHelper;
import net.programmierecke.radiodroid2.utils.SwipeableViewHolder;

public class ItemAdapterIconOnlyStation extends ItemAdapterStation {

    private final String TAG = "AdapterIconOnlyStations";

    class StationViewHolder extends ItemAdapterStation.StationViewHolder implements View.OnClickListener, SwipeableViewHolder {

        StationViewHolder(View itemView) {
            super(itemView);

            viewForeground = itemView.findViewById(R.id.station_icon_foreground);
            frameLayout = itemView.findViewById(R.id.stationIconFrameLayout);

            imageViewIcon = itemView.findViewById(R.id.iconImageViewIcon);
            transparentImageView = itemView.findViewById(R.id.iconTransparentCircle);
            itemView.setOnClickListener(this);
        }
    }

    public ItemAdapterIconOnlyStation(FragmentActivity fragmentActivity, int resourceId) {
        super(fragmentActivity, resourceId);
    }

    @Override
    public StationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(resourceId, parent, false);

        return new StationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ItemAdapterStation.StationViewHolder holder, int position) {
        final DataRadioStation station = stationsList.get(position);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        boolean useCircularIcons = Utils.useCircularIcons(getContext());

        if (!station.IconUrl.isEmpty()) {
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
        RecyclerItemMoveAndSwipeHelper<ItemAdapterStation.StationViewHolder> swipeAndMoveHelper = new RecyclerItemMoveAndSwipeHelper<>(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0, this);
        new ItemTouchHelper(swipeAndMoveHelper).attachToRecyclerView(recyclerView);
    }
}

