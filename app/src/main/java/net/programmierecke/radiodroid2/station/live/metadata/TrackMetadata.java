package net.programmierecke.radiodroid2.station.live.metadata;

import java.util.ArrayList;
import java.util.List;

public class TrackMetadata {
    public enum AlbumArtSize {
        SMALL,
        MEDIUM,
        LARGE,
        EXTRA_LARGE
    }

    public static class AlbumArt {
        public AlbumArtSize size;
        public String url;

        public AlbumArt(AlbumArtSize size, String url) {
            this.size = size;
            this.url = url;
        }
    }

    private String artist;
    private String album;
    private String track;
    private ArrayList<String> tags;
    private List<AlbumArt> albumArts;

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public List<AlbumArt> getAlbumArts() {
        return albumArts;
    }

    public void setAlbumArts(List<AlbumArt> albumArts) {
        this.albumArts = albumArts;
    }
}
