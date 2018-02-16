package net.programmierecke.radiodroid2.recording;

public interface RecordableListener {
    void onBytesAvailable(byte[] buffer, int offset, int length);
    void onRecordingEnded();
}
