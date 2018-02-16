package net.programmierecke.radiodroid2.recording;

import java.io.FileOutputStream;

public class RunningRecordingInfo {
    private Recordable recordable;
    private String title;
    private String fileName;
    private FileOutputStream outputStream;
    private long bytesWritten;

    public Recordable getRecordable() {
        return recordable;
    }

    protected void setRecordable(Recordable recordable) {
        this.recordable = recordable;
    }

    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    public String getFileName() {
        return fileName;
    }

    protected void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public FileOutputStream getOutputStream() {
        return outputStream;
    }

    protected void setOutputStream(FileOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    protected void setBytesWritten(long bytesWritten) {
        this.bytesWritten = bytesWritten;
    }
}
