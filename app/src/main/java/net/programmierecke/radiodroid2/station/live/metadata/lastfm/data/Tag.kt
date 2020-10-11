package net.programmierecke.radiodroid2.station.live.metadata.lastfm.data

import com.google.gson.annotations.SerializedName

data class Tag(
        @SerializedName("name")
        val name: String? = null,

        @SerializedName("url")
        val url: String? = null
)
