package net.programmierecke.radiodroid2;

import android.os.Environment;
import android.util.Log;

import net.programmierecke.radiodroid2.data.DataRecording;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecordingsManager {
    final static String TAG = "REC";

    public static String getRecordDir(){
        String pathRecordings = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/Recordings";
        File folder = new File(pathRecordings);
        if (!folder.exists()){
            if (!folder.mkdir()){
                Log.e(TAG,"could not create dir:"+pathRecordings);
            }
        }
        return pathRecordings;
    }

    public static DataRecording[] getRecordings(){
        String path = getRecordDir();
        Log.e(TAG,"path:"+path);
        List<DataRecording> list = new ArrayList<DataRecording>();
        File folder = new File(path);
        File[] files = folder.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                DataRecording dr = new DataRecording();
                dr.Name = f.getName();
                dr.Time = new Date(f.lastModified());
                list.add(dr);
            }
        }else{
            Log.e(TAG,"could not enumerate files in recordings directory");
        }
        return list.toArray(new DataRecording[0]);
    }
}
