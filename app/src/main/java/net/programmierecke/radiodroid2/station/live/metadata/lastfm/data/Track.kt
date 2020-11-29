package net.programmierecke.radiodroid2.station.live.metadata.lastfm.data

import com.google.gson.annotations.SerializedName

data class Track(
        @SerializedName("name")
        val name: String? = null,

        @SerializedName("mbid")
        val mbid: String? = null,

        @SerializedName("url")
        val url: String? = null,

        @SerializedName("duration")
        val duration: String? = null,

        @SerializedName("streamable")
        val streamable: Streamable? = null,

        @SerializedName("listeners")
        val listeners: String? = null,

        @SerializedName("playcount")
        val playcount: String? = null,

        @SerializedName("artist")
        val artist: Artist? = null,

        @SerializedName("album")
        val album: Album? = null,

        @SerializedName("toptags")
        val toptags: Toptags? = null
)