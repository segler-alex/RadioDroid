package net.programmierecke.radiodroid2;

import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import net.programmierecke.radiodroid2.adapters.ItemAdapterRecordings;
import net.programmierecke.radiodroid2.data.DataRecording;
import net.programmierecke.radiodroid2.interfaces.IFragmentRefreshable;
import net.programmierecke.radiodroid2.recording.RecordingsManager;

import java.io.File;
import java.util.List;

public class FragmentRecordings extends Fragment implements IFragmentRefreshable{
    private ItemAdapterRecordings itemAdapterRecordings;
    private ListView lv;
    final String TAG = "FragREC";

    void ClickOnItem(DataRecording theData) {
        String path = RecordingsManager.getRecordDir() + "/" + theData.Name;
        if(BuildConfig.DEBUG) { Log.d(TAG,"play :"+path); }

        Intent i = new Intent(path);
        i.setAction(android.content.Intent.ACTION_VIEW);


        File file = new File(path);
        Uri fileUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
        i.setDataAndType(fileUri, "audio/*");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ClipData clip = ClipData.newUri(getContext().getContentResolver(), "Record", fileUri);
            i.setClipData(clip);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            List<ResolveInfo> resInfoList =
                    getContext().getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);

            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                getContext().grantUriPermission(packageName, fileUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }

        startActivity(i);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_statistics,null);

        if (itemAdapterRecordings == null) {
            itemAdapterRecordings = new ItemAdapterRecordings(getActivity(), R.layout.list_item_recording);
        }

        lv = (ListView)view.findViewById(R.id.listViewStatistics);
        lv.setAdapter(itemAdapterRecordings);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object anObject = parent.getItemAtPosition(position);
                if (anObject instanceof DataRecording) {
                    ClickOnItem((DataRecording) anObject);
                }
            }
        });

        final int[] attrs = new int[] {android.R.attr.listDivider};
        final TypedArray a = getContext().obtainStyledAttributes(attrs);
        lv.setDivider(a.getDrawable(0));
        a.recycle();

        RefreshListGui();

        return view;
    }

    protected void RefreshListGui(){
        if(BuildConfig.DEBUG) { Log.d(TAG, "RefreshListGUI()"); }

        if (!Utils.verifyStoragePermissions(getActivity())){
            Log.e(TAG,"could not get permissions");
        }

        if (lv != null) {
            if(BuildConfig.DEBUG) { Log.d(TAG,"LV != null"); }
            ItemAdapterRecordings arrayAdapter = (ItemAdapterRecordings) lv.getAdapter();
            arrayAdapter.clear();
            DataRecording[] recordings = RecordingsManager.getRecordings();
            if(BuildConfig.DEBUG) { Log.d(TAG,"Station count:"+recordings.length); }
            for (DataRecording aRecording : recordings) {
                if (!aRecording.Name.equals(PlayerServiceUtil.getCurrentRecordFileName())) {
                    arrayAdapter.add(aRecording);
                }
            }

            lv.invalidate();
        }else{
            Log.e(TAG,"LV == null");
        }
    }

    @Override
    public void Refresh() {
        RefreshListGui();
    }
}
