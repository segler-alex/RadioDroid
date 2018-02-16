package net.programmierecke.radiodroid2;

import net.programmierecke.radiodroid2.data.StreamLiveInfo;

interface IPlayerService
{
void SaveInfo(String theUrl,String theName,String theID, String theIconUrl);
void Play(boolean isAlarm);
void Pause();
void Resume();
void Stop();
void addTimer(int secondsAdd);
void clearTimer();
long getTimerSeconds();
String getCurrentStationID();
String getStationName();
String getStationIconUrl();
StreamLiveInfo getMetadataLive();
String getMetadataStreamName();
String getMetadataServerName();
String getMetadataGenre();
String getMetadataHomepage();
int getMetadataBitrate();
int getMetadataSampleRate();
int getMetadataChannels();
boolean isPlaying();
void startRecording();
void stopRecording();
boolean isRecording();
String getCurrentRecordFileName();
long getTransferredBytes();
boolean getIsHls();
}
