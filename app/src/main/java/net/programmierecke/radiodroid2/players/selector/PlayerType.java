package net.programmierecke.radiodroid2.players.selector;

import android.os.Parcel;
import android.os.Parcelable;

public enum PlayerType implements Parcelable {
    MPD_SERVER(0),
    RADIODROID(1),
    EXTERNAL(2),
    CAST(3);

    private final int value;

    PlayerType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<PlayerType> CREATOR = new Parcelable.Creator<PlayerType>() {
        @Override
        public PlayerType createFromParcel(Parcel in) {
            return PlayerType.values()[in.readInt()];
        }

        @Override
        public PlayerType[] newArray(int size) {
            return new PlayerType[size];
        }
    };
}
