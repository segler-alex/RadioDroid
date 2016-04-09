package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;

public class FragmentBase extends Fragment {
    private ProgressDialog itsProgressLoading;
    private ItemAdapterStation itsArrayAdapter = null;
    private ListView lv;
    private String url;
    private String urlResult;
    private DataRadioStation[] stations = new DataRadioStation[0];

    public FragmentBase() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getArguments();
        url = bundle.getString("url");

        InitArrayAdapter();

        DownloadUrl();
    }

    protected String getUrlResult(){
        return urlResult;
    }

    protected void InitArrayAdapter(){
    }

    public void SetDownloadUrl(String theUrl) {
        Log.w("","new url "+theUrl);
        url = theUrl;
        DownloadUrl();
    }

    public void DownloadUrl() {
        if (TextUtils.isGraphic(url)) {
            itsProgressLoading = ProgressDialog.show(getActivity(), "", "Loading...");
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    return Utils.downloadFeed(url);
                }

                @Override
                protected void onPostExecute(String result) {
                    urlResult = result;
                    RefreshListGui();
                    itsProgressLoading.dismiss();
                    super.onPostExecute(result);
                }
            }.execute();
        }else{
            RefreshListGui();
        }
    }

    protected void RefreshListGui(){
    }
}