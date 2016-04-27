package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

public class FragmentBase extends Fragment {
    private ProgressDialog itsProgressLoading;
    private ListView lv;
    private String url;
    private String urlResult;
    private DataRadioStation[] stations = new DataRadioStation[0];
    private Context mycontext;

    public FragmentBase() {
    }

    @Override
    public void onAttach(Context context) {
        Log.w("ATT", "attached");
        super.onAttach(context);
        mycontext = getActivity();
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
            if (mycontext != null) {
                itsProgressLoading = ProgressDialog.show(mycontext, "", "Loading...");
            }
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    return Utils.downloadFeed(url);
                }

                @Override
                protected void onPostExecute(String result) {
                    if (itsProgressLoading != null) {
                        itsProgressLoading.dismiss();
                        itsProgressLoading = null;
                    }
                    if (result != null) {
                        urlResult = result;
                        RefreshListGui();
                    }else{
                        Toast toast = Toast.makeText(getContext(), getResources().getText(R.string.error_list_update), Toast.LENGTH_SHORT);
                        toast.show();
                    }
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