package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.ListViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.programmierecke.radiodroid2.adapters.ItemAdapterStatistics;
import net.programmierecke.radiodroid2.data.DataStatistics;
import net.programmierecke.radiodroid2.interfaces.IFragmentRefreshable;

public class FragmentServerInfo extends Fragment implements IFragmentRefreshable {
    private ItemAdapterStatistics itemAdapterStatistics;
    private ProgressDialog itsProgressLoading;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_statistics,null);

        if (itemAdapterStatistics == null) {
            itemAdapterStatistics = new ItemAdapterStatistics(getActivity(), R.layout.list_item_statistic);
        }

        ListViewCompat lv = (ListViewCompat)view.findViewById(R.id.listViewStatistics);
        lv.setAdapter(itemAdapterStatistics);

        Download(false);

        return view;
    }

    void Download(final boolean forceUpdate){
        itsProgressLoading = ProgressDialog.show(getActivity(), "", getActivity().getString(R.string.progress_loading));
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return Utils.downloadFeed(getActivity(), "http://www.radio-browser.info/webservice/json/stats", forceUpdate, null);
            }

            @Override
            protected void onPostExecute(String result) {
                itsProgressLoading.dismiss();
                if (result != null) {
                    itemAdapterStatistics.clear();
                    DataStatistics[] items = DataStatistics.DecodeJson(result);
                    for(DataStatistics item: items) {
                        itemAdapterStatistics.add(item);
                    }
                }else{
                    Toast toast = Toast.makeText(getContext(), getResources().getText(R.string.error_list_update), Toast.LENGTH_SHORT);
                    toast.show();
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    @Override
    public void Refresh() {
        Download(true);
    }
}
