package net.programmierecke.radiodroid2

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FallbackStationsManagerTest {
    lateinit var instrumentationContext: Context
    lateinit var stations: FallbackStationsManager
    val client = OkHttpClient()

    @BeforeEach
    internal fun setup() {
        instrumentationContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        stations = FallbackStationsManager(instrumentationContext)
    }

    @Test
    fun fallbackStationsExists() {
        assertFalse(stations.isEmpty)
    }

    fun checkUrl(url: String): Boolean  {
        val request = Request.Builder()
                .url(url)
                .build();
        return client.newCall(request).execute().isSuccessful
    }

    @Test
    fun allFallbackStationStreamUrlsWork() {
        for (station in stations.list) {
            assert(checkUrl(station.StreamUrl))
        }
    }

    @Test
    fun allFallbackStationIconUrlsWork() {
        for (station in stations.list) {
            assert(checkUrl(station.IconUrl))
        }
    }

}
