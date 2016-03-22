package net.programmierecke.radiodroid2;
interface IPlayerService
{
void Play(String theUrl,String theName,String theID);
void Stop();
String getCurrentStationID();
}
