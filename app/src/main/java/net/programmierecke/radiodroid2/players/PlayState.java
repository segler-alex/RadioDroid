package net.programmierecke.radiodroid2.players;

import android.os.Parcel;
import android.os.Parcelable;

public enum PlayState implements Parcelable {
    Idle,
    PrePlaying,
    Playing,
    Paused;

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PlayState> CREATOR = new Creator<PlayState>() {
        @Override
        public PlayState createFromParcel(Parcel in) {
            return PlayState.values()[in.readInt()];
        }

        @Override
        public PlayState[] newArray(int size) {
            return new PlayState[size];
        }
    };
}
