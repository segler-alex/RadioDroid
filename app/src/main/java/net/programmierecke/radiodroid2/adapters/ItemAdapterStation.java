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
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.CountryFlagsLoader;
import net.programmierecke.radiodroid2.data.DataRadioStation;
import net.programmierecke.radiodroid2.FavouriteManager;
import net.programmierecke.radiodroid2.FragmentStarred;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioAlarmManager;
import net.programmierecke.radiodroid2.TimePickerFragment;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.interfaces.IAdapterRefreshable;
import net.programmierecke.radiodroid2.views.TagsView;

public class ItemAdapterStation extends RecyclerView.Adapter<ItemAdapterStation.StationViewHolder> implements TimePickerDialog.OnTimeSetListener {
    private final String TAG = "AdapterStations";


    public interface StationClickListener {
        void onStationClick(DataRadioStation station);
    }

    private List<DataRadioStation> stationsList;

    private int resourceId;

    private ProgressDialog progressLoading;
    private IAdapterRefreshable refreshable;

    private FragmentActivity activity;
    private DataRadioStation selectedStation;

    private Drawable stationImagePlaceholder;

    private StationClickListener stationClickListener;

    private int expandedPosition = -1;

    private boolean shouldLoadIcons;

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

    class StationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView imageViewIcon;
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
            imageViewIcon = (ImageView) itemView.findViewById(R.id.imageViewIcon);
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
            if (stationClickListener != null) {
                stationClickListener.onStationClick(stationsList.get(getAdapterPosition()));
            }
        }
    }

    public ItemAdapterStation(FragmentActivity fragmentActivity, int resourceId) {
        this.activity = fragmentActivity;
        this.resourceId = resourceId;

        stationImagePlaceholder = ContextCompat.getDrawable(fragmentActivity, R.drawable.ic_photo_black_24dp);
    }

    public void setStationClickListener(StationClickListener stationClickListener) {
        this.stationClickListener = stationClickListener;
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

        if (!shouldLoadIcons) {
            holder.imageViewIcon.setVisibility(View.GONE);
        } else {
            if (!station.IconUrl.isEmpty()) {
                Resources r = activity.getResources();
                final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, r.getDisplayMetrics());

                Callback cachedImageLoadCallback = new Callback() {
                    @Override
                    public void onSuccess() {
                        //Offline cache hit
                    }

                    @Override
                    public void onError() {
                        Picasso.with(getContext())
                                .load(station.IconUrl)
                                .networkPolicy(NetworkPolicy.NO_CACHE)
                                .resize((int) px, 0)
                                .into(holder.imageViewIcon);
                    }
                };

                Picasso.with(getContext())
                        .load(station.IconUrl)
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .resize((int) px, 0)
                        .placeholder(stationImagePlaceholder)
                        .into(holder.imageViewIcon, cachedImageLoadCallback);
            } else {
                holder.imageViewIcon.setImageDrawable(stationImagePlaceholder);
            }

            if (PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext()).getBoolean("icon_click_toggles_favorite", true)) {

                holder.imageViewIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Context context = getContext().getApplicationContext();
                        FavouriteManager fm = new FavouriteManager(context);

                        if (fm.has(station.ID)) {
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
            FavouriteManager fm = new FavouriteManager(getContext().getApplicationContext());

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

            if (fm.has(station.ID)) {
                holder.buttonBookmark.setImageResource(R.drawable.ic_star_black_24dp);
                holder.buttonBookmark.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        unStar(station);
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
            FavouriteManager fm = new FavouriteManager(getContext().getApplicationContext());
            fm.add(station);
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
            FavouriteManager fm = new FavouriteManager(getContext().getApplicationContext());
            fm.remove(station.ID);
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
}
