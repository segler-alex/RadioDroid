package net.programmierecke.radiodroid2.players;

import android.content.Context;
import android.os.Build;

import net.programmierecke.radiodroid2.PlayerService;

/**
 * Factory for creating RadioPlayer instances.
 */
public class RadioPlayerFactory {

    private RadioPlayerFactory() {
        // no instance
    }

    /** Creates a new radio player instance.
     * */
    public static RadioPlayer newRadioPlayer(Context context) {
        RadioPlayer radioPlayer;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            radioPlayer = new RadioPlayer(context, new ExoPlayerWrapper());
        } else {
            // use old MediaPlayer on API levels < 16
            // https://github.com/google/ExoPlayer/issues/711
            radioPlayer = new RadioPlayer(context, new MediaPlayerWrapper());
        }
        return radioPlayer;
    }
}
