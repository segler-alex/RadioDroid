package net.programmierecke.radiodroid2.recording;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.R;

import java.io.File;
import java.util.List;

public class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.RecordingItemViewHolder> {
    final static String TAG = "RecordingsAdapter";

    class RecordingItemViewHolder extends RecyclerView.ViewHolder {
        final ViewGroup viewRoot;
        final TextView textViewTitle;
        final TextView textViewTime;

        private RecordingItemViewHolder(View itemView) {
            super(itemView);

            viewRoot = (ViewGroup) itemView;
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewTime = itemView.findViewById(R.id.textViewTime);
        }
    }

    private Context context;
    private List<DataRecording> recordings;

    public RecordingsAdapter(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public RecordingsAdapter.RecordingItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.list_item_recording, parent, false);
        return new RecordingItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingItemViewHolder holder, int position) {
        final DataRecording recording = recordings.get(position);

        holder.textViewTitle.setText(recording.Name);
        //holder.textViewTime.setText(recording.Time);

        holder.viewRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openRecording(recording);
            }
        });
    }

    public void setRecordings(List<DataRecording> recordings) {
        if (this.recordings != null && recordings.size() == this.recordings.size()) {
            boolean same = true;
            for (int i = 0; i < recordings.size(); i++) {
                if (!recordings.get(i).equals(this.recordings.get(i))) {
                    same = false;
                    break;
                }
            }

            if (same) {
                return;
            }
        }

        this.recordings = recordings;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return recordings != null ? recordings.size() : 0;
    }

    void openRecording(DataRecording theData) {
        String path = RecordingsManager.getRecordDir() + "/" + theData.Name;
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "play: " + path);
        }

        Intent i = new Intent(path);
        i.setAction(android.content.Intent.ACTION_VIEW);

        File file = new File(path);
        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        i.setDataAndType(fileUri, "audio/*");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ClipData clip = ClipData.newUri(context.getContentResolver(), "Record", fileUri);
            i.setClipData(clip);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            List<ResolveInfo> resInfoList =
                    context.getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);

            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, fileUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }

        context.startActivity(i);
    }
}
