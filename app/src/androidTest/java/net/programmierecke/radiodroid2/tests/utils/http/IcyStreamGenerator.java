package net.programmierecke.radiodroid2.tests.utils.http;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

import okhttp3.mockwebserver.MockResponse;
import okio.Buffer;

public class IcyStreamGenerator {
    public class MetadataInfo {

    }

    public static MockResponse generateIcyStream(@Nonnull Class clazz, @Nonnull String sourceFile) {
        InputStream is = clazz.getResourceAsStream(sourceFile);
        try {
            return new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "audio/mpeg")
                    .setBody(new Buffer().readFrom(is));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
