package net.programmierecke.radiodroid2.interfaces;

import net.programmierecke.radiodroid2.data.ShoutcastInfo;

import java.util.Map;

public interface IStreamProxyEventReceiver {
    void foundShoutcastStream(ShoutcastInfo bitrate, boolean isHls);
    void foundLiveStreamInfo(Map<String,String> liveInfo);
    void streamCreated(String proxyConnection);
    void streamStopped();
}
