package net.programmierecke.radiodroid2.station;

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
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.*;

import net.programmierecke.radiodroid2.*;
import net.programmierecke.radiodroid2.interfaces.IAdapterRefreshable;
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
        void onStationClick(DataRadioStation station);
        void onStationMoved(int from, int to);
        void onStationSwiped(DataRadioStation station);
        void onStationMoveFinished();
    }

    private final String TAG = "AdapterStations";

    List<DataRadioStation> stationsList;

    int resourceId;

    private StationActionsListener stationActionsListener;
    private boolean supportsStationRemoval = false;

    private boolean shouldLoadIcons;

    private IAdapterRefreshable refreshable;
    private FragmentActivity activity;

    private BroadcastReceiver updateUIReceiver;

    private int expandedPosition = -1;
    public int playingStationPosition = -1;

    Drawable stationImagePlaceholder;

    private FavouriteManager favouriteManager;

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
        ImageButton buttonStationLinks;
        ImageButton buttonBookmark;
        ImageView imageTrend;
        ImageButton buttonAddAlarm;
        TagsView viewTags;
        ImageButton buttonCreateShortcut;

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
                stationActionsListener.onStationClick(stationsList.get(getAdapterPosition()));
            }
        }

        @Override
        public View getForegroundView() {
            return viewForeground;
        }
    }

    public ItemAdapterStation(FragmentActivity fragmentActivity, int resourceId) {
        this.activity = fragmentActivity;
        this.resourceId = resourceId;

        stationImagePlaceholder = ContextCompat.getDrawable(fragmentActivity, R.drawable.ic_photo_24dp);

        RadioDroidApp radioDroidApp = (RadioDroidApp)fragmentActivity.getApplication();
        favouriteManager = radioDroidApp.getFavouriteManager();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayerService.PLAYER_SERVICE_META_UPDATE);

        this.updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                highlightCurrentStation();
            }
        };
        getContext().registerReceiver(this.updateUIReceiver,filter);

    }

    public void setStationActionsListener(StationActionsListener stationActionsListener) {
        this.stationActionsListener = stationActionsListener;
    }

    public void enableItemRemoval(RecyclerView recyclerView) {
        if (!supportsStationRemoval) {
            supportsStationRemoval = true;

            RecyclerItemSwipeHelper<StationViewHolder> swipeHelper = new RecyclerItemSwipeHelper<>(0, ItemTouchHelper.LEFT+ItemTouchHelper.RIGHT, this);
            new ItemTouchHelper(swipeHelper).attachToRecyclerView(recyclerView);
        }
    }

    public void enableItemMoveAndRemoval(RecyclerView recyclerView) {
        if (!supportsStationRemoval) {
            supportsStationRemoval = true;

            RecyclerItemMoveAndSwipeHelper<StationViewHolder> swipeAndMoveHelper = new RecyclerItemMoveAndSwipeHelper<>(ItemTouchHelper.UP|ItemTouchHelper.DOWN, ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT, this);
            new ItemTouchHelper(swipeAndMoveHelper).attachToRecyclerView(recyclerView);
        }
    }

    public void updateList(FragmentStarred refreshableList, List<DataRadioStation> stationsList) {
        this.refreshable = refreshableList;
        this.stationsList = stationsList;

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
        final DataRadioStation station = stationsList.get(position);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        boolean useCircularIcons = Utils.useCircularIcons(getContext());

        if (!shouldLoadIcons) {
            holder.imageViewIcon.setVisibility(View.GONE);
        } else {
            if (!station.IconUrl.isEmpty()) {
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
                            StationActions.removeFromFavourites(getContext(), station);
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
        if(playingStationPosition == position) {
            getContext().getTheme().resolveAttribute(R.attr.colorAccentMy, tv, true);
            holder.textViewTitle.setTextColor(tv.data);
            holder.textViewTitle.setTypeface(null, Typeface.BOLD);
        }
        else {
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

        Drawable flag = CountryFlagsLoader.getInstance().getFlag(activity, station.Country);

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
            holder.viewDetails = holder.stubDetails == null? holder.viewDetails : holder.stubDetails.inflate();
            holder.stubDetails = null;
            holder.viewTags = (TagsView) holder.viewDetails.findViewById(R.id.viewTags);
            holder.buttonStationLinks = holder.viewDetails.findViewById(R.id.buttonStationWebLink);
            holder.buttonBookmark = holder.viewDetails.findViewById(R.id.buttonBookmark);
            holder.buttonAddAlarm = holder.viewDetails.findViewById(R.id.buttonAddAlarm);
            holder.buttonCreateShortcut = holder.viewDetails.findViewById(R.id.buttonCreateShortcut);

//            holder.buttonShare.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    StationActions.share(getContext(), station);
//                }
//            });

            holder.buttonStationLinks.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    StationActions.showWebLinks(activity, station);
                }
            });

            if (favouriteManager.has(station.StationUuid)) {
                holder.buttonBookmark.setImageResource(R.drawable.ic_star_black_24dp);
                holder.buttonBookmark.setContentDescription(getContext().getApplicationContext().getString(R.string.detail_unstar));
                holder.buttonBookmark.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        StationActions.removeFromFavourites(getContext(), station);

                        int position = holder.getAdapterPosition();
                        notifyItemChanged(position);
                    }
                });
            } else {
                holder.buttonBookmark.setImageResource(R.drawable.ic_star_border_black_24dp);
                holder.buttonBookmark.setContentDescription(getContext().getApplicationContext().getString(R.string.detail_star));
                holder.buttonBookmark.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        StationActions.markAsFavourite(getContext(), station);

                        int position = holder.getAdapterPosition();
                        notifyItemChanged(position);
                    }
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

            String[] tags = station.TagsAll.split(",");
            holder.viewTags.setTags(Arrays.asList(tags));
            holder.viewTags.setTagSelectionCallback(tagSelectionCallback);
        }
        if(holder.viewDetails != null)
            holder.viewDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
    }

    @TargetApi(26)
    class CreatePinShortcutListener implements DataRadioStation.ShortcutReadyListener {
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
        return stationsList.size();
    }

    @Override
    public void onSwiped(StationViewHolder viewHolder, int direction) {
        stationActionsListener.onStationSwiped(stationsList.get(viewHolder.getAdapterPosition()));
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
        getContext().unregisterReceiver(updateUIReceiver);
    }

    Context getContext() {
        return activity;
    }

    void setupIcon(boolean useCircularIcons, ImageView imageView, ImageView transparentImageView) {
        if(useCircularIcons) {
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
        if(holder.transparentImageView.getVisibility() == View.VISIBLE) {
            holder.transparentImageView.getLayoutParams().height  = (int) getContext().getResources().getDimension(R.dimen.compact_style_icon_height);
            holder.transparentImageView.getLayoutParams().width  = (int) getContext().getResources().getDimension(R.dimen.compact_style_icon_width);
            holder.imageViewIcon.getLayoutParams().height = (int) getContext().getResources().getDimension(R.dimen.compact_style_icon_height);
        }
    }

    private void  highlightCurrentStation() {
        if(!PlayerServiceUtil.isPlaying()) return;

        int oldPlayingStationPosition = playingStationPosition;

        for (int i = 0; i < stationsList.size(); i++) {
            if (stationsList.get(i).StationUuid.equals(PlayerServiceUtil.getStationId())) {
                playingStationPosition = i;
                break;
            }
        }
        if(playingStationPosition != oldPlayingStationPosition) {
            if(oldPlayingStationPosition > -1)
                notifyItemChanged(oldPlayingStationPosition);
            if(playingStationPosition > -1)
                notifyItemChanged(playingStationPosition);
        }
    }
}
