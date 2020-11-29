package net.programmierecke.radiodroid2.station.live.metadata.lastfm.data

import com.google.gson.annotations.SerializedName

data class Streamable(
        @SerializedName("#text")
        val text: String? = null,

        @SerializedName("fulltrack")
        val fulltrack: String? = null
)