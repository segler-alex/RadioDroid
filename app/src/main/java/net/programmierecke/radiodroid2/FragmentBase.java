package net.programmierecke.radiodroid2;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

public class FragmentBase extends Fragment {
    private static final String TAG = "FragmentBase";

    private ProgressDialog itsProgressLoading;
    private String url;
    private String urlResult;

    private boolean isCreated = false;

    public FragmentBase() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isCreated = true;

        if (url == null) {
            Bundle bundle = this.getArguments();
            url = bundle.getString("url");
        }

        DownloadUrl(false);
    }

    protected String getUrlResult() {
        return urlResult;
    }

    public void SetDownloadUrl(String theUrl) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "new url " + theUrl);
        }
        url = theUrl;
        DownloadUrl(false);
    }

    public void DownloadUrl(final boolean forceUpdate) {
        DownloadUrl(forceUpdate, true);
    }

    public void DownloadUrl(final boolean forceUpdate, final boolean displayProgress) {
        if (!isCreated) {
            return;
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        final boolean show_broken = sharedPref.getBoolean("show_broken", false);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Download url:" + url);
        }

        if (TextUtils.isGraphic(url)) {
            String cache = Utils.getCacheFile(getActivity(), url);
            if (cache == null || forceUpdate) {
                if (getContext() != null && displayProgress) {
                    itsProgressLoading = ProgressDialog.show(getContext(), "", getActivity().getString(R.string.progress_loading));
                }
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        HashMap<String, String> p = new HashMap<String, String>();
                        if (!show_broken) {
                            p.put("hidebroken", "true");
                        }
                        return Utils.downloadFeed(getActivity(), url, forceUpdate, p);
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        DownloadFinished();
                        if (itsProgressLoading != null) {
                            itsProgressLoading.dismiss();
                            itsProgressLoading = null;
                        }
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Download url finished:" + url);
                        }
                        if (result != null) {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "Download url OK:" + url);
                            }
                            urlResult = result;
                            RefreshListGui();
                        } else {
                            Toast toast = Toast.makeText(getContext(), getResources().getText(R.string.error_list_update), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        super.onPostExecute(result);
                    }
                }.execute();
            } else {
                urlResult = cache;
                DownloadFinished();
                RefreshListGui();

            }
        } else {
            RefreshListGui();
        }
    }

    protected void RefreshListGui() {
    }

    protected void DownloadFinished() {
    }
}