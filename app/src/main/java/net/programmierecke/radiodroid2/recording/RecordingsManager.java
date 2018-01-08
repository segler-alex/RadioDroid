package net.programmierecke.radiodroid2.recording;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.data.DataRecording;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecordingsManager {
    private final static String TAG = "Recordings";
    private DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US);

    private class RunningRecordableListener implements RecordableListener {
        private RunningRecordingInfo runningRecordingInfo;
        private boolean ended;

        private RunningRecordableListener(@NonNull RunningRecordingInfo runningRecordingInfo) {
            this.runningRecordingInfo = runningRecordingInfo;
        }

        @Override
        public void onBytesAvailable(byte[] buffer, int offset, int length) {
            try {
                runningRecordingInfo.getOutputStream().write(buffer, offset, length);
                runningRecordingInfo.setBytesWritten(runningRecordingInfo.getBytesWritten() + length);
            } catch (IOException e) {
                e.printStackTrace();
                runningRecordingInfo.getRecordable().stopRecording();
            }
        }

        @Override
        public void onRecordingEnded() {
            if (ended) {
                return;
            }

            ended = true;

            try {
                runningRecordingInfo.getOutputStream().close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            RecordingsManager.this.stopRecording(runningRecordingInfo.getRecordable());
        }
    }

    private Map<Recordable, RunningRecordingInfo> runningRecordings = new HashMap<>();

    public void record(@NonNull Recordable recordable) {
        if (!recordable.canRecord()) {
            return;
        }

        if (!runningRecordings.containsKey(recordable)) {
            RunningRecordingInfo info = new RunningRecordingInfo();

            info.setRecordable(recordable);
            info.setTitle(recordable.getTitle());

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());

            String dataStr = dateFormatter.format(calendar.getTime());
            String sanitizedTitle = Utils.sanitizeName(info.getTitle());

            info.setFileName(String.format(Locale.US, "%s_%s.%s", sanitizedTitle, dataStr, recordable.getExtension()));

            //TODO: check available disk space here

            String filePath = RecordingsManager.getRecordDir() + "/" + info.getFileName();
            try {
                info.setOutputStream(new FileOutputStream(filePath));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }

            recordable.startRecording(new RunningRecordableListener(info));

            runningRecordings.put(recordable, info);
        }
    }

    public void stopRecording(@NonNull Recordable recordable) {
        recordable.stopRecording();

        runningRecordings.remove(recordable);
    }

    public RunningRecordingInfo getRecordingInfo(Recordable recordable) {
        return runningRecordings.get(recordable);
    }

    public Map<Recordable, RunningRecordingInfo> getRunningRecordings() {
        return Collections.unmodifiableMap(runningRecordings);
    }

    public static String getRecordDir() {
        String pathRecordings = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/Recordings";
        File folder = new File(pathRecordings);
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                Log.e(TAG, "could not create dir:" + pathRecordings);
            }
        }
        return pathRecordings;
    }

    public static DataRecording[] getRecordings() {
        String path = getRecordDir();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "path:" + path);
        }
        List<DataRecording> list = new ArrayList<DataRecording>();
        File folder = new File(path);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                DataRecording dr = new DataRecording();
                dr.Name = f.getName();
                dr.Time = new Date(f.lastModified());
                list.add(dr);
            }
        } else {
            Log.e(TAG, "could not enumerate files in recordings directory");
        }
        return list.toArray(new DataRecording[0]);
    }
}
