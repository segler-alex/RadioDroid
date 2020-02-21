package net.programmierecke.radiodroid2.station.live;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.exoplayer2.metadata.icy.IcyHeaders;

import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.Response;

import static net.programmierecke.radiodroid2.Utils.parseIntWithDefault;

public class ShoutcastInfo implements Parcelable {
    public int metadataOffset;
    public int bitrate;

    // e.g.: ice-audio-info: ice-samplerate=44100;ice-bitrate=128;ice-channels=2
    public String audioInfo;

    public String audioDesc;

    // e.g.: icy-genre:Pop / Rock
    public String audioGenre;

    public String audioName;
    public String audioHomePage;

    // e.g.: Server: Icecast 2.3.2
    public String serverName;

    public boolean serverPublic;
    public int channels;
    public int sampleRate;

    public ShoutcastInfo() {
    }

    public ShoutcastInfo(IcyHeaders icyHeaders) {
        this.bitrate = icyHeaders.bitrate;
        this.audioGenre = icyHeaders.genre;
        this.serverPublic = icyHeaders.isPublic;
        this.audioName = icyHeaders.name;
        this.audioHomePage = icyHeaders.url;
    }

    public static ShoutcastInfo Decode(Response response) {
        ShoutcastInfo info = new ShoutcastInfo();
        info.metadataOffset = parseIntWithDefault(response.header("icy-metaint"), 0);
        info.bitrate = parseIntWithDefault(response.header("icy-br"), 0);
        // e.g.: ice-audio-info: ice-samplerate=44100;ice-bitrate=128;ice-channels=2
        info.audioInfo = response.header("ice-audio-info");
        info.audioDesc = response.header("icy-description");
        // e.g.: icy-genre:Pop / Rock
        info.audioGenre = response.header("icy-genre");
        info.audioName = response.header("icy-name");
        info.audioHomePage = response.header("icy-url");
        // e.g.: Server: Icecast 2.3.2
        info.serverName = response.header("Server");
        info.serverPublic = parseIntWithDefault(response.header("icy-pub"), 0) > 0;

        if (info.audioInfo != null) {
            Map<String, String> audioInfoParams = splitAudioInfo(info.audioInfo);

            info.channels = parseIntWithDefault(audioInfoParams.get("ice-channels"), 0);
            if (info.channels == 0) {
                info.channels = parseIntWithDefault(audioInfoParams.get("channels"), 0);
            }

            info.sampleRate = parseIntWithDefault(audioInfoParams.get("ice-samplerate"), 0);
            if (info.sampleRate == 0) {
                info.sampleRate = parseIntWithDefault(audioInfoParams.get("samplerate"), 0);
            }

            if (info.bitrate == 0) {
                info.bitrate = parseIntWithDefault(audioInfoParams.get("ice-bitrate"), 0);
                if (info.bitrate == 0) {
                    info.bitrate = parseIntWithDefault(audioInfoParams.get("bitrate"), 0);
                }
            }
        }

        // info needs at least metadataoffset
        if (info.metadataOffset == 0) {
            return null;
        }

        return info;
    }

    private static Map<String, String> splitAudioInfo(String audioInfo) {
        Map<String, String> params = new LinkedHashMap<>();
        String[] pairs = audioInfo.split(";");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            params.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return params;
    }

    protected ShoutcastInfo(Parcel in) {
        metadataOffset = in.readInt();
        bitrate = in.readInt();
        audioInfo = in.readString();
        audioDesc = in.readString();
        audioGenre = in.readString();
        audioName = in.readString();
        audioHomePage = in.readString();
        serverName = in.readString();
        serverPublic = in.readByte() != 0;
        channels = in.readInt();
        sampleRate = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(metadataOffset);
        dest.writeInt(bitrate);
        dest.writeString(audioInfo);
        dest.writeString(audioDesc);
        dest.writeString(audioGenre);
        dest.writeString(audioName);
        dest.writeString(audioHomePage);
        dest.writeString(serverName);
        dest.writeByte((byte) (serverPublic ? 1 : 0));
        dest.writeInt(channels);
        dest.writeInt(sampleRate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ShoutcastInfo> CREATOR = new Creator<ShoutcastInfo>() {
        @Override
        public ShoutcastInfo createFromParcel(Parcel in) {
            return new ShoutcastInfo(in);
        }

        @Override
        public ShoutcastInfo[] newArray(int size) {
            return new ShoutcastInfo[size];
        }
    };
}
