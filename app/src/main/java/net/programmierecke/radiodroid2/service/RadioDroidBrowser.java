package net.programmierecke.radiodroid2.service;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.support.v4.media.MediaBrowserCompat;
import androidx.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.station.DataRadioStation;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;
import jp.wasabeef.picasso.transformations.CropSquareTransformation;
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

import static net.programmierecke.radiodroid2.Utils.resourceToUri;


public class RadioDroidBrowser {
    private static final String MEDIA_ID_ROOT = "__ROOT__";
    private static final String MEDIA_ID_MUSICS_FAVORITE = "__FAVORITE__";
    private static final String MEDIA_ID_MUSICS_HISTORY = "__HISTORY__";
    private static final String MEDIA_ID_MUSICS_TOP = "__TOP__";
    private static final String MEDIA_ID_MUSICS_TOP_TAGS = "__TOP_TAGS__";

    private static final char LEAF_SEPARATOR = '|';

    private static final int IMAGE_LOAD_TIMEOUT_MS = 2000;

    private RadioDroidApp radioDroidApp;

    private Map<String, DataRadioStation> stationIdToStation = new HashMap<>();

    private static class RetrieveStationsIconAndSendResult extends AsyncTask<Void, Void, Void> {
        private MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result;
        private List<DataRadioStation> stations;
        private WeakReference<Context> contextRef;

        private Map<String, Bitmap> stationIdToIcon = new HashMap<>();
        private CountDownLatch countDownLatch;
        private  Resources resources;
        // Picasso stores weak references to targets
        List<Target> imageLoadTargets = new ArrayList<>();

        RetrieveStationsIconAndSendResult(MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result, List<DataRadioStation> stations, Context context) {
            this.result = result;
            this.stations = stations;
            this.contextRef = new WeakReference<>(context);
            resources = context.getApplicationContext().getResources();
        }

        @Override
        protected void onPreExecute() {
            countDownLatch = new CountDownLatch(stations.size());

            for (final DataRadioStation station : stations) {
                Context context = contextRef.get();
                if (context == null) {
                    break;
                }

                Target imageLoadTarget = new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        stationIdToIcon.put(station.StationUuid, bitmap);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        onBitmapLoaded(((BitmapDrawable) errorDrawable).getBitmap(), null);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                };
                imageLoadTargets.add(imageLoadTarget);

                Picasso.get().load((!station.hasIcon() ? resourceToUri(resources, R.drawable.ic_launcher).toString() : station.IconUrl))
                        .transform(new CropSquareTransformation())
                        .error(R.drawable.ic_launcher)
                        .transform(Utils.useCircularIcons(context) ? new CropCircleTransformation() : new CropSquareTransformation())
                        .transform(new RoundedCornersTransformation(12, 2, RoundedCornersTransformation.CornerType.ALL))
                        .resize(128, 128)
                        .into(imageLoadTarget);
            }

            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                countDownLatch.await(IMAGE_LOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextRef.get();
            if (context != null) {
                for (Target target : imageLoadTargets) {
                    Picasso.get().cancelRequest(target);
                }
            }

            List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

            for (DataRadioStation station : stations) {
                Bitmap stationIcon = stationIdToIcon.get(station.StationUuid);
                if (stationIcon == null)
                    stationIcon = BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.ic_launcher);
                Bundle extras = new Bundle();
                extras.putParcelable(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, stationIcon);
                extras.putParcelable(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, stationIcon);
                mediaItems.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                        .setMediaId(MEDIA_ID_MUSICS_HISTORY + LEAF_SEPARATOR + station.StationUuid)
                        .setTitle(station.Name)
                        .setIconBitmap(stationIcon)
                        .setExtras(extras)
                        .build(),
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
            }

            result.sendResult(mediaItems);

            super.onPostExecute(aVoid);
        }
    }

    public RadioDroidBrowser(RadioDroidApp radioDroidApp) {
        this.radioDroidApp = radioDroidApp;
    }

    @Nullable
    public MediaBrowserServiceCompat.BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_ROOT, null);
    }

    public void onLoadChildren(@NonNull String parentId, @NonNull MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result) {
        Resources resources = radioDroidApp.getResources();
        if (MEDIA_ID_ROOT.equals(parentId)) {
            result.sendResult(createBrowsableMediaItemsForRoot(resources));
            return;
        }

        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        List<DataRadioStation> stations = null;

        switch (parentId) {
            case MEDIA_ID_MUSICS_FAVORITE: {
                stations = radioDroidApp.getFavouriteManager().getList();
                break;
            }
            case MEDIA_ID_MUSICS_HISTORY: {
                stations = radioDroidApp.getHistoryManager().getList();
                break;
            }
            case MEDIA_ID_MUSICS_TOP: {

                break;
            }
        }

        if (stations != null && !stations.isEmpty()) {
            stationIdToStation.clear();
            for (DataRadioStation station : stations) {
                stationIdToStation.put(station.StationUuid, station);
            }
            result.detach();
            new RetrieveStationsIconAndSendResult(result, stations, radioDroidApp).execute();
        } else {
            result.sendResult(mediaItems);
        }

    }

    @Nullable
    public DataRadioStation getStationById(@NonNull String stationId) {
        return stationIdToStation.get(stationId);
    }

    private List<MediaBrowserCompat.MediaItem> createBrowsableMediaItemsForRoot(Resources resources) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        mediaItems.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId(MEDIA_ID_MUSICS_FAVORITE)
                .setTitle(resources.getString(R.string.nav_item_starred))
                .setIconUri(resourceToUri(resources, R.drawable.ic_star_black_24dp))
                .build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

        mediaItems.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId(MEDIA_ID_MUSICS_HISTORY)
                .setTitle(resources.getString(R.string.nav_item_history))
                .setIconUri(resourceToUri(resources, R.drawable.ic_restore_black_24dp))
                .build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

        mediaItems.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId(MEDIA_ID_MUSICS_TOP)
                .setTitle(resources.getString(R.string.action_top_click))
                .setIconUri(resourceToUri(resources, R.drawable.ic_restore_black_24dp))
                .build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        return mediaItems;
    }

    public static String stationIdFromMediaId(final String mediaId) {
        if (mediaId == null) {
            return "";
        }

        final int separatorIdx = mediaId.indexOf(LEAF_SEPARATOR);

        if (separatorIdx <= 0) {
            return mediaId;
        }

        return mediaId.substring(separatorIdx + 1);
    }
}
