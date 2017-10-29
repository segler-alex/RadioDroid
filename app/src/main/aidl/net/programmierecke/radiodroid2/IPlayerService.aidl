package net.programmierecke.radiodroid2;

import net.programmierecke.radiodroid2.data.StreamLiveInfo;

interface IPlayerService
{
void Play(String theUrl,String theName,String theID, boolean isAlarm);
void Pause();
void Resume();
void Stop();
void addTimer(int secondsAdd);
void clearTimer();
long getTimerSeconds();
String getCurrentStationID();
String getStationName();
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
