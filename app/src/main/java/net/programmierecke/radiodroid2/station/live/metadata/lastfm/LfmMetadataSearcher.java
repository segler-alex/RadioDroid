package net.programmierecke.radiodroid2.station.live.metadata.lastfm;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.station.live.metadata.TrackMetadata;
import net.programmierecke.radiodroid2.station.live.metadata.TrackMetadataCallback;
import net.programmierecke.radiodroid2.station.live.metadata.lastfm.data.Image;
import net.programmierecke.radiodroid2.station.live.metadata.lastfm.data.LfmTrackMetadata;
import net.programmierecke.radiodroid2.station.live.metadata.lastfm.data.Track;
import net.programmierecke.radiodroid2.utils.RateLimiter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LfmMetadataSearcher {
    private static final String API_GET_TRACK_METADATA = "http://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=%s&artist=%s&track=%s&format=json";

    private OkHttpClient httpClient;
    private Gson gson = new Gson();

    private RateLimiter rateLimiter = new RateLimiter(4, 60 * 1000);

    public LfmMetadataSearcher(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Some station add to track suffix or prefix, like:
     * "Cool track [Bad station name]"
     * "Cool track (Bad station name)"
     * This makes such track unsearchable, try to remove these modifications and search track without them.
     * It has obvious drawback - some tracks have brackets in their name and such operation could
     * rarely lead to displaying wrong info if original name was unsearchable.
     * <p>
     * Also some stations have different suffix/prefix for different tracks so we cannot be smart and
     * devise from several tracks' names the scheme.
     *
     * @param track Track name as station sent to us
     * @return - null if nothing changed
     * - track name without additional station's suffix/prefix
     */
    private String tryNormalizeTrack(@NonNull final String track) {
        String normalizedTrack = track
                .replaceAll("\\(.*\\)", "")
                .replaceAll("\\[.*\\]", "")
                .replaceAll("\\*.*\\*", "")
                .trim();
        return normalizedTrack.equals(track) ? null : normalizedTrack;
    }

    public void fetchTrackMetadata(String artist, @NonNull final String track, @NonNull final TrackMetadataCallback trackMetadataCallback) {
        if (BuildConfig.LastFMAPIKey.isEmpty() || TextUtils.isEmpty(track)) {
            trackMetadataCallback.onFailure(TrackMetadataCallback.FailureType.UNRECOVERABLE);
            return;
        }

        final String trimmedArtist = artist.trim();
        final String trimmedTrack = track.trim();

        // We want to rate limit calls to Last.fm API to prevent exceeding unknown limits.
        if (rateLimiter.allowed()) {
            httpClient.newCall(buildRequest(trimmedArtist, trimmedTrack))
                    .enqueue(new MetadataCallback(trackMetadataCallback, trimmedArtist, trimmedTrack));
        } else {
            trackMetadataCallback.onFailure(TrackMetadataCallback.FailureType.RECOVERABLE);
        }
    }

    private Request buildRequest(String artist, String track) {
        HttpUrl url = HttpUrl.parse(String.format(API_GET_TRACK_METADATA, BuildConfig.LastFMAPIKey, artist, track));
        Request.Builder requestBuilder = new Request.Builder().url(url).get();
        return requestBuilder.build();
    }

    private class MetadataCallback implements Callback {
        private final TrackMetadataCallback trackMetadataCallback;
        private final String artist;
        private final String track;

        public MetadataCallback(TrackMetadataCallback trackMetadataCallback, String artist, String track) {
            this.trackMetadataCallback = trackMetadataCallback;
            this.track = track;
            this.artist = artist;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            trackMetadataCallback.onFailure(TrackMetadataCallback.FailureType.RECOVERABLE);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            try {
                LfmTrackMetadata lfmTrackMetadata = gson.fromJson(response.body().charStream(), LfmTrackMetadata.class);

                TrackMetadata trackMetadata = new TrackMetadata();

                Track trackData = lfmTrackMetadata.getTrack();

                if (trackData == null) {
                    String normalizedTrack = tryNormalizeTrack(track);
                    if (normalizedTrack != null && normalizedTrack.length() > 3) {
                        httpClient.newCall(buildRequest(artist, normalizedTrack)).enqueue(new MetadataCallback(trackMetadataCallback, artist, normalizedTrack));
                    } else {
                        trackMetadataCallback.onFailure(TrackMetadataCallback.FailureType.UNRECOVERABLE);
                    }

                    return;
                }

                if (trackData.getArtist() != null) {
                    trackMetadata.setArtist(trackData.getArtist().getName());
                }

                List<TrackMetadata.AlbumArt> albumArts = new ArrayList<>();
                trackMetadata.setAlbumArts(albumArts);

                if (trackData.getAlbum() != null) {
                    trackMetadata.setAlbum(trackData.getAlbum().getTitle());
                    List<Image> images = lfmTrackMetadata.getTrack().getAlbum().getImage();

                    for (Image img : images) {
                        TrackMetadata.AlbumArtSize artSize = TrackMetadata.AlbumArtSize.SMALL;
                        switch (img.getSize()) {
                            case "small":
                                artSize = TrackMetadata.AlbumArtSize.SMALL;
                                break;
                            case "medium":
                                artSize = TrackMetadata.AlbumArtSize.MEDIUM;
                                break;
                            case "large":
                                artSize = TrackMetadata.AlbumArtSize.LARGE;
                                break;
                            case "extralarge":
                                artSize = TrackMetadata.AlbumArtSize.EXTRA_LARGE;
                                break;
                        }

                        albumArts.add(new TrackMetadata.AlbumArt(artSize, img.getText()));
                    }

                    Collections.sort(albumArts, new Comparator<TrackMetadata.AlbumArt>() {
                        @Override
                        public int compare(TrackMetadata.AlbumArt o1, TrackMetadata.AlbumArt o2) {
                            return o2.size.compareTo(o1.size);
                        }
                    });
                }

                trackMetadata.setTrack(lfmTrackMetadata.getTrack().getName());

                trackMetadataCallback.onSuccess(trackMetadata);
            } catch (Exception ex) {
                trackMetadataCallback.onFailure(TrackMetadataCallback.FailureType.UNRECOVERABLE);
            }
        }
    }
}
