package net.programmierecke.radiodroid2

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.images.WebImage
import net.programmierecke.radiodroid2.cast.CastAwareActivity
import net.programmierecke.radiodroid2.service.PauseReason
import net.programmierecke.radiodroid2.service.PlayerServiceUtil

private sealed class CastState {
    abstract fun setActivity(activity: CastAwareActivity?)

    abstract fun onPause()
    abstract fun onResume()

    abstract fun onSessionStarted(session: Session)
    abstract fun onSessionResumed(session: Session)
    abstract fun onSessionLost()

    abstract fun play(title: String, url: String, iconurl: String?)
}

private object CastUnavailable : CastState() {
    private const val TAG = "CastHandler.CastUnavailable"
    override fun setActivity(activity: CastAwareActivity?) {
    }

    override fun onPause() {
    }

    override fun onResume() {
    }

    override fun onSessionStarted(session: Session) {
        Log.e(TAG, "onSessionStarted: Illegal operation")
    }

    override fun onSessionResumed(session: Session) {
        Log.e(TAG, "onSessionResumed: Illegal operation")
    }

    override fun onSessionLost() {
        Log.e(TAG, "onSessionLost: Illegal operation")
    }

    override fun play(title: String, url: String, iconurl: String?) {
        Log.e(TAG, "play: Illegal operation")
    }
}

private class CastAvailable(val castContext: CastContext,
                            val sessionManager: SessionManager,
                            val sessionManagerListener: SessionManagerListener<Session>,
                            var castSession: CastSession?) : CastState() {
    private var activity: CastAwareActivity? = null

    override fun setActivity(activity: CastAwareActivity?) {
        this.activity = activity
    }

    override fun onPause() {
        sessionManager.removeSessionManagerListener(sessionManagerListener)
        castSession = null
    }

    override fun onResume() {
        sessionManager.addSessionManagerListener(sessionManagerListener)
        castSession = sessionManager.currentCastSession
    }

    override fun onSessionStarted(session: Session) {
        castSession = sessionManager.currentCastSession

        invalidateOptions()

        if (PlayerServiceUtil.isPlaying()) {
            PlayerServiceUtil.pause(PauseReason.USER)

            val station = PlayerServiceUtil.getCurrentStation()!!
            play(station.Name, station.playableUrl, station.IconUrl)
        }
    }

    override fun onSessionResumed(session: Session) {
        castSession = sessionManager.currentCastSession

        invalidateOptions()
    }

    override fun onSessionLost() {
        castSession = null

        invalidateOptions()
    }

    override fun play(title: String, url: String, iconurl: String?) {
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title)
        movieMetadata.addImage(WebImage(Uri.parse(iconurl)))

        val mediaInfo = MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType("audio/ogg")
                .setMetadata(movieMetadata)
                .build()

        castSession?.remoteMediaClient?.load(mediaInfo, true)
    }

    private fun invalidateOptions() {
        activity?.invalidateOptionsMenuForCast()
    }
}

public class CastHandler {

    companion object {
        private const val TAG = "CastHandler"
    }

    private var castState: CastState = CastUnavailable

    val isReal: Boolean
        get() = true

    val isCastAvailable: Boolean
        get() = castState is CastAvailable

    val isCastSessionAvailable: Boolean
        get() = (castState as? CastAvailable)?.castSession != null

    fun onCreate(context: Context) {
        if (castState is CastAvailable) {
            return
        }

        try {
            val googleAPI = GoogleApiAvailability.getInstance()
            val result = googleAPI.isGooglePlayServicesAvailable(context)

            if (result == ConnectionResult.SUCCESS) {
                val castContext = CastContext.getSharedInstance(context)
                val castState = CastAvailable(
                        castContext = castContext,
                        sessionManager = castContext.sessionManager,
                        sessionManagerListener = SessionManagerListenerImpl(),
                        castSession = null
                )

                castState.sessionManager.addSessionManagerListener(castState.sessionManagerListener)

                this.castState = castState
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    fun setActivity(activity: CastAwareActivity?) {
        castState.setActivity(activity)
    }

    private inline fun <T, reified S : T> sealedIf(sealedInstance: T, block: (S) -> Unit): Any? {
        return (sealedInstance as? S)?.also(block)
    }

    fun onPause() {
        castState.onPause()
    }

    fun onResume() {
        castState.onResume()
    }

    fun getRouteItem(context: Context, menu: Menu): MenuItem {
        return CastButtonFactory.setUpMediaRouteButton(context,
                menu,
                R.id.media_route_menu_item)
    }

    fun playRemote(title: String, url: String, iconurl: String?) {
        Log.i(TAG, title)

        castState.play(title, url, iconurl)
    }

    inner class SessionManagerListenerImpl : SessionManagerListener<Session> {
        override fun onSessionStarting(session: Session) {
            Log.i(TAG, "onSessionStarting")
        }

        override fun onSessionStarted(session: Session, sessionId: String) {
            Log.i(TAG, "onSessionStarted")

            castState.onSessionStarted(session)
        }

        override fun onSessionStartFailed(session: Session, i: Int) {
            Log.i(TAG, "onSessionStartFailed")
        }

        override fun onSessionEnding(session: Session) {
            Log.i(TAG, "onSessionEnding")
        }

        override fun onSessionResumed(session: Session, wasSuspended: Boolean) {
            Log.i(TAG, "onSessionStarting")

            castState.onSessionResumed(session)
        }

        override fun onSessionResumeFailed(session: Session, i: Int) {
            Log.i(TAG, "onSessionResumeFailed")

            castState.onSessionLost()
        }

        override fun onSessionSuspended(session: Session, i: Int) {
            Log.i(TAG, "onSessionSuspended")

            castState.onSessionLost()
        }

        override fun onSessionEnded(session: Session, error: Int) {
            Log.i(TAG, "onSessionEnded")

            castState.onSessionLost()
        }

        override fun onSessionResuming(session: Session, s: String) {
            Log.i(TAG, "onSessionResuming")
        }
    }
}