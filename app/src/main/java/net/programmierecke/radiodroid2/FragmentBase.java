package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.util.HashMap;

import okhttp3.OkHttpClient;

public class FragmentBase extends Fragment {
    private static final String TAG = "FragmentBase";

    private String relativeUrl;
    private String urlResult;

    private boolean isCreated = false;

    private AsyncTask task = null;

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

        if (relativeUrl == null) {
            Bundle bundle = this.getArguments();
            relativeUrl = bundle.getString("url");
        }

        DownloadUrl(false);
    }

    @Override
    public void onDestroy() {
        if (task != null) {
            task.cancel(true);
        }

        super.onDestroy();
    }

    protected String getUrlResult() {
        return urlResult;
    }

    protected boolean hasUrl() {
        return !TextUtils.isEmpty(relativeUrl);
    }

    /*
    public void SetDownloadUrl(String theUrl) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "new relativeUrl " + theUrl);
        }
        relativeUrl = theUrl;
        DownloadUrl(false);
    }
     */

    public void DownloadUrl(final boolean forceUpdate) {
        DownloadUrl(forceUpdate, true);
    }

    public void DownloadUrl(final boolean forceUpdate, final boolean displayProgress) {
        if (!isCreated) {
            return;
        }
        if (task != null){
            task.cancel(true);
            task = null;
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        final boolean show_broken = sharedPref.getBoolean("show_broken", false);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Download relativeUrl:" + relativeUrl);
        }

        if (TextUtils.isGraphic(relativeUrl)) {
            String cache = Utils.getCacheFile(getActivity(), relativeUrl);
            if (cache == null || forceUpdate) {
                if (getContext() != null && displayProgress) {
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(ActivityMain.ACTION_SHOW_LOADING));
                }

                RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
                final OkHttpClient httpClient = radioDroidApp.getHttpClient();

                task = new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        HashMap<String, String> p = new HashMap<String, String>();
                        p.put("hidebroken", ""+(!show_broken));
                        return Utils.downloadFeedRelative(httpClient, getActivity(), relativeUrl, forceUpdate, p);
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        DownloadFinished();
                        if(getContext() != null)
                            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Download relativeUrl finished:" + relativeUrl);
                        }
                        if (result != null) {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "Download relativeUrl OK:" + relativeUrl);
                            }
                            urlResult = result;
                            RefreshListGui();
                        } else {
                            try {
                                Toast toast = Toast.makeText(getContext(), getResources().getText(R.string.error_list_update), Toast.LENGTH_SHORT);
                                toast.show();
                            }
                            catch(Exception e){
                                Log.e("ERR",e.toString());
                            }
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