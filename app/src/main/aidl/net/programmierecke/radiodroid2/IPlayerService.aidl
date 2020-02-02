package net.programmierecke.radiodroid2;

import net.programmierecke.radiodroid2.service.PauseReason;
import net.programmierecke.radiodroid2.station.DataRadioStation;
import net.programmierecke.radiodroid2.station.live.StreamLiveInfo;
import net.programmierecke.radiodroid2.station.live.ShoutcastInfo;
import net.programmierecke.radiodroid2.players.PlayState;
import net.programmierecke.radiodroid2.players.selector.PlayerType;
import android.support.v4.media.session.MediaSessionCompat;

interface IPlayerService
{
void SetStation(in DataRadioStation station);
void Play(boolean isAlarm);
void Pause(in PauseReason pauseReason);
void Resume();
void Stop();
void SkipToNext();
void SkipToPrevious();
void addTimer(int secondsAdd);
void clearTimer();
long getTimerSeconds();
String getCurrentStationID();
DataRadioStation getCurrentStation();
StreamLiveInfo getMetadataLive();
ShoutcastInfo getShoutcastInfo();
MediaSessionCompat.Token getMediaSessionToken();
boolean isPlaying();
PlayState getPlayerState();
void startRecording();
void stopRecording();
boolean isRecording();
String getCurrentRecordFileName();
long getTransferredBytes();
long getBufferedSeconds();
long getLastPlayStartTime();
boolean getIsHls();
PauseReason getPauseReason();

void enableMPD(String hostname, int port);
void disableMPD();

void warnAboutMeteredConnection(in PlayerType playerType);
}
