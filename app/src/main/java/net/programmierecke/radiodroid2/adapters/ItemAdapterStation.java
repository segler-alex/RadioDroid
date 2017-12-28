package net.programmierecke.radiodroid2.adapters;

import java.util.Arrays;
import java.util.List;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.CountryFlagsLoader;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.data.DataRadioStation;
import net.programmierecke.radiodroid2.FavouriteManager;
import net.programmierecke.radiodroid2.FragmentStarred;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioAlarmManager;
import net.programmierecke.radiodroid2.TimePickerFragment;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.interfaces.IAdapterRefreshable;
import net.programmierecke.radiodroid2.utils.RecyclerItemSwipeHelper;
import net.programmierecke.radiodroid2.utils.SwipeableViewHolder;
import net.programmierecke.radiodroid2.views.TagsView;

public class ItemAdapterStation
        extends RecyclerView.Adapter<ItemAdapterStation.StationViewHolder>
        implements TimePickerDialog.OnTimeSetListener, RecyclerItemSwipeHelper.SwipeCallback<ItemAdapterStation.StationViewHolder> {

    public interface StationActionsListener {
        void onStationClick(DataRadioStation station);

        void onStationSwiped(DataRadioStation station);
    }

    private final String TAG = "AdapterStations";

    private List<DataRadioStation> stationsList;

    private int resourceId;

    private StationActionsListener stationActionsListener;
    private boolean supportsStationRemoval = false;

    private boolean shouldLoadIcons;

    private ProgressDialog progressLoading;
    private IAdapterRefreshable refreshable;
    private FragmentActivity activity;

    private DataRadioStation selectedStation;
    private int expandedPosition = -1;

    private Drawable stationImagePlaceholder;

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
        ImageButton buttonStationWebLink;
        ImageButton buttonShare;
        ImageButton buttonBookmark;
        ImageButton buttonSetTimer;
        TagsView viewTags;

        StationViewHolder(View itemView) {
            super(itemView);

            viewForeground = itemView.findViewById(R.id.station_foreground);
            layoutMain = (LinearLayout) itemView.findViewById(R.id.layoutMain);
            frameLayout = (FrameLayout) itemView.findViewById(R.id.frameLayout);

            imageViewIcon = (ImageView) itemView.findViewById(R.id.imageViewIcon);
            transparentImageView = (ImageView) itemView.findViewById(R.id.transparentCircle);
            starredStatusIcon = (ImageView) itemView.findViewById(R.id.starredStatusIcon);
            textViewTitle = (TextView) itemView.findViewById(R.id.textViewTitle);
            textViewShortDescription = (TextView) itemView.findViewById(R.id.textViewShortDescription);
            textViewTags = (TextView) itemView.findViewById(R.id.textViewTags);
            buttonMore = (ImageButton) itemView.findViewById(R.id.buttonMore);

            viewDetails = itemView.findViewById(R.id.layoutDetails);
            viewTags = (TagsView) viewDetails.findViewById(R.id.viewTags);
            buttonStationWebLink = (ImageButton) viewDetails.findViewById(R.id.buttonStationWebLink);
            buttonShare = (ImageButton) viewDetails.findViewById(R.id.buttonShare);
            buttonBookmark = (ImageButton) viewDetails.findViewById(R.id.buttonBookmark);
            buttonSetTimer = (ImageButton) viewDetails.findViewById(R.id.buttonSetTimer);

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

        stationImagePlaceholder = ContextCompat.getDrawable(fragmentActivity, R.drawable.ic_photo_black_24dp);

        RadioDroidApp radioDroidApp = (RadioDroidApp)fragmentActivity.getApplication();
        favouriteManager = radioDroidApp.getFavouriteManager();
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

    public void updateList(FragmentStarred refreshableList, List<DataRadioStation> stationsList) {
        this.refreshable = refreshableList;
        this.stationsList = stationsList;

        expandedPosition = -1;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        shouldLoadIcons = sharedPreferences.getBoolean("load_icons", false);

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
        boolean useCircularIcons = prefs.getBoolean("circular_icons", false);

        if (!shouldLoadIcons) {
            holder.imageViewIcon.setVisibility(View.GONE);
        } else {
            if (!station.IconUrl.isEmpty()) {
                Resources r = activity.getResources();
                final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, r.getDisplayMetrics());

                setupIcon(useCircularIcons, holder.imageViewIcon, holder.transparentImageView);
                Callback imageLoadCallback = new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError() {
                    }
                };

                Picasso.with(getContext())
                        .load(station.IconUrl)
                        .resize((int) px, 0)
                        .placeholder(stationImagePlaceholder)
                        .into(holder.imageViewIcon, imageLoadCallback);
            } else {
                holder.imageViewIcon.setImageDrawable(stationImagePlaceholder);
            }

            if(prefs.getBoolean("compact_style", false))
                setupCompactStyle(holder);

            if (prefs.getBoolean("icon_click_toggles_favorite", true)) {

                holder.imageViewIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Context context = getContext().getApplicationContext();

                        if (favouriteManager.has(station.ID)) {
                            unStar(station);
                            Toast toast = Toast.makeText(context, context.getString(R.string.notify_unstarred), Toast.LENGTH_SHORT);
                            toast.show();
                        } else {
                            star(station);
                            Toast toast = Toast.makeText(context, context.getString(R.string.notify_starred), Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        int position = holder.getAdapterPosition();
                        notifyItemChanged(position);
                    }
                });
            }
        }

        final boolean isExpanded = position == expandedPosition;
        holder.viewDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.textViewTags.setVisibility(isExpanded ? View.GONE : View.VISIBLE);

        holder.buttonMore.setImageResource(isExpanded ? R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_more_black_24dp);
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

        holder.textViewTitle.setText(station.Name);
        holder.textViewShortDescription.setText(station.getShortDetails(getContext()));
        holder.textViewTags.setText(station.TagsAll.replace(",", ", "));

        Context context = getContext().getApplicationContext();

        holder.starredStatusIcon.setVisibility(favouriteManager.has(station.ID) ? View.VISIBLE : View.GONE);

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
            holder.buttonShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    share(station);
                }
            });

            holder.buttonStationWebLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLinks(station);
                }
            });

            if (favouriteManager.has(station.ID)) {
                holder.buttonBookmark.setImageResource(R.drawable.ic_star_black_24dp);
                holder.buttonBookmark.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        unStar(station);
                        Context context = getContext().getApplicationContext();
                        Toast toast = Toast.makeText(context, context.getString(R.string.notify_unstarred), Toast.LENGTH_SHORT);
                        toast.show();
                        int position = holder.getAdapterPosition();
                        notifyItemChanged(position);
                    }
                });
            } else {
                holder.buttonBookmark.setImageResource(R.drawable.ic_star_border_black_24dp);
                holder.buttonBookmark.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        star(station);
                        Context context = getContext().getApplicationContext();
                        Toast toast = Toast.makeText(context, context.getString(R.string.notify_starred), Toast.LENGTH_SHORT);
                        toast.show();
                        int position = holder.getAdapterPosition();
                        notifyItemChanged(position);
                    }
                });
            }

            holder.buttonSetTimer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setAsAlarm(station);
                }
            });

            String[] tags = station.TagsAll.split(",");
            holder.viewTags.setTags(Arrays.asList(tags));
            holder.viewTags.setTagSelectionCallback(tagSelectionCallback);
        }
    }

    @Override
    public int getItemCount() {
        return stationsList.size();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Log.w(TAG, "onTimeSet() " + hourOfDay);
        RadioAlarmManager ram = new RadioAlarmManager(getContext().getApplicationContext(), null);
        ram.add(selectedStation, hourOfDay, minute);
    }

    @Override
    public void onSwiped(StationViewHolder viewHolder, int direction) {
        stationActionsListener.onStationSwiped(stationsList.get(viewHolder.getAdapterPosition()));
    }

    private void showLinks(final DataRadioStation station) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setItems(R.array.actions_station_link, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0:
                        openStationHomeUrl(station);
                        break;
                    case 1:
                        retrieveAndCopyStreamUrlToClipboard(station);
                        break;
                }
            }
        }).setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.create().show();
    }

    private void openStationHomeUrl(final DataRadioStation station) {
        if (station.HomePageUrl != null && !station.HomePageUrl.isEmpty()) {
            Uri stationUrl = Uri.parse(station.HomePageUrl);
            if (stationUrl != null) {
                Intent newIntent = new Intent(Intent.ACTION_VIEW, stationUrl);
                activity.startActivity(newIntent);
            }
        }
    }

    private void retrieveAndCopyStreamUrlToClipboard(final DataRadioStation station) {
        final ProgressDialog loadingDialog = ProgressDialog.show(getContext(), "", getContext().getResources().getText(R.string.progress_loading));
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return Utils.getRealStationLink(getContext().getApplicationContext(), station.ID);
            }

            @Override
            protected void onPostExecute(String result) {
                loadingDialog.dismiss();

                if (result != null) {
                    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Stream Url", result);
                    clipboard.setPrimaryClip(clip);

                    CharSequence toastText = getContext().getResources().getText(R.string.notify_stream_url_copied);
                    Toast.makeText(getContext().getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
                } else {
                    CharSequence toastText = getContext().getResources().getText(R.string.error_station_load);
                    Toast.makeText(getContext().getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    private void star(DataRadioStation station) {
        if (station != null) {
            favouriteManager.add(station);
            vote(station.ID);
        } else {
            Log.e(TAG, "empty station info");
        }
    }

    private void vote(final String stationID) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return Utils.downloadFeed(activity, "https://www.radio-browser.info/webservice/json/vote/" + stationID, true, null);
            }

            @Override
            protected void onPostExecute(String result) {
                Log.i(TAG, result);
                super.onPostExecute(result);
            }
        }.execute();
    }

    private void unStar(DataRadioStation station) {
        if (station != null) {
            favouriteManager.remove(station.ID);
            if (refreshable != null) {
                refreshable.RefreshListGui();
            }
        } else {
            Log.e(TAG, "empty station info");
        }
    }

    private Context getContext() {
        return activity;
    }

    private void setAsAlarm(DataRadioStation station) {
        Log.w(TAG, "setAsAlarm() 1");
        if (station != null) {
            selectedStation = station;
            Log.w(TAG, "setAsAlarm() 2");
            TimePickerFragment newFragment = new TimePickerFragment();
            newFragment.setCallback(this);
            newFragment.show(activity.getSupportFragmentManager(), "timePicker");
        }
    }

    private void share(final DataRadioStation station) {
        progressLoading = ProgressDialog.show(getContext(), "", getContext().getResources().getString(R.string.progress_loading));
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return Utils.getRealStationLink(getContext().getApplicationContext(), station.ID);
            }

            @Override
            protected void onPostExecute(String result) {
                progressLoading.dismiss();

                if (result != null) {
                    Intent share = new Intent(Intent.ACTION_VIEW);
                    share.setDataAndType(Uri.parse(result), "audio/*");
                    String title = getContext().getResources().getString(R.string.share_action);
                    Intent chooser = Intent.createChooser(share, title);

                    if (share.resolveActivity(getContext().getPackageManager()) != null) {
                        getContext().startActivity(chooser);
                    }
                } else {
                    Toast toast = Toast.makeText(getContext().getApplicationContext(), getContext().getResources().getText(R.string.error_station_load), Toast.LENGTH_SHORT);
                    toast.show();
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    private void setupIcon(boolean useCircularIcons, ImageView imageView, ImageView transparentImageView) {
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
}
