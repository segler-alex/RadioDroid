package net.programmierecke.radiodroid2;
interface IPlayerService
{
void Play(String theUrl,String theName,String theID, boolean isAlarm);
void Stop();
void addTimer(int secondsAdd);
void clearTimer();
long getTimerSeconds();
String getCurrentStationID();
String getStationName();
Map getMetadataLive();
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
long getTransferedBytes();
}
