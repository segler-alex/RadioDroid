package net.programmierecke.radiodroid2.station.live.metadata.lastfm.data

import com.google.gson.annotations.SerializedName

data class Toptags(
        @SerializedName("tag")
        val tag: List<Tag>? = null
)