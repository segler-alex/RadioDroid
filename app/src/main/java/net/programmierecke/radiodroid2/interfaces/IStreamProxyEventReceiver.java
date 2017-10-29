package net.programmierecke.radiodroid2.interfaces;

import net.programmierecke.radiodroid2.data.ShoutcastInfo;
import net.programmierecke.radiodroid2.data.StreamLiveInfo;

public interface IStreamProxyEventReceiver {
    void foundShoutcastStream(ShoutcastInfo bitrate, boolean isHls);
    void foundLiveStreamInfo(StreamLiveInfo liveInfo);
    void streamCreated(String proxyConnection);
    void streamStopped();
}
