package net.programmierecke.radiodroid2.station.live.metadata.lastfm.data

import com.google.gson.annotations.SerializedName

data class Album(
        @SerializedName("artist")
        val artist: String? = null,

        @SerializedName("title")
        val title: String? = null,

        @SerializedName("url")
        val url: String? = null,

        @SerializedName("image")
        val image: List<Image>? = null
)