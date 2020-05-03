package net.programmierecke.radiodroid2.players.exoplayer;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import net.programmierecke.radiodroid2.station.live.ShoutcastInfo;
import net.programmierecke.radiodroid2.station.live.StreamLiveInfo;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import okhttp3.OkHttpClient;


class IcyDataSourceTest {
    private static IcyDataSource icyDataSource;
    private static StringBuffer transferredBytesWithoutMetadata;

    @BeforeAll
    public static void setup() {
        icyDataSource = new IcyDataSource(new OkHttpClient(), new TestTransferListener(), new TestDataSourceListener());
        icyDataSource.shoutcastInfo = new ShoutcastInfo();
        icyDataSource.shoutcastInfo.metadataOffset = 16000;
    }

    @BeforeEach
    void init() {
        transferredBytesWithoutMetadata = new StringBuffer();
    }

    @Test
    void sendToDataSourceListenersWithoutMetadata_canHandleMultipleMetadataFrames() {
        final byte[] buffer = "OFFSETaudio1audio2\u0001METADATAMETADATAaudio3audio4audio5\u0002METADATAMETADATAMETADATAMETADATAaudio6".getBytes();
        final int offset = 6;
        icyDataSource.remainingUntilMetadata = "audioN".length() * 2;
        icyDataSource.shoutcastInfo.metadataOffset = "audioN".length() * 3;
        icyDataSource.metadataBytesToSkip = 0;
        icyDataSource.sendToDataSourceListenersWithoutMetadata(buffer, offset, buffer.length - offset);
        assertEquals("audio1audio2audio3audio4audio5audio6", transferredBytesWithoutMetadata.toString());
        assertEquals(icyDataSource.shoutcastInfo.metadataOffset - "audioN".length(), icyDataSource.remainingUntilMetadata);
        assertEquals(0, icyDataSource.metadataBytesToSkip);
    }

    @Test
    void sendToDataSourceListenersWithoutMetadata_canHandleIncompleteMetaDataFrames() {
        final byte[] buffer = "OFFSETaudio7audio8\u0001METADATAMETADATAaudio9audioAaudioB\u0001META".getBytes();
        final int offset = 6;
        icyDataSource.remainingUntilMetadata = "audioN".length() * 2;
        icyDataSource.shoutcastInfo.metadataOffset = "audioN".length() * 3;
        icyDataSource.metadataBytesToSkip = 0;
        icyDataSource.sendToDataSourceListenersWithoutMetadata(buffer, offset, buffer.length - offset);
        assertEquals("audio7audio8audio9audioAaudioB", transferredBytesWithoutMetadata.toString());
        assertEquals(16 - "META".length(), icyDataSource.metadataBytesToSkip);
        assertEquals(icyDataSource.shoutcastInfo.metadataOffset + 16 - "META".length(), icyDataSource.remainingUntilMetadata);
    }

    @Test
    void sendToDataSourceListenersWithoutMetadata_canHandleInterruptedMetadata() {
        sendToDataSourceListenersWithoutMetadata_canHandleIncompleteMetaDataFrames();
        final byte[] buffer = "DATAMETADATAaudioCaudioDaudioE\u0001METADATAMETADATAaudioF".getBytes();
        icyDataSource.sendToDataSourceListenersWithoutMetadata(buffer, 0, buffer.length);
        assertEquals("audio7audio8audio9audioAaudioBaudioCaudioDaudioEaudioF", transferredBytesWithoutMetadata.toString());
        assertEquals(0, icyDataSource.metadataBytesToSkip);
        assertEquals("audioN".length() * 2, icyDataSource.remainingUntilMetadata);
    }

    @Test
    void sendToDataSourceListenersWithoutMetadata_canHandleInterruptedAudioData() {
        sendToDataSourceListenersWithoutMetadata_canHandleMultipleMetadataFrames();
        final byte[] buffer = "audio7audio8".getBytes();
        icyDataSource.sendToDataSourceListenersWithoutMetadata(buffer, 0, buffer.length);
        assertEquals("audio1audio2audio3audio4audio5audio6audio7audio8", transferredBytesWithoutMetadata.toString());
        assertEquals(0, icyDataSource.metadataBytesToSkip);
        assertEquals(0, icyDataSource.metadataBytesToSkip);
    }

    static class TestDataSourceListener implements IcyDataSource.IcyDataSourceListener {

        @Override
        public void onDataSourceConnected() {

        }

        @Override
        public void onDataSourceConnectionLost() {

        }

        @Override
        public void onDataSourceConnectionLostIrrecoverably() {

        }

        @Override
        public void onDataSourceShoutcastInfo(@Nullable ShoutcastInfo shoutcastInfo) {

        }

        @Override
        public void onDataSourceStreamLiveInfo(StreamLiveInfo streamLiveInfo) {

        }

        @Override
        public void onDataSourceBytesRead(byte[] buffer, int offset, int length) {
            transferredBytesWithoutMetadata.append(new String(buffer, offset,length));
        }
    }

    static class TestTransferListener implements TransferListener {

        @Override
        public void onTransferInitializing(DataSource source, DataSpec dataSpec, boolean isNetwork) {

        }

        @Override
        public void onTransferStart(DataSource source, DataSpec dataSpec, boolean isNetwork) {

        }

        @Override
        public void onBytesTransferred(DataSource source, DataSpec dataSpec, boolean isNetwork, int bytesTransferred) {

        }

        @Override
        public void onTransferEnd(DataSource source, DataSpec dataSpec, boolean isNetwork) {

        }
    }

}