package net.programmierecke.radiodroid2.tests.utils.conditionwatcher;

import android.content.Context;
import android.media.AudioManager;

import androidx.test.core.app.ApplicationProvider;

import javax.annotation.Nonnull;

public class IsMusicPlayingCondition implements ConditionWatcher.Condition {
    private boolean expectPlaying;

    public IsMusicPlayingCondition(boolean expectPlaying) {
        this.expectPlaying = expectPlaying;
    }

    @Override
    public boolean testCondition() {
        AudioManager manager = (AudioManager) ApplicationProvider.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        return manager.isMusicActive() == expectPlaying;
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "";
    }
}
