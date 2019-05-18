package net.programmierecke.radiodroid2.recording;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;

/* TODO: Actually have info about recording by storing them in the database and matching with files on disk.
 */
public class RecordingsManager {
    private final static String TAG = "Recordings";
    private DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private DateFormat timeFormatter = new SimpleDateFormat("HH-mm", Locale.US);

    private class RecordingsObservable extends Observable {
        @Override
        public synchronized boolean hasChanged() {
            return true;
        }
    }

    private Observable savedRecordingsObservable = new RecordingsObservable();

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
    private ArrayList<DataRecording> savedRecordings = new ArrayList<>();

    public void record(@NonNull Context context, @NonNull Recordable recordable) {
        if (!recordable.canRecord()) {
            return;
        }

        if (!runningRecordings.containsKey(recordable)) {
            RunningRecordingInfo info = new RunningRecordingInfo();

            info.setRecordable(recordable);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

            final String fileNameFormat = prefs.getString("record_name_formatting", context.getString(R.string.settings_record_name_formatting_default));

            final Map<String, String> formattingArgs = new HashMap<>(recordable.getRecordNameFormattingArgs());

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());

            final Date currentTime = calendar.getTime();

            String dateStr = dateFormatter.format(currentTime);
            String timeStr = timeFormatter.format(currentTime);

            formattingArgs.put("date", dateStr);
            formattingArgs.put("time", timeStr);

            final int recordNum = prefs.getInt("record_num", 1);
            formattingArgs.put("index", Integer.toString(recordNum));

            final String recordTitle = Utils.formatStringWithNamedArgs(fileNameFormat, formattingArgs);

            info.setTitle(recordTitle);
            info.setFileName(String.format("%s.%s", recordTitle, recordable.getExtension()));

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

            prefs.edit().putInt("record_num", recordNum + 1).apply();
        }
    }

    public void stopRecording(@NonNull Recordable recordable) {
        recordable.stopRecording();

        runningRecordings.remove(recordable);

        updateRecordingsList();
    }

    public RunningRecordingInfo getRecordingInfo(Recordable recordable) {
        return runningRecordings.get(recordable);
    }

    public Map<Recordable, RunningRecordingInfo> getRunningRecordings() {
        return Collections.unmodifiableMap(runningRecordings);
    }

    public List<DataRecording> getSavedRecordings() {
        return new ArrayList<>(savedRecordings);
    }

    public Observable getSavedRecordingsObservable() {
        return savedRecordingsObservable;
    }

    public static String getRecordDir() {
        String pathRecordings = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/Recordings";
        File folder = new File(pathRecordings);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Log.e(TAG, "could not create dir:" + pathRecordings);
            }
        }
        return pathRecordings;
    }

    public void updateRecordingsList() {
        String path = getRecordDir();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Updating recordings from " + path);
        }

        savedRecordings.clear();

        File folder = new File(path);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                DataRecording dr = new DataRecording();
                dr.Name = f.getName();
                dr.Time = new Date(f.lastModified());
                savedRecordings.add(dr);
            }

            Collections.sort(savedRecordings, (o1, o2) -> Long.compare(o2.Time.getTime(), o1.Time.getTime()));
        } else {
            Log.e(TAG, "Could not enumerate files in recordings directory");
        }

        savedRecordingsObservable.notifyObservers();
    }
}
