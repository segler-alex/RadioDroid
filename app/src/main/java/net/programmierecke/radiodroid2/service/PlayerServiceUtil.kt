package net.programmierecke.radiodroid2.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.util.TypedValue
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import net.programmierecke.radiodroid2.BuildConfig
import net.programmierecke.radiodroid2.IPlayerService
import net.programmierecke.radiodroid2.R
import net.programmierecke.radiodroid2.players.PlayState
import net.programmierecke.radiodroid2.players.selector.PlayerType
import net.programmierecke.radiodroid2.station.DataRadioStation
import net.programmierecke.radiodroid2.station.live.ShoutcastInfo
import net.programmierecke.radiodroid2.station.live.StreamLiveInfo

object PlayerServiceUtil {
    private var mainContext: Context? = null
    private var bound = false
    private var serviceConnection: ServiceConnection? = null
    @JvmStatic
    fun bind(context: Context) {
        if (bound) return
        val intent = Intent(context, PlayerService::class.java)
        intent.putExtra(PlayerService.PLAYER_SERVICE_NO_NOTIFICATION_EXTRA, true)
        mainContext = context
        serviceConnection = getServiceConnection()
        context.startService(intent)
        context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
        bound = true
    }

    @JvmStatic
    fun unBind(context: Context?) {
        try {
            context?.unbindService(serviceConnection!!)
        } catch (e: Exception) {
        }
        serviceConnection = null
        bound = false
    }

    @JvmStatic
    fun shutdownService() {
        if (mainContext != null) {
            try {
                if (BuildConfig.DEBUG) {
                    Log.d("PlayerServiceUtil", "PlayerServiceUtil: shutdownService")
                }
                val intent = Intent(mainContext, PlayerService::class.java)
                unBind(mainContext)
                mainContext?.stopService(intent)
                itsPlayerService = null
                serviceConnection = null
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.d("PlayerServiceUtil", "PlayerServiceUtil: shutdownService E001:" + e.message)
                }
            }
        }
    }

    private var itsPlayerService: IPlayerService? = null
    private fun getServiceConnection(): ServiceConnection {
        return object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, binder: IBinder) {
                if (BuildConfig.DEBUG) {
                    Log.d("PLAYER", "Service came online")
                }
                itsPlayerService = IPlayerService.Stub.asInterface(binder)
                val local = Intent()
                local.action = PlayerService.PLAYER_SERVICE_BOUND
                LocalBroadcastManager.getInstance(mainContext!!).sendBroadcast(local)
            }

            override fun onServiceDisconnected(className: ComponentName) {
                if (BuildConfig.DEBUG) {
                    Log.d("PLAYER", "Service offline")
                }
                unBind(mainContext)
            }
        }
    }

    @JvmStatic
    val isServiceBound: Boolean
        get() = itsPlayerService != null

    @JvmStatic
    val isPlaying: Boolean
        get() {
            return try {
                itsPlayerService?.isPlaying ?: false
            } catch (e: RemoteException) {
                false
            }
        }

    @JvmStatic
    val playerState: PlayState
        get() {
            return try {
                itsPlayerService?.playerState ?: PlayState.Idle
            } catch (e: RemoteException) {
                PlayState.Idle
            }
        }

    @JvmStatic
    fun stop() {
        try {
            itsPlayerService?.Stop()
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    fun play(station: DataRadioStation?) {
        try {
            itsPlayerService?.SetStation(station)
            itsPlayerService?.Play(false)
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    fun setStation(station: DataRadioStation?) {
        try {
            itsPlayerService?.SetStation(station)
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    fun skipToNext() {
        try {
            itsPlayerService?.SkipToNext()
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    fun skipToPrevious() {
        try {
            itsPlayerService?.SkipToPrevious()
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    fun pause(pauseReason: PauseReason?) {
        try {
            itsPlayerService?.Pause(pauseReason)
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    fun resume() {
        try {
            itsPlayerService?.Resume()
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    fun clearTimer() {
        try {
            itsPlayerService?.clearTimer()
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    fun addTimer(secondsAdd: Int) {
        try {
            itsPlayerService?.addTimer(secondsAdd)
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    val timerSeconds: Long
        get() = try {
            itsPlayerService?.timerSeconds ?: 0
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            0
        }

    @JvmStatic
    val metadataLive: StreamLiveInfo
        get() = try {
            itsPlayerService?.metadataLive
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            null
        } ?: StreamLiveInfo(null)

    @JvmStatic
    val stationId: String?
        get() = try {
            itsPlayerService?.currentStationID
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            null
        }

    @JvmStatic
    val currentStation: DataRadioStation?
        get() = try {
            itsPlayerService?.currentStation
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            null
        }

    @JvmStatic
    fun getStationIcon(holder: ImageView?, fromUrl: String?) {
        if (fromUrl == null || fromUrl.isBlank()) return

        val r = mainContext!!.resources
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70f, r.displayMetrics)
        val imageLoadCallback: Callback = object : Callback {
            override fun onSuccess() {}
            override fun onError(e: Exception) {
                Picasso.get()
                        .load(fromUrl)
                        .placeholder(ContextCompat.getDrawable(mainContext!!, R.drawable.ic_photo_24dp)!!)
                        .resize(px.toInt(), 0)
                        .networkPolicy(NetworkPolicy.NO_CACHE)
                        .into(holder)
            }
        }
        Picasso.get()
                .load(fromUrl)
                .placeholder(ContextCompat.getDrawable(mainContext!!, R.drawable.ic_photo_24dp)!!)
                .resize(px.toInt(), 0)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(holder, imageLoadCallback)
    }

    @JvmStatic
    val shoutcastInfo: ShoutcastInfo?
        get() = try {
            itsPlayerService?.shoutcastInfo
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            null
        }

    @JvmStatic
    fun startRecording() {
        try {
            itsPlayerService?.startRecording()
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    fun stopRecording() {
        try {
            itsPlayerService?.stopRecording()
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    val isRecording: Boolean
        get() = try {
            itsPlayerService?.isRecording ?: false
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            false
        }


    @JvmStatic
    val currentRecordFileName: String?
        get() = try {
            itsPlayerService?.currentRecordFileName
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            null
        }

    @JvmStatic
    val isHls: Boolean
        get() = try {
            itsPlayerService?.isHls ?: false
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            false
        }

    @JvmStatic
    val transferredBytes: Long
        get() = try {
            itsPlayerService?.transferredBytes ?: 0
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            0
        }

    @JvmStatic
    val bufferedSeconds: Long
        get() = try {
            itsPlayerService?.bufferedSeconds ?: 0
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            0
        }

    @JvmStatic
    val lastPlayStartTime: Long
        get() = try {
            itsPlayerService?.lastPlayStartTime ?: 0
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            0
        }

    @JvmStatic
    val pauseReason: PauseReason
        get() = try {
            itsPlayerService?.pauseReason ?: PauseReason.NONE
        } catch (e: RemoteException) {
            Log.e("", "" + e)
            PauseReason.NONE
        }

    @JvmStatic
    fun enableMPD(hostname: String?, port: Int) {
        try {
            itsPlayerService?.enableMPD(hostname, port)
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    fun disableMPD() {
        try {
            itsPlayerService?.disableMPD()
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }

    @JvmStatic
    fun warnAboutMeteredConnection(playerType: PlayerType?) {
        try {
            itsPlayerService?.warnAboutMeteredConnection(playerType)
        } catch (e: RemoteException) {
            Log.e("", "" + e)
        }
    }
}
