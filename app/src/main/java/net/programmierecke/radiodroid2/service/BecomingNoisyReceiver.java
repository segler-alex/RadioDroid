package net.programmierecke.radiodroid2.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;

import androidx.preference.PreferenceManager;

public class BecomingNoisyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()) && PlayerServiceUtil.isPlaying()) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPref.getBoolean("pause_when_noisy", true)) {
                PlayerServiceUtil.pause(PauseReason.BECAME_NOISY);
            }
        }
    }
}
