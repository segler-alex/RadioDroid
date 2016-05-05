package net.programmierecke.radiodroid2;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

public class FragmentSettings extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
    }
}
