package net.programmierecke.radiodroid2.playlist;

import android.util.Log;

import net.programmierecke.radiodroid2.BuildConfig;

/**
 * Created by segler on 04.03.17.
 */

public class PlaylistM3UEntry {
    final static String EXTINF = "#EXTINF:";
    final static String STREAMINF = "#EXT-X-STREAM-INF:";
    final static String STREAMINF_PROGRAM = "PROGRAM-ID=";
    final static String STREAMINF_BANDWIDTH = "BANDWIDTH=";
    final static String STREAMINF_CODECS = "CODECS=";

    final static String TAG = "M3U";

    String header;
    String content;
    int length = -1;
    String title = null;
    int bitrate = -1;
    int programid = -1;
    boolean isStreamInfo = false;

    public PlaylistM3UEntry(String _header, String _content){
        header = _header;
        content = _content;

        decode();
    }

    public PlaylistM3UEntry(String _content){
        header = null;
        content = _content;
    }

    void decode() {
        if (header == null) {
            return;
        }
        if (header.startsWith(EXTINF)) {
            if(BuildConfig.DEBUG) { Log.d(TAG,"found EXTINF:"+header); }
            String attributes = header.substring(EXTINF.length());
            int sep = attributes.indexOf(",");
            String timeStr = attributes.substring(0, sep);
            length = Integer.getInteger(timeStr, -1);
            title = attributes.substring(sep + 1);
        } else if (header.startsWith(STREAMINF)) {
            if(BuildConfig.DEBUG) { Log.d(TAG,"found STREAMINFO:"+header); }
            isStreamInfo = true;
            String attributes = header.substring(STREAMINF.length());
            String[] attributesList = attributes.split(",");
            for (String attr : attributesList) {
                if (attr.startsWith(STREAMINF_BANDWIDTH)) {
                    String paramStr = attr.substring(STREAMINF_BANDWIDTH.length());
                    bitrate = Integer.getInteger(paramStr, -1);
                }
                if (attr.startsWith(STREAMINF_PROGRAM)) {
                    String paramStr = attr.substring(STREAMINF_PROGRAM.length());
                    programid = Integer.getInteger(paramStr, -1);
                }
            }
        }
    }

    public boolean getIsStreamInformation(){
        return isStreamInfo;
    }

    public int getBitrate(){
        return bitrate;
    }

    public int getLength(){
        return length;
    }

    public String getTitle(){
        return title;
    }

    public int getProgramId(){
        return programid;
    }

    public String getContent(){
        return content;
    }
}
