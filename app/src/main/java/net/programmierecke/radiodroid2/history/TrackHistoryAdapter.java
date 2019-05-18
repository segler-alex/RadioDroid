package net.programmierecke.radiodroid2.history;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.squareup.picasso.Picasso;

import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.service.PlayerServiceUtil;

import java.text.DateFormat;

public class TrackHistoryAdapter extends PagedListAdapter<TrackHistoryEntry, TrackHistoryAdapter.TrackHistoryItemViewHolder> {
    class TrackHistoryItemViewHolder extends RecyclerView.ViewHolder {
        final View rootview;

        final ImageView imageViewStationIcon;
        final TextView textViewTrackName;
        final TextView textViewTrackArtist;

        private TrackHistoryItemViewHolder(View itemView) {
            super(itemView);

            rootview = itemView;

            imageViewStationIcon = itemView.findViewById(R.id.imageViewStationIcon);
            textViewTrackName = itemView.findViewById(R.id.textViewTrackName);
            textViewTrackArtist = itemView.findViewById(R.id.textViewTrackArtist);
        }
    }

    private Context context;
    private Activity activity;
    private final LayoutInflater inflater;
    private boolean shouldLoadIcons;
    private Drawable stationImagePlaceholder;

    public TrackHistoryAdapter(Activity activity) {
        super(DIFF_CALLBACK);
        this.activity = activity;
        this.context = activity;
        inflater = LayoutInflater.from(context);

        stationImagePlaceholder = ContextCompat.getDrawable(context, R.drawable.ic_photo_24dp);
    }

    @NonNull
    @Override
    public TrackHistoryItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.list_item_history_track_item, parent, false);
        return new TrackHistoryItemViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(@NonNull final TrackHistoryItemViewHolder holder, int position) {
        final TrackHistoryEntry historyEntry = getItem(position);

        // null if a placeholder
        if (historyEntry == null) {
            return;
        }

        if (shouldLoadIcons) {
            if (!TextUtils.isEmpty(historyEntry.stationIconUrl)) {
                //setupIcon(useCircularIcons, holder.imageViewIcon, holder.transparentImageView);
                PlayerServiceUtil.getStationIcon(holder.imageViewStationIcon, historyEntry.stationIconUrl);
            } else {
                holder.imageViewStationIcon.setImageDrawable(stationImagePlaceholder);
            }
        } else {
            holder.imageViewStationIcon.setVisibility(View.GONE);
        }

        holder.textViewTrackName.setText(historyEntry.track);
        holder.textViewTrackArtist.setText(historyEntry.artist);

        holder.textViewTrackName.setSelected(true);
        holder.textViewTrackArtist.setSelected(true);

        holder.rootview.setOnClickListener(view -> showTrackInfoDialog(historyEntry));
    }

    @Override
    public void submitList(PagedList<TrackHistoryEntry> pagedList) {
        shouldLoadIcons = Utils.shouldLoadIcons(context);
        super.submitList(pagedList);
    }

    private void showTrackInfoDialog(final TrackHistoryEntry historyEntry) {
        BottomSheetDialog mBottomSheetDialog = new BottomSheetDialog(activity);

        View sheetView = activity.getLayoutInflater().inflate(R.layout.dialog_track_history_details, null);

        AppCompatImageView imageViewTrackArt = sheetView.findViewById(R.id.imageViewTrackArt);
        TextView textViewDate = sheetView.findViewById(R.id.textViewDate);
        TextView textViewDuration = sheetView.findViewById(R.id.textViewDuration);
        AppCompatButton btnLyrics = sheetView.findViewById(R.id.btnViewLyrics);
        AppCompatButton btnCopyInfo = sheetView.findViewById(R.id.btnCopyTrackInfo);

        Resources resource = context.getResources();
        final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, resource.getDisplayMetrics());
        Picasso.get()
                .load(historyEntry.artUrl)
                .placeholder(ContextCompat.getDrawable(context, R.drawable.ic_photo_24dp))
                .resize((int) px, 0)
                .into(imageViewTrackArt);

        // TODO: Icons for date and duration

        textViewDate.setText(DateFormat.getDateInstance().format(historyEntry.startTime));

        if (historyEntry.endTime.after(historyEntry.startTime)) {
            String elapsedTime = DateUtils.formatElapsedTime((historyEntry.endTime.getTime() - historyEntry.startTime.getTime()) / 1000);
            textViewDuration.setText(elapsedTime);
        } else {
            textViewDuration.setText("");
        }

        btnLyrics.setOnClickListener(v -> {
            if (isQuickLyricInstalled()) {
                context.startActivity(new Intent("com.geecko.QuickLyric.getLyrics")
                        .putExtra("TAGS", new String[]{historyEntry.artist, historyEntry.track}));
            } else {
                // TODO: send to QuickLyric's download page
            }
        });

        btnCopyInfo.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Track info", String.format("%s %s", historyEntry.artist, historyEntry.track));
                clipboard.setPrimaryClip(clip);

                CharSequence toastText = context.getResources().getText(R.string.notify_stream_url_copied);
                Toast.makeText(context.getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
            } else {
                //Log.e(TAG, "Clipboard is NULL!");
                // TODO: toast general error
            }
        });

        mBottomSheetDialog.setContentView(sheetView);
        mBottomSheetDialog.show();
    }

    public boolean isQuickLyricInstalled() {

        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("com.geecko.QuickLyric", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private static DiffUtil.ItemCallback<TrackHistoryEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TrackHistoryEntry>() {
                @Override
                public boolean areItemsTheSame(TrackHistoryEntry oldEntry, TrackHistoryEntry newEntry) {
                    return oldEntry.uid == newEntry.uid;
                }

                @Override
                public boolean areContentsTheSame(TrackHistoryEntry oldEntry,
                                                  @NonNull TrackHistoryEntry newEntry) {
                    return oldEntry.equals(newEntry);
                }
            };

}
