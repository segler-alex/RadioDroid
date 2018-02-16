package net.programmierecke.radiodroid2;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import java.lang.reflect.Method;


public class CastHandler {

    private static CastContext mCastContext;
    private static SessionManager mSessionManager;
    private static final SessionManagerListener mSessionManagerListener =
            new SessionManagerListenerImpl();
    public static CastSession mCastSession;
    private static ActivityMain activityMain;

    public static boolean isReal() {
        return true;
    }

    public static void onCreate(ActivityMain activity) {
        activityMain = activity;
        mCastContext = CastContext.getSharedInstance(activityMain);
        mSessionManager = mCastContext.getSessionManager();
        mSessionManager.addSessionManagerListener(mSessionManagerListener);
    }

    public static void onPause() {
        mSessionManager.removeSessionManagerListener(mSessionManagerListener);
        mCastSession = null;
    }

    public static void onResume() {
        mCastSession = mSessionManager.getCurrentCastSession();
        mSessionManager.addSessionManagerListener(mSessionManagerListener);
    }

    public static MenuItem getRouteItem(Context context, Menu menu) {
        return CastButtonFactory.setUpMediaRouteButton(context,
                menu,
                R.id.media_route_menu_item);
    }

    public static boolean isCastSessionAvailable() {
        return mCastSession != null;
    }

    public static void PlayRemote(String title, String url, String iconurl){
        Log.i("CAST",title);
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);

        movieMetadata.putString(MediaMetadata.KEY_TITLE, title);
        //movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, "MySubTitle");
        movieMetadata.addImage(new WebImage(Uri.parse(iconurl)));
        //movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(1))));


        MediaInfo mediaInfo = new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType("audio/ogg")
                .setMetadata(movieMetadata)
                //.setStreamDuration(mSelectedMedia.getDuration() * 1000)
                .build();
        RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        remoteMediaClient.load(mediaInfo, true);
    }

    //TODO: Replace this
    private static void invalidateOptions() {
        try {
            Method invalidate = activityMain.getClass().getMethod("invalidateOptionsMenu");
            invalidate.invoke(activityMain);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static class SessionManagerListenerImpl implements SessionManagerListener {
        @Override
        public void onSessionStarting(Session session) {
            Log.i("CAST","onSessionStarting");
        }

        @Override
        public void onSessionStarted(Session session, String sessionId) {
            Log.i("CAST","onSessionStarted");
            invalidateOptions();
            mCastSession = mSessionManager.getCurrentCastSession();
        }

        @Override
        public void onSessionStartFailed(Session session, int i) {
            Log.i("CAST","onSessionStartFailed");
        }

        @Override
        public void onSessionEnding(Session session) {
            Log.i("CAST","onSessionEnding");
        }

        @Override
        public void onSessionResumed(Session session, boolean wasSuspended) {
            Log.i("CAST","onSessionStarting");
            invalidateOptions();
            mCastSession = mSessionManager.getCurrentCastSession();
        }

        @Override
        public void onSessionResumeFailed(Session session, int i) {
            Log.i("CAST","onSessionResumeFailed");
            mCastSession = null;
        }

        @Override
        public void onSessionSuspended(Session session, int i) {
            Log.i("CAST","onSessionSuspended");
            mCastSession = null;
        }

        @Override
        public void onSessionEnded(Session session, int error) {
            Log.i("CAST","onSessionEnded");
            mCastSession = null;
        }

        @Override
        public void onSessionResuming(Session session, String s) {
            Log.i("CAST","onSessionResuming");
        }
    }

}