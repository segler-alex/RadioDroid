package net.programmierecke.radiodroid2.history;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.squareup.picasso.Picasso;

import net.programmierecke.radiodroid2.R;

import java.text.DateFormat;
import java.util.Objects;

public class TrackHistoryInfoDialog extends BottomSheetDialogFragment {

    public static final String FRAGMENT_TAG = "tracks_history_info_dialog_fragment";

    private final TrackHistoryEntry historyEntry;

    public TrackHistoryInfoDialog(TrackHistoryEntry historyEntry) {
        this.historyEntry = historyEntry;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setRetainInstance(true);

        View view = inflater.inflate(R.layout.dialog_track_history_details, container, false);

        AppCompatImageView imageViewTrackArt = view.findViewById(R.id.imageViewTrackArt);
        TextView textViewDate = view.findViewById(R.id.textViewDate);
        TextView textViewDuration = view.findViewById(R.id.textViewDuration);
        AppCompatButton btnLyrics = view.findViewById(R.id.btnViewLyrics);
        AppCompatButton btnCopyInfo = view.findViewById(R.id.btnCopyTrackInfo);

        Resources resource = Objects.requireNonNull(getContext()).getResources();
        final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, resource.getDisplayMetrics());
        Picasso.get()
                .load(historyEntry.artUrl)
                .placeholder(ContextCompat.getDrawable(getContext(), R.drawable.ic_photo_24dp))
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
                getContext().startActivity(new Intent("com.geecko.QuickLyric.getLyrics")
                        .putExtra("TAGS", new String[]{historyEntry.artist, historyEntry.track}));
            } else {
                new AlertDialog.Builder(getContext())
                        .setMessage(this.getString(R.string.alert_install_lyrics_app))
                        .setCancelable(true)
                        .setPositiveButton(this.getString(R.string.yes), (dialog, id) -> {
                            try {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.geecko.QuickLyric"));
                                getContext().startActivity(browserIntent);
                            } catch (ActivityNotFoundException ex) {
                                try {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.geecko.QuickLyric"));
                                    getContext().startActivity(browserIntent);
                                } catch (ActivityNotFoundException ex2) {
                                    Toast toast = Toast.makeText(getContext(), R.string.notify_open_link_failure, Toast.LENGTH_LONG);
                                    toast.show();
                                }
                            }
                        })
                        .setNegativeButton(this.getString(R.string.no), null)
                        .show();
            }
        });

        btnCopyInfo.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Track info", String.format("%s %s", historyEntry.artist, historyEntry.track));
                clipboard.setPrimaryClip(clip);

                CharSequence toastText = getContext().getResources().getText(R.string.notify_track_info_copied);
                Toast.makeText(getContext().getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
            } else {
                //Log.e(TAG, "Clipboard is NULL!");
                // TODO: toast general error
            }
        });

        return view;
    }

    private boolean isQuickLyricInstalled() {
        PackageManager pm = Objects.requireNonNull(getContext()).getPackageManager();
        try {
            pm.getPackageInfo("com.geecko.QuickLyric", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }
}
