package net.programmierecke.radiodroid2.data;

import java.net.URLConnection;

public class ShoutcastInfo {
    public int metadataOffset;
    public int bitrate;

    // e.g.: ice-audio-info: ice-samplerate=44100;ice-bitrate=128;ice-channels=2
    public String audioInfo;

    public String audioDesc;

    // e.g.: icy-genre:Pop / Rock
    public String audioGenre;

    public String audioName;
    public String audioHP;

    // e.g.: Server: Icecast 2.3.2
    public String serverName;

    public boolean serverPublic;
    public int channels;
    public int sampleRate;

    public static ShoutcastInfo Decode(URLConnection connection) {
        ShoutcastInfo info = new ShoutcastInfo();
        info.metadataOffset = connection.getHeaderFieldInt("icy-metaint", 0);
        info.bitrate = connection.getHeaderFieldInt("icy-br", 0);
        // e.g.: ice-audio-info: ice-samplerate=44100;ice-bitrate=128;ice-channels=2
        info.audioInfo = connection.getHeaderField("ice-audio-info");
        info.audioDesc = connection.getHeaderField("icy-description");
        // e.g.: icy-genre:Pop / Rock
        info.audioGenre = connection.getHeaderField("icy-genre");
        info.audioName = connection.getHeaderField("icy-name");
        info.audioHP = connection.getHeaderField("icy-url");
        // e.g.: Server: Icecast 2.3.2
        info.serverName = connection.getHeaderField("Server");
        info.serverPublic = connection.getHeaderFieldInt("icy-pub",0) > 0;

        if (info.audioInfo != null){
            String[] items = info.audioInfo.split(";");
            for (int i=0;i<items.length;i++){
                String[] item = items[i].split("=");
                if (item.length == 2){
                    String key = item[0];
                    if (key.equals("ice-channels") || key.equals("channels")){
                        try {
                            info.channels = Integer.parseInt(item[1]);
                        }catch(Exception e){};
                    } else if (key.equals("ice-samplerate") || key.equals("samplerate")){
                        try {
                            info.sampleRate = Integer.parseInt(item[1]);
                        }catch(Exception e){};
                    } else if (key.equals("ice-bitrate") || key.equals("bitrate")){
                        if (info.bitrate == 0) {
                            try {
                                info.bitrate = Integer.parseInt(item[1]);
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        }

        // info needs at least metadataoffset
        if (info.metadataOffset == 0){
            return null;
        }

        return info;
    }
}
