package net.programmierecke.radiodroid2.players;

import android.content.Context;

interface PlayerWrapper {
    interface PlayStateListener {
        void onStateChanged(RadioPlayer.PlayState state);

        void onPlayerError(final int messageId);
    }

    void play(String proxyConnection, Context context, boolean isAlarm);

    void pause();

    void stop();

    boolean isPlaying();

    int getAudioSessionId();

    void setVolume(float newVolume);

    void setStateListener(PlayStateListener listener);
}
