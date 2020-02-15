package net.programmierecke.radiodroid2.service;

import android.os.Parcel;
import android.os.Parcelable;

public enum PauseReason implements Parcelable {

    NONE,
    BECAME_NOISY,
    FOCUS_LOSS,
    FOCUS_LOSS_TRANSIENT,
    METERED_CONNECTION,
    USER;

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<PauseReason> CREATOR = new Parcelable.Creator<PauseReason>() {
        @Override
        public PauseReason createFromParcel(Parcel in) {
            return PauseReason.values()[in.readInt()];
        }

        @Override
        public PauseReason[] newArray(int size) {
            return new PauseReason[size];
        }
    };
}
