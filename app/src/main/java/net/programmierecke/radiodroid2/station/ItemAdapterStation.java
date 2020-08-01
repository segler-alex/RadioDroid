package net.programmierecke.radiodroid2.station;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.*;

import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;
import com.mikepenz.iconics.view.IconicsImageButton;

import net.programmierecke.radiodroid2.*;
import net.programmierecke.radiodroid2.interfaces.IAdapterRefreshable;
import net.programmierecke.radiodroid2.players.PlayStationTask;
import net.programmierecke.radiodroid2.players.selector.PlayerType;
import net.programmierecke.radiodroid2.utils.RecyclerItemMoveAndSwipeHelper;
import net.programmierecke.radiodroid2.service.PlayerService;
import net.programmierecke.radiodroid2.service.PlayerServiceUtil;
import net.programmierecke.radiodroid2.utils.RecyclerItemSwipeHelper;
import net.programmierecke.radiodroid2.utils.SwipeableViewHolder;
import net.programmierecke.radiodroid2.views.TagsView;

public class ItemAdapterStation
        extends RecyclerView.Adapter<ItemAdapterStation.StationViewHolder>
        implements RecyclerItemMoveAndSwipeHelper.MoveAndSwipeCallback<ItemAdapterStation.StationViewHolder> {

    public interface StationActionsListener {
        void onStationClick(DataRadioStation station, int pos);

        void onStationMoved(int from, int to);

        void onStationSwiped(DataRadioStation station);

        void onStationMoveFinished();
    }

    public interface FilterListener {
        void onSearchCompleted(StationsFilter.SearchStatus searchStatus);
    }

    private final String TAG = "AdapterStations";

    List<DataRadioStation> stationsList;
    List<DataRadioStation> filteredStationsList = new ArrayList<>();

    int resourceId;

    StationActionsListener stationActionsListener;
    private FilterListener filterListener;
    private boolean supportsStationRemoval = false;
    private StationsFilter.FilterType filterType = StationsFilter.FilterType.LOCAL;

    private boolean shouldLoadIcons;

    private IAdapterRefreshable refreshable;
    FragmentActivity activity;

    private BroadcastReceiver updateUIReceiver;

    private int expandedPosition = -1;
    public int playingStationPosition = -1;

    Drawable stationImagePlaceholder;

    private FavouriteManager favouriteManager;

    private StationsFilter filter;

    private TagsView.TagSelectionCallback tagSelectionCallback = new TagsView.TagSelectionCallback() {
        @Override
        public void onTagSelected(String tag) {
            Intent i = new Intent(getContext(), ActivityMain.class);
            i.putExtra(ActivityMain.EXTRA_SEARCH_TAG, tag);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        }
    };

    class StationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, SwipeableViewHolder {
        View viewForeground;
        LinearLayout layoutMain;
        FrameLayout frameLayout;

        ImageView imageViewIcon;
        ImageView transparentImageView;
        ImageView starredStatusIcon;
        TextView textViewTitle;
        TextView textViewShortDescription;
        TextView textViewTags;
        ImageButton buttonMore;

        View viewDetails;
        ViewStub stubDetails;
        IconicsImageButton buttonVisitWebsite;
        ImageButton buttonBookmark;
        ImageButton buttonShare;
        ImageView imageTrend;
        ImageButton buttonAddAlarm;
        TagsView viewTags;
        ImageButton buttonCreateShortcut;
        ImageButton buttonPlayInternalOrExternal;

        StationViewHolder(View itemView) {
            super(itemView);

            viewForeground = itemView.findViewById(R.id.station_foreground);
            layoutMain = itemView.findViewById(R.id.layoutMain);
            frameLayout = itemView.findViewById(R.id.frameLayout);

            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            imageTrend = itemView.findViewById(R.id.trendStatusIcon);
            transparentImageView = itemView.findViewById(R.id.transparentCircle);
            starredStatusIcon = itemView.findViewById(R.id.starredStatusIcon);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewShortDescription = itemView.findViewById(R.id.textViewShortDescription);
            textViewTags = itemView.findViewById(R.id.textViewTags);
            buttonMore = itemView.findViewById(R.id.buttonMore);
            stubDetails = itemView.findViewById(R.id.stubDetails);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (stationActionsListener != null) {
                int pos = getAdapterPosition();
                stationActionsListener.onStationClick(filteredStationsList.get(pos), pos);
            }
        }

        @Override
        public View getForegroundView() {
            return viewForeground;
        }
    }

    public ItemAdapterStation(FragmentActivity fragmentActivity, int resourceId, StationsFilter.FilterType filterType) {
        this.activity = fragmentActivity;
        this.resourceId = resourceId;
        this.filterType = filterType;

        stationImagePlaceholder = ContextCompat.getDrawable(fragmentActivity, R.drawable.ic_photo_24dp);

        RadioDroidApp radioDroidApp = (RadioDroidApp) fragmentActivity.getApplication();
        favouriteManager = radioDroidApp.getFavouriteManager();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayerService.PLAYER_SERVICE_META_UPDATE);
        filter.addAction(DataRadioStation.RADIO_STATION_LOCAL_INFO_CHAGED);

        this.updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }

                switch (intent.getAction()) {
                    case PlayerService.PLAYER_SERVICE_META_UPDATE:
                        highlightCurrentStation();
                        break;
                    case DataRadioStation.RADIO_STATION_LOCAL_INFO_CHAGED:
                        String uuid = intent.getStringExtra(DataRadioStation.RADIO_STATION_UUID);
                        notifyChangedByStationUuid(uuid);
                        break;
                }

            }
        };

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(this.updateUIReceiver, filter);
    }

    public void setStationActionsListener(StationActionsListener stationActionsListener) {
        this.stationActionsListener = stationActionsListener;
    }

    public void setFilterListener(FilterListener filterListener) {
        this.filterListener = filterListener;
    }

    public void enableItemRemoval(RecyclerView recyclerView) {
        if (!supportsStationRemoval) {
            supportsStationRemoval = true;

            RecyclerItemSwipeHelper<StationViewHolder> swipeHelper = new RecyclerItemSwipeHelper<>(getContext(), 0, ItemTouchHelper.LEFT + ItemTouchHelper.RIGHT, this);
            new ItemTouchHelper(swipeHelper).attachToRecyclerView(recyclerView);
        }
    }

    public void enableItemMoveAndRemoval(RecyclerView recyclerView) {
        if (!supportsStationRemoval) {
            supportsStationRemoval = true;

            RecyclerItemMoveAndSwipeHelper<StationViewHolder> swipeAndMoveHelper = new RecyclerItemMoveAndSwipeHelper<>(getContext(), ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this);
            new ItemTouchHelper(swipeAndMoveHelper).attachToRecyclerView(recyclerView);
        }
    }

    public void updateList(FragmentStarred refreshableList, List<DataRadioStation> stationsList) {
        this.refreshable = refreshableList;
        this.stationsList = stationsList;
        this.filteredStationsList = stationsList;

        notifyStationsChanged();
    }

    private void notifyStationsChanged() {
        expandedPosition = -1;
        playingStationPosition = -1;

        shouldLoadIcons = Utils.shouldLoadIcons(getContext());

        highlightCurrentStation();

        notifyDataSetChanged();
    }

    @Override
    public StationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(resourceId, parent, false);

        return new StationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final StationViewHolder holder, int position) {
        final DataRadioStation station = filteredStationsList.get(position);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        boolean useCircularIcons = Utils.useCircularIcons(getContext());

        if (!shouldLoadIcons) {
            holder.imageViewIcon.setVisibility(View.GONE);
        } else {
            if (station.hasIcon()) {
                setupIcon(useCircularIcons, holder.imageViewIcon, holder.transparentImageView);
                PlayerServiceUtil.getStationIcon(holder.imageViewIcon, station.IconUrl);
            } else {
                holder.imageViewIcon.setImageDrawable(stationImagePlaceholder);
            }

            if (prefs.getBoolean("compact_style", false))
                setupCompactStyle(holder);

            if (prefs.getBoolean("icon_click_toggles_favorite", true)) {

                final boolean isInFavorites = favouriteManager.has(station.StationUuid);
                holder.imageViewIcon.setContentDescription(getContext().getApplicationContext().getString(isInFavorites ? R.string.detail_unstar : R.string.detail_star));
                holder.imageViewIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (favouriteManager.has(station.StationUuid)) {
                            StationActions.removeFromFavourites(getContext(), view, station);
                        } else {
                            StationActions.markAsFavourite(getContext(), station);
                        }

                        int position = holder.getAdapterPosition();
                        notifyItemChanged(position);
                    }
                });
            }
        }

        final boolean isExpanded = position == expandedPosition;
        holder.textViewTags.setVisibility(isExpanded ? View.GONE : View.VISIBLE);

        holder.buttonMore.setImageResource(isExpanded ? R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_more_black_24dp);
        holder.buttonMore.setContentDescription(getContext().getApplicationContext().getString(isExpanded ? R.string.image_button_less : R.string.image_button_more));
        holder.buttonMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Notify prev item change
                if (expandedPosition != -1) {
                    notifyItemChanged(expandedPosition);
                }

                int position = holder.getAdapterPosition();
                expandedPosition = isExpanded ? -1 : position;

                // Notify current item changed
                if (expandedPosition != -1) {
                    notifyItemChanged(expandedPosition);
                }
            }
        });

        TypedValue tv = new TypedValue();
        if (playingStationPosition == position) {
            getContext().getTheme().resolveAttribute(R.attr.colorAccentMy, tv, true);
            holder.textViewTitle.setTextColor(tv.data);
            holder.textViewTitle.setTypeface(null, Typeface.BOLD);
        } else {
            getContext().getTheme().resolveAttribute(R.attr.boxBackgroundColor, tv, true);
            holder.textViewTitle.setTypeface(holder.textViewShortDescription.getTypeface());
            getContext().getTheme().resolveAttribute(R.attr.iconsInItemBackgroundColor, tv, true);
            holder.textViewTitle.setTextColor(tv.data);
        }

        holder.textViewTitle.setText(station.Name);
        holder.textViewShortDescription.setText(station.getShortDetails(getContext()));
        holder.textViewTags.setText(station.TagsAll.replace(",", ", "));

        boolean inFavourites = favouriteManager.has(station.StationUuid);
        holder.starredStatusIcon.setVisibility(inFavourites ? View.VISIBLE : View.GONE);
        holder.starredStatusIcon.setContentDescription(inFavourites ? getContext().getString(R.string.action_favorite) : "");

        if (prefs.getBoolean("click_trend_icon_visible", true)) {
            if (station.ClickTrend < 0) {
                holder.imageTrend.setImageResource(R.drawable.ic_trending_down_black_24dp);
                holder.imageTrend.setContentDescription(getContext().getString(R.string.icon_click_trend_decreasing));
            } else if (station.ClickTrend > 0) {
                holder.imageTrend.setImageResource(R.drawable.ic_trending_up_black_24dp);
                holder.imageTrend.setContentDescription(getContext().getString(R.string.icon_click_trend_increasing));
            } else {
                holder.imageTrend.setImageResource(R.drawable.ic_trending_flat_black_24dp);
                holder.imageTrend.setContentDescription(getContext().getString(R.string.icon_click_trend_stable));
            }
        } else {
            holder.imageTrend.setVisibility(View.GONE);
        }

        Drawable flag = CountryFlagsLoader.getInstance().getFlag(activity, station.CountryCode);

        if (flag != null) {
            float k = flag.getMinimumWidth() / (float) flag.getMinimumHeight();
            float viewHeight = holder.textViewShortDescription.getTextSize();
            flag.setBounds(0, 0, (int) (k * viewHeight), (int) viewHeight);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            holder.textViewShortDescription.setCompoundDrawablesRelative(flag, null, null, null);
        } else {
            holder.textViewShortDescription.setCompoundDrawables(flag, null, null, null);
        }

        if (isExpanded) {
            holder.viewDetails = holder.stubDetails == null ? holder.viewDetails : holder.stubDetails.inflate();
            holder.stubDetails = null;
            holder.viewTags = (TagsView) holder.viewDetails.findViewById(R.id.viewTags);
            holder.buttonVisitWebsite = holder.viewDetails.findViewById(R.id.buttonVisitWebsite);
            holder.buttonShare = holder.viewDetails.findViewById(R.id.buttonShare);
            holder.buttonBookmark = holder.viewDetails.findViewById(R.id.buttonBookmark);
            holder.buttonAddAlarm = holder.viewDetails.findViewById(R.id.buttonAddAlarm);
            holder.buttonCreateShortcut = holder.viewDetails.findViewById(R.id.buttonCreateShortcut);
            holder.buttonPlayInternalOrExternal = holder.viewDetails.findViewById(R.id.buttonPlayInRadioDroid);

            holder.buttonVisitWebsite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    StationActions.openStationHomeUrl(activity, station);
                }
            });

            holder.buttonShare.setOnClickListener(view -> StationActions.share(activity, station));

            if (favouriteManager.has(station.StationUuid)) {
                // favorite stations should only be removed in the favorites view
                holder.buttonBookmark.setVisibility(View.GONE);
            } else {
                holder.buttonBookmark.setOnClickListener(view -> {
                    StationActions.markAsFavourite(getContext(), station);
                    int position1 = holder.getAdapterPosition();
                    notifyItemChanged(position1);
                });
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                    && getContext().getApplicationContext().getSystemService(ShortcutManager.class).isRequestPinShortcutSupported()) {
                holder.buttonCreateShortcut.setVisibility(View.VISIBLE);
                holder.buttonCreateShortcut.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        station.prepareShortcut(getContext(), new CreatePinShortcutListener());
                    }
                });
            } else {
                holder.buttonCreateShortcut.setVisibility(View.INVISIBLE);
            }

            holder.buttonAddAlarm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    StationActions.setAsAlarm(activity, station);
                }
            });

            if (prefs.getBoolean("play_external", false)) {
                holder.buttonPlayInternalOrExternal.setOnClickListener(v -> {
                    StationActions.playInRadioDroid(getContext(), station);
                });
            } else {
                Context context = getContext();
                holder.buttonPlayInternalOrExternal.setContentDescription(getContext().getString(R.string.detail_play_in_external_player));
                holder.buttonPlayInternalOrExternal.setImageDrawable(new IconicsDrawable(getContext(), CommunityMaterial.Icon2.cmd_play_box_outline).size(IconicsSize.dp(24)));
                holder.buttonPlayInternalOrExternal.setOnClickListener(v -> Utils.playAndWarnIfMetered((RadioDroidApp) context.getApplicationContext(), station,
                        PlayerType.EXTERNAL, () -> PlayStationTask.playExternal(station, context).execute()));
            }
            String[] tags = station.TagsAll.split(",");
            holder.viewTags.setTags(Arrays.asList(tags));
            holder.viewTags.setTagSelectionCallback(tagSelectionCallback);
        }
        if (holder.viewDetails != null)
            holder.viewDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
    }

    @TargetApi(26)
    public class CreatePinShortcutListener implements DataRadioStation.ShortcutReadyListener {
        @Override
        public void onShortcutReadyListener(ShortcutInfo shortcut) {
            ShortcutManager shortcutManager = getContext().getApplicationContext().getSystemService(ShortcutManager.class);
            if (shortcutManager.isRequestPinShortcutSupported()) {
                shortcutManager.requestPinShortcut(shortcut, null);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (filteredStationsList != null) {
            return filteredStationsList.size();
        }
        return 0;
    }

    @Override
    public void onSwiped(StationViewHolder viewHolder, int direction) {
        stationActionsListener.onStationSwiped(filteredStationsList.get(viewHolder.getAdapterPosition()));
    }

    @Override
    public void onDragged(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, double dX, double dY) {

    }

    @Override
    public void onMoved(StationViewHolder viewHolder, int from, int to) {
        stationActionsListener.onStationMoved(from, to);
        notifyItemMoved(from, to);
    }

    @Override
    public void onMoveEnded(StationViewHolder viewHolder) {
        stationActionsListener.onStationMoveFinished();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(updateUIReceiver);
    }

    public StationsFilter getFilter() {
        if (filter == null) {
            filter = new StationsFilter(getContext(), filterType, new StationsFilter.DataProvider() {
                @Override
                public List<DataRadioStation> getOriginalStationList() {
                    return stationsList;
                }

                @Override
                public void notifyFilteredStationsChanged(StationsFilter.SearchStatus status, List<DataRadioStation> filteredStations) {
                    filteredStationsList = filteredStations;

                    notifyStationsChanged();

                    if (filterListener != null) {
                        filterListener.onSearchCompleted(status);
                    }
                }
            });
        }

        return filter;
    }

    Context getContext() {
        return activity;
    }

    void setupIcon(boolean useCircularIcons, ImageView imageView, ImageView transparentImageView) {
        if (useCircularIcons) {
            transparentImageView.setVisibility(View.VISIBLE);
            imageView.getLayoutParams().height = imageView.getLayoutParams().height = imageView.getLayoutParams().width;
            imageView.setBackgroundColor(getContext().getResources().getColor(android.R.color.black));
        }
    }

    private void setupCompactStyle(final StationViewHolder holder) {
        holder.layoutMain.setMinimumHeight((int) getContext().getResources().getDimension(R.dimen.compact_style_item_minimum_height));
        holder.frameLayout.getLayoutParams().width = (int) getContext().getResources().getDimension(R.dimen.compact_style_icon_container_width);
        holder.imageViewIcon.getLayoutParams().width = (int) getContext().getResources().getDimension(R.dimen.compact_style_icon_width);

        holder.textViewShortDescription.setVisibility(View.GONE);
        if (holder.transparentImageView.getVisibility() == View.VISIBLE) {
            holder.transparentImageView.getLayoutParams().height = (int) getContext().getResources().getDimension(R.dimen.compact_style_icon_height);
            holder.transparentImageView.getLayoutParams().width = (int) getContext().getResources().getDimension(R.dimen.compact_style_icon_width);
            holder.imageViewIcon.getLayoutParams().height = (int) getContext().getResources().getDimension(R.dimen.compact_style_icon_height);
        }
    }

    private void highlightCurrentStation() {
        if (!PlayerServiceUtil.isPlaying()) return;
        if (filteredStationsList == null) return;

        int oldPlayingStationPosition = playingStationPosition;

        String currentStationUuid = PlayerServiceUtil.getStationId();
        for (int i = 0; i < filteredStationsList.size(); i++) {
            if (filteredStationsList.get(i).StationUuid.equals(currentStationUuid)) {
                playingStationPosition = i;
                break;
            }
        }
        if (playingStationPosition != oldPlayingStationPosition) {
            if (oldPlayingStationPosition > -1)
                notifyItemChanged(oldPlayingStationPosition);
            if (playingStationPosition > -1)
                notifyItemChanged(playingStationPosition);
        }
    }

    private void notifyChangedByStationUuid(String uuid) {
        // TODO: Iterate through view holders instead of whole collection
        for (int i = 0; i < filteredStationsList.size(); i++) {
            if (filteredStationsList.get(i).StationUuid.equals(uuid)) {
                notifyItemChanged(i);
                break;
            }
        }
    }
}
