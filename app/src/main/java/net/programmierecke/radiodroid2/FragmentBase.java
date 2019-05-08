package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

import okhttp3.OkHttpClient;

public class FragmentBase extends Fragment {
    private static final String TAG = "FragmentBase";

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
                    getContext().sendBroadcast(new Intent(ActivityMain.ACTION_SHOW_LOADING));
                }

                RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
                final OkHttpClient httpClient = radioDroidApp.getHttpClient();

                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        HashMap<String, String> p = new HashMap<String, String>();
                        if (!show_broken) {
                            p.put("hidebroken", "true");
                        }
                        return Utils.downloadFeed(httpClient, getActivity(), url, forceUpdate, p);
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        DownloadFinished();
                        if(getContext() != null)
                            getContext().sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));
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