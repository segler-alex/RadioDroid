package net.programmierecke.radiodroid2

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class UtilsTest {
    @Test
    fun testUrlIndicatesHlsStream() {
        assert(Utils.urlIndicatesHlsStream("http://www.example.org/playlist.m3u8"))
        assert(Utils.urlIndicatesHlsStream("http://www.example.org/playlist.m3u8 # cool"))
        assert(Utils.urlIndicatesHlsStream("http://www.example.org/playlist.m3u8\n"))
        assert(Utils.urlIndicatesHlsStream("https://stream.revma.ihrhls.com/zc5260/hls.m3u8?streamid=5260"))
        assert(Utils.urlIndicatesHlsStream("http://www.example.org/playlist.m3u8?bitrate=256&z=43"))
        assert(Utils.urlIndicatesHlsStream("http://www.example.org/playlist.m3u8#0100"))
        assert(Utils.urlIndicatesHlsStream("http://www.example.org/playlist.m3u8#START"))
        assert(Utils.urlIndicatesHlsStream("http://www.example.org/m3u8playlist.m3u8"))

        assertFalse(Utils.urlIndicatesHlsStream("http://www.example.org/no.m3u8.m3u"))
        assertFalse(Utils.urlIndicatesHlsStream("http://www.example.org/playlist.m3u85"))
        assertFalse(Utils.urlIndicatesHlsStream("http://www.example.org/playlist.m3united"))
    }
}
