package net.programmierecke.radiodroid2.players;

import android.os.Parcel;
import android.os.Parcelable;

public enum PlayState implements Parcelable {
    Idle("IDLE"),
    PrePlaying("PRE_PLAYING"),
    Playing("PLAYING"),
    Paused("PAUSED");

    public static final Parcelable.Creator<PlayState> CREATOR = new Parcelable.Creator<PlayState>() {
        public PlayState createFromParcel(Parcel in) {
            return values()[in.readInt()];
        }

        public PlayState[] newArray(int size) {
            return new PlayState[size];
        }
    };

    private final String option;

    PlayState(String option) {
        this.option = option;
    }

    public String getName() {
        return option;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(ordinal());
    }
}