package net.programmierecke.radiodroid2.station.live.metadata

class TrackMetadata {
    enum class AlbumArtSize {
        SMALL, MEDIUM, LARGE, EXTRA_LARGE
    }

    class AlbumArt(var size: AlbumArtSize, var url: String)

    var artist = ""
    var album = ""
    var track = ""
    var tags = emptyList<String>()
    var albumArts = emptyList<AlbumArt>()
}
