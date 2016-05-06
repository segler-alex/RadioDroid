package net.programmierecke.radiodroid2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import net.programmierecke.radiodroid2.interfaces.IApplicationSelected;

public class FragmentSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, IApplicationSelected {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
        findPreference("shareapp_package").setSummary(getPreferenceManager().getSharedPreferences().getString("shareapp_package",""));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        findPreference("shareapp_package").setSummary(getPreferenceManager().getSharedPreferences().getString("shareapp_package",""));
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
        String key) {
        Log.e("AAA","changed key:"+key);
        if (key.equals("alarm_external")) {
            boolean active = sharedPreferences.getBoolean(key,false);
            if (active) {
                ApplicationSelectorDialog newFragment = new ApplicationSelectorDialog();
                newFragment.setCallback(this);
                newFragment.show(getActivity().getSupportFragmentManager(), "appPicker");
            }
        }
    }

    @Override
    public void onAppSelected(String packageName, String activityName) {
        Log.w("SEL","selected:"+packageName+"/"+activityName);
        SharedPreferences.Editor ed = getPreferenceManager().getSharedPreferences().edit();
        ed.putString("shareapp_package",packageName);
        ed.putString("shareapp_activity",activityName);
        ed.commit();

        findPreference("shareapp_package").setSummary(packageName);
    }
}
