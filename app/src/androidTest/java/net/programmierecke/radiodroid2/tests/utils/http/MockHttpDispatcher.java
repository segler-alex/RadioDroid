package net.programmierecke.radiodroid2.tests.utils.http;

import android.util.Log;

import java.io.InputStream;
import java.util.Scanner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

public class MockHttpDispatcher extends Dispatcher {

    public interface CustomRequestDispatcher {
        @Nullable
        MockResponse dispatch(String path);
    }

    public interface PathFilter {
        boolean compatible(@Nonnull String path);
    }

    public static final PathFilter isStationsRequest = path -> path.startsWith("/json/stations");
    public static final PathFilter isStationsSearchRequest = path -> path.startsWith("/json/stations/by");
    public static final PathFilter isTagsRequest = path -> path.startsWith("/json/tags");
    public static final PathFilter isCountryCodesRequest = path -> path.startsWith("/json/countrycodes");
    public static final PathFilter isLanguagesRequest = path -> path.startsWith("/json/languages");
    public static final PathFilter isStationUrlRequest = path -> path.startsWith("/json/url");
    public static final PathFilter isAudioRequest = path -> path.endsWith("audio.mp3");

    private CustomRequestDispatcher customRequestDispatcher;

    private String fromFile(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        Scanner sc = new Scanner(is);
        StringBuilder sb = new StringBuilder();
        while (sc.hasNext()) {
            sb.append(sc.nextLine());
        }

        return sb.toString();
    }

    public void setCustomRequestDispatcher(@Nullable CustomRequestDispatcher customRequestDispatcher) {
        this.customRequestDispatcher = customRequestDispatcher;
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        String originalUrlStr = request.getRequestUrl().queryParameter("url");
        HttpUrl originalUrl = HttpUrl.parse(originalUrlStr);
        String path = originalUrl.encodedPath();

        if (customRequestDispatcher != null) {
            MockResponse customResponse = customRequestDispatcher.dispatch(path);
            if (customResponse != null) {
                return customResponse;
            }
        }

        if (isStationsSearchRequest.compatible(path)) {
            return new MockResponse().setResponseCode(200).setBody(fromFile("/stations_search_list.json"));
        } else if (isStationsRequest.compatible(path)) {
            return new MockResponse().setResponseCode(200).setBody(fromFile("/stations_list.json"));
        } else if (isTagsRequest.compatible(path)) {
            return new MockResponse().setResponseCode(200).setBody(fromFile("/tags_list.json"));
        } else if (isCountryCodesRequest.compatible(path)) {
            return new MockResponse().setResponseCode(200).setBody(fromFile("/countrycodes_list.json"));
        } else if (isLanguagesRequest.compatible(path)) {
            return new MockResponse().setResponseCode(200).setBody(fromFile("/languages_list.json"));
        } else if (isStationUrlRequest.compatible(path)) {
            return new MockResponse().setResponseCode(200).setBody(fromFile("/station_url.json"));
        } else if (isAudioRequest.compatible(path)) {
            return IcyStreamGenerator.generateIcyStream(getClass(), "/test.mp3");
        }

        Log.w("MockHttpDispatcher", String.format("No handling of \"%s\"", request.toString()));

        return new MockResponse().setResponseCode(404);
    }
}
