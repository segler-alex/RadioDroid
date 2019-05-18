package net.programmierecke.radiodroid2;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.programmierecke.radiodroid2.interfaces.IApplicationSelected;

import java.util.ArrayList;
import java.util.List;

public class ApplicationSelectorDialog extends DialogFragment {
    ArrayList<ActivityInfo> listInfos = new ArrayList<ActivityInfo>();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.select_dialog_singlechoice);

        PackageManager pm = getContext().getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_VIEW);
        mainIntent.setDataAndType(Uri.parse("http://example.com/test.mp3"), "audio/*");
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for(ResolveInfo info : resolveInfos) {
            ApplicationInfo applicationInfo = info.activityInfo.applicationInfo;
            if(BuildConfig.DEBUG) { Log.d("UUU", ""+applicationInfo.packageName + " -- "+ info.activityInfo.name+ " -> "); }
            arrayAdapter.add(""+pm.getApplicationLabel(applicationInfo));
            listInfos.add(info.activityInfo);
        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.alert_select_external_alarm_app);
        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if(BuildConfig.DEBUG) { Log.d("AAA","choose : "+which); }
                if (callback != null){
                    ActivityInfo info = listInfos.get(which);
                    callback.onAppSelected(info.packageName,info.name);
                }
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    IApplicationSelected callback;

    public void setCallback(IApplicationSelected callback) {
        this.callback = callback;
    }
}
