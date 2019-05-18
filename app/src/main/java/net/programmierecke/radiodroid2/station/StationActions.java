package net.programmierecke.radiodroid2.station;

import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioBrowserServerManager;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.alarm.TimePickerFragment;
import net.programmierecke.radiodroid2.views.ItemListDialog;

import java.lang.ref.WeakReference;

import okhttp3.OkHttpClient;

public class StationActions {
    private static final String TAG = "StationActions";

    public static void setAsAlarm(final @NonNull FragmentActivity activity, final @NonNull DataRadioStation station) {
        final RadioDroidApp radioDroidApp = (RadioDroidApp) activity.getApplicationContext();

        final TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.setCallback((timePicker, hourOfDay, minute) -> {
            Log.i(TAG, String.format("Alarm time picked %d:%d", hourOfDay, minute));
            radioDroidApp.getAlarmManager().add(station, hourOfDay, minute);
        });
        newFragment.show(activity.getSupportFragmentManager(), "timePicker");
    }

    public static void showWebLinks(final @NonNull FragmentActivity activity, final @NonNull DataRadioStation station) {
        ItemListDialog.create(activity, new int[]{
                R.string.action_station_visit_website, R.string.action_station_copy_stream_url, R.string.action_station_share
        }, resourceId -> {
            switch (resourceId) {
                case R.string.action_station_visit_website:
                {
                    openStationHomeUrl(activity, station);
                    break;
                }
                case R.string.action_station_copy_stream_url:
                {
                    retrieveAndCopyStreamUrlToClipboard(activity, station);
                    break;
                }
                case R.string.action_station_share:
                {
                    share(activity, station);
                    break;
                }
            }
        }).show();
    }

    private static void openStationHomeUrl(final @NonNull FragmentActivity activity, final @NonNull DataRadioStation station) {
        if (!TextUtils.isEmpty(station.HomePageUrl)) {
            Uri stationUrl = Uri.parse(station.HomePageUrl);
            if (stationUrl != null) {
                Intent newIntent = new Intent(Intent.ACTION_VIEW, stationUrl);
                activity.startActivity(newIntent);
            }
        }
    }

    private static void retrieveAndCopyStreamUrlToClipboard(final @NonNull Context context, final @NonNull DataRadioStation station) {
        context.sendBroadcast(new Intent(ActivityMain.ACTION_SHOW_LOADING));

        final WeakReference<Context> contextRef = new WeakReference<>(context);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                Context ctx = contextRef.get();
                if (ctx == null) {
                    return null;
                }

                final RadioDroidApp radioDroidApp = (RadioDroidApp) ctx.getApplicationContext();
                final OkHttpClient httpClient = radioDroidApp.getHttpClient();

                return Utils.getRealStationLink(httpClient, radioDroidApp, station.StationUuid);
            }

            @Override
            protected void onPostExecute(String result) {
                Context ctx = contextRef.get();
                if (ctx == null) {
                    super.onPostExecute(result);
                    return;
                }

                ctx.sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));

                if (result != null) {
                    ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("Stream Url", result);
                        clipboard.setPrimaryClip(clip);

                        CharSequence toastText = ctx.getResources().getText(R.string.notify_stream_url_copied);
                        Toast.makeText(ctx.getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Clipboard is NULL!");
                        // TODO: toast general error
                    }
                } else {
                    CharSequence toastText = ctx.getResources().getText(R.string.error_station_load);
                    Toast.makeText(ctx.getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    public static void markAsFavourite(final @NonNull Context context, final @NonNull DataRadioStation station) {
        final RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
        radioDroidApp.getFavouriteManager().add(station);

        Toast toast = Toast.makeText(context, context.getString(R.string.notify_starred), Toast.LENGTH_SHORT);
        toast.show();

        vote(context, station);
    }

    public static void removeFromFavourites(final @NonNull Context context, final @NonNull DataRadioStation station) {
        final RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
        radioDroidApp.getFavouriteManager().remove(station.StationUuid);

        Toast toast = Toast.makeText(context, context.getString(R.string.notify_unstarred), Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void share(final @NonNull Context context, final @NonNull DataRadioStation station) {
        context.sendBroadcast(new Intent(ActivityMain.ACTION_SHOW_LOADING));

        final WeakReference<Context> contextRef = new WeakReference<>(context);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                Context ctx = contextRef.get();
                if (ctx == null) {
                    return null;
                }

                final RadioDroidApp radioDroidApp = (RadioDroidApp) ctx.getApplicationContext();
                final OkHttpClient httpClient = radioDroidApp.getHttpClient();

                return Utils.getRealStationLink(httpClient, radioDroidApp, station.StationUuid);
            }

            @Override
            protected void onPostExecute(String result) {
                Context ctx = contextRef.get();
                if (ctx == null) {
                    return;
                }

                ctx.sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));

                if (result != null) {
                    Intent share = new Intent(Intent.ACTION_VIEW);
                    share.setDataAndType(Uri.parse(result), "audio/*");
                    String title = ctx.getResources().getString(R.string.share_action);
                    Intent chooser = Intent.createChooser(share, title);

                    if (share.resolveActivity(ctx.getPackageManager()) != null) {
                        ctx.startActivity(chooser);
                    }
                } else {
                    Toast toast = Toast.makeText(ctx.getApplicationContext(), ctx.getResources().getText(R.string.error_station_load), Toast.LENGTH_SHORT);
                    toast.show();
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    private static void vote(final @NonNull Context context, final @NonNull DataRadioStation station) {
        final WeakReference<Context> contextRef = new WeakReference<>(context);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                Context ctx = contextRef.get();
                if (ctx == null) {
                    return null;
                }

                final RadioDroidApp radioDroidApp = (RadioDroidApp) ctx.getApplicationContext();
                final OkHttpClient httpClient = radioDroidApp.getHttpClient();

                return Utils.downloadFeed(httpClient, ctx, RadioBrowserServerManager.getWebserviceEndpoint(ctx, "json/vote/" + station.StationUuid), true, null);
            }

            @Override
            protected void onPostExecute(String result) {
                Log.i(TAG, result);
                super.onPostExecute(result);
            }
        }.execute();
    }
}
