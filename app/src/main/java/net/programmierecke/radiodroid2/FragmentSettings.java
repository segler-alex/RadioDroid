package net.programmierecke.radiodroid2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.AudioEffect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;

import android.util.Log;
import android.widget.Toast;

import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial;
import com.bytehamster.lib.preferencesearch.SearchConfiguration;
import com.bytehamster.lib.preferencesearch.SearchPreference;

import net.programmierecke.radiodroid2.interfaces.IApplicationSelected;
import net.programmierecke.radiodroid2.proxy.ProxySettingsDialog;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import static net.programmierecke.radiodroid2.ActivityMain.FRAGMENT_FROM_BACKSTACK;

public class FragmentSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, IApplicationSelected, PreferenceFragmentCompat.OnPreferenceStartScreenCallback  {

    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    public static FragmentSettings openNewSettingsSubFragment(ActivityMain activity, String key) {
        FragmentSettings f = new FragmentSettings();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key);
        f.setArguments(args);
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.containerView, f).addToBackStack(String.valueOf(FRAGMENT_FROM_BACKSTACK)).commit();
        return f;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat,
                                           PreferenceScreen preferenceScreen) {
        openNewSettingsSubFragment((ActivityMain) getActivity(), preferenceScreen.getKey());
        return true;
    }

    private boolean isToplevel() {
        return getPreferenceScreen() == null || getPreferenceScreen().getKey().equals("pref_toplevel");
    }

    private void refreshToplevelIcons() {
        findPreference("shareapp_package").setSummary(getPreferenceManager().getSharedPreferences().getString("shareapp_package", ""));
        findPreference("pref_category_ui").setIcon(Utils.IconicsIcon(getContext(), CommunityMaterial.Icon2.cmd_monitor));
        findPreference("pref_category_startup").setIcon(Utils.IconicsIcon(getContext(), GoogleMaterial.Icon.gmd_flight_takeoff));
        findPreference("pref_category_interaction").setIcon(Utils.IconicsIcon(getContext(), CommunityMaterial.Icon.cmd_gesture_tap));
        findPreference("pref_category_player").setIcon(Utils.IconicsIcon(getContext(), CommunityMaterial.Icon2.cmd_play));
        findPreference("pref_category_alarm").setIcon(Utils.IconicsIcon(getContext(), CommunityMaterial.Icon.cmd_clock_outline));
        findPreference("pref_category_connectivity").setIcon(Utils.IconicsIcon(getContext(), GoogleMaterial.Icon.gmd_import_export));
        findPreference("pref_category_recordings").setIcon(Utils.IconicsIcon(getContext(), CommunityMaterial.Icon2.cmd_record_rec));
        findPreference("pref_category_mpd").setIcon(Utils.IconicsIcon(getContext(), CommunityMaterial.Icon2.cmd_speaker_wireless));
        findPreference("pref_category_other").setIcon(Utils.IconicsIcon(getContext(), CommunityMaterial.Icon2.cmd_information_outline));
    }

    private void refreshToolbar() {
        ActivityMain activity = (ActivityMain) getActivity();
        final Toolbar myToolbar = activity.getToolbar(); //findViewById(R.id.my_awesome_toolbar);

        if (myToolbar == null || getPreferenceScreen() == null)
            return;

        myToolbar.setTitle(getPreferenceScreen().getTitle());

        if (Utils.bottomNavigationEnabled(activity)) {
            if (isToplevel()) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                activity.getSupportActionBar().setDisplayShowHomeEnabled(false);
                myToolbar.setNavigationOnClickListener(v -> activity.onBackPressed());
            } else {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
                myToolbar.setNavigationOnClickListener(v -> activity.onBackPressed());
            }
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.preferences, s);
        refreshToolbar();
        if (s == null) {
            refreshToplevelIcons();
            SearchPreference searchPreference = (SearchPreference) findPreference("searchPreference");
            SearchConfiguration config = searchPreference.getSearchConfiguration();
            config.setActivity((AppCompatActivity) getActivity());
            config.index(R.xml.preferences);
        } else if (s.equals("pref_category_player")) {
            findPreference("equalizer").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);

                    if (getContext().getPackageManager().resolveActivity(intent, 0) == null) {
                        Toast.makeText(getContext(), R.string.error_no_equalizer_found, Toast.LENGTH_SHORT).show();
                    } else {
                        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                        startActivityForResult(intent, ActivityMain.LAUNCH_EQUALIZER_REQUEST);
                    }

                    return false;
                }
            });
        } else if (s.equals("pref_category_connectivity")) {
            //final ListPreference servers = (ListPreference) findPreference("radiobrowser_server");
            //updateDnsList(servers);

            findPreference("settings_proxy").setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ProxySettingsDialog proxySettingsDialog = new ProxySettingsDialog();
                    proxySettingsDialog.setCancelable(true);
                    proxySettingsDialog.show(getFragmentManager(), "");
                    return false;
                }
            });

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                findPreference("settings_retry_timeout").setVisible(false);
                findPreference("settings_retry_delay").setVisible(false);
            }
        } else if (s.equals("pref_category_mpd")) {
            findPreference("mpd_servers_viewer").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    RadioDroidApp radioDroidApp = (RadioDroidApp) requireActivity().getApplication();
                    Utils.showMpdServersDialog(radioDroidApp, requireActivity().getSupportFragmentManager(), null);
                    return false;
                }
            });
        } else if (s.equals("pref_category_other")) {
            findPreference("show_statistics").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ((ActivityMain) getActivity()).getToolbar().setTitle(R.string.settings_statistics);
                    FragmentServerInfo f = new FragmentServerInfo();
                    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.containerView, f).addToBackStack(String.valueOf(FRAGMENT_FROM_BACKSTACK)).commit();
                    return false;
                }
            });

            findPreference("show_about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ((ActivityMain) getActivity()).getToolbar().setTitle(R.string.settings_about);
                    FragmentAbout f = new FragmentAbout();
                    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.containerView, f).addToBackStack(String.valueOf(FRAGMENT_FROM_BACKSTACK)).commit();
                    return false;
                }
            });
        }
    }

    /*
    private void setServersData(String[] list, ListPreference servers) {
        servers.setEntries(list);
        if (list.length > 0){
            servers.setDefaultValue(list[0]);
        }
        servers.setEntryValues(list);
    }

    void updateDnsList(final ListPreference lp){
        final AsyncTask<Void, Void, String[]> xxx = new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... params) {
                return RadioBrowserServerManager.getServerList(false);
            }

            @Override
            protected void onPostExecute(String[] result) {
                setServersData(result, lp);
                super.onPostExecute(result);
            }
        }.execute();
    }
    */

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        refreshToolbar();

        if(isToplevel())
            refreshToplevelIcons();

        if(findPreference("shareapp_package") != null)
            findPreference("shareapp_package").setSummary(getPreferenceManager().getSharedPreferences().getString("shareapp_package", ""));
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (BuildConfig.DEBUG) {
            Log.d("AAA", "changed key:" + key);
        }
        if (key.equals("alarm_external")) {
            boolean active = sharedPreferences.getBoolean(key, false);
            if (active) {
                ApplicationSelectorDialog newFragment = new ApplicationSelectorDialog();
                newFragment.setCallback(this);
                newFragment.show(getActivity().getSupportFragmentManager(), "appPicker");
            }
        }
        if (key.equals("theme_name") || key.equals("circular_icons") || key.equals("bottom_navigation")) {
            if (key.equals("circular_icons"))
                ((RadioDroidApp) getActivity().getApplication()).getFavouriteManager().updateShortcuts();
            getActivity().recreate();
        }
    }

    @Override
    public void onAppSelected(String packageName, String activityName) {
        if (BuildConfig.DEBUG) {
            Log.d("SEL", "selected:" + packageName + "/" + activityName);
        }
        SharedPreferences.Editor ed = getPreferenceManager().getSharedPreferences().edit();
        ed.putString("shareapp_package", packageName);
        ed.putString("shareapp_activity", activityName);
        ed.commit();

        findPreference("shareapp_package").setSummary(packageName);
    }
}
