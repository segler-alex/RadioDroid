package net.programmierecke.radiodroid2.station.live.metadata.lastfm.data

import com.google.gson.annotations.SerializedName

data class Image(
        @SerializedName("#text")
        val text: String? = null,

        @SerializedName("size")
        val size: String? = null
)