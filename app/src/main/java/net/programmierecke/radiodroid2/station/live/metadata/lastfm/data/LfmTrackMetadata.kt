package net.programmierecke.radiodroid2.station.live.metadata.lastfm.data

import com.google.gson.annotations.SerializedName

data class LfmTrackMetadata(
        @SerializedName("track")
        val track: Track? = null
)