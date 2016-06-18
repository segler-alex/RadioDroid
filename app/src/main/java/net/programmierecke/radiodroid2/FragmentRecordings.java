package net.programmierecke.radiodroid2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import net.programmierecke.radiodroid2.adapters.ItemAdapterRecordings;
import net.programmierecke.radiodroid2.data.DataRecording;
import net.programmierecke.radiodroid2.interfaces.IFragmentRefreshable;

import java.io.File;

public class FragmentRecordings extends Fragment implements IFragmentRefreshable{
    private ItemAdapterRecordings itemAdapterRecordings;
    private ListViewCompat lv;

    void ClickOnItem(DataRecording theData) {
        String path = Recordings.getRecordDir() + "/" + theData.Name;
        Log.w("REC","play :"+path);
        Intent i = new Intent(path);
        i.setAction(android.content.Intent.ACTION_VIEW);
        File file = new File(path);
        i.setDataAndType(Uri.fromFile(file), "audio/*");
        startActivity(i);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_statistics,null);

        if (itemAdapterRecordings == null) {
            itemAdapterRecordings = new ItemAdapterRecordings(getActivity(), R.layout.list_item_recording);
        }

        lv = (ListViewCompat)view.findViewById(R.id.listViewStatistics);
        lv.setAdapter(itemAdapterRecordings);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object anObject = parent.getItemAtPosition(position);
                if (anObject instanceof DataRecording) {
                    ClickOnItem((DataRecording) anObject);
                }
            }
        });

        RefreshListGui();

        return view;
    }

    protected void RefreshListGui(){
        Log.d("ABC", "RefreshListGUI()");

        if (!Utils.verifyStoragePermissions(getActivity())){
            Log.e("REC","could not get permissions");
        }

        if (lv != null) {
            Log.d("ABC","LV != null");
            ItemAdapterRecordings arrayAdapter = (ItemAdapterRecordings) lv.getAdapter();
            arrayAdapter.clear();
            DataRecording[] recordings = Recordings.getRecordings();
            Log.d("ABC","Station count:"+recordings.length);
            for (DataRecording aRecording : recordings) {
                arrayAdapter.add(aRecording);
            }

            lv.invalidate();
        }else{
            Log.e("NULL","LV == null");
        }
    }

    @Override
    public void Refresh() {
        RefreshListGui();
    }
}
