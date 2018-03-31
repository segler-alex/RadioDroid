package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.AudioEffect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.programmierecke.radiodroid2.data.MPDServer;
import net.programmierecke.radiodroid2.interfaces.IApplicationSelected;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static net.programmierecke.radiodroid2.ActivityMain.FRAGMENT_FROM_BACKSTACK;

public class FragmentSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, IApplicationSelected {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
        final ListPreference servers = (ListPreference) findPreference("radiobrowser_server");
        updateDnsList(servers);
        /*servers.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        updateDnsList((ListPreference)preference);
                        return false;
                    }
                }
        );*/
        findPreference("shareapp_package").setSummary(getPreferenceManager().getSharedPreferences().getString("shareapp_package", ""));
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
        findPreference("mpd_servers_viewer").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showAllMPDServers();
                return false;
            }
        });

        findPreference("show_statistics").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((ActivityMain)getActivity()).getToolbar().setTitle(R.string.settings_statistics);
                FragmentServerInfo f = new FragmentServerInfo();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.containerView, f).addToBackStack(String.valueOf(FRAGMENT_FROM_BACKSTACK)).commit();
                return false;
            }
        });

        findPreference("show_about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((ActivityMain)getActivity()).getToolbar().setTitle(R.string.settings_about);
                FragmentAbout f = new FragmentAbout();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.containerView, f).addToBackStack(String.valueOf(FRAGMENT_FROM_BACKSTACK)).commit();
                return false;
            }
        });


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            findPreference("settings_retry_timeout").setVisible(false);
            findPreference("settings_retry_delay").setVisible(false);
        }
    }

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
                Vector<String> listResult = new Vector<String>();
                // add default server
                listResult.add("www.radio-browser.info/webservice");
                // add best server based on geodns location
                listResult.add("api.radio-browser.online");
                // add round robin address
                listResult.add("all.api.radio-browser.info");
                try {
                    // add all round robin servers one by one to select them separately
                    InetAddress[] list = InetAddress.getAllByName("all.api.radio-browser.info");
                    for (InetAddress item : list) {
                        Log.e("XXX", item.toString() + " -> " + item.getCanonicalHostName());
                        listResult.add(item.getCanonicalHostName());
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                return listResult.toArray(new String[0]);
            }

            @Override
            protected void onPostExecute(String[] result) {
                setServersData(result, lp);
                super.onPostExecute(result);
            }
        }.execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
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

    private void showAllMPDServers() {
        final List<MPDServer> servers = Utils.getMPDServers(getContext());
        List<String> serversNames = new ArrayList<>();
        for (MPDServer server : servers) {
            serversNames.add(server.name);
        }
        if(serversNames.size() == 0)
        {
            editMPDServer(null);
            return;
        }

        new AlertDialog.Builder(getContext())
                .setItems(serversNames.toArray(new String[serversNames.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editMPDServer(servers.get(which));
                    }
                })
                .setNeutralButton(R.string.alert_select_mpd_server_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editMPDServer(null);
                    }
                })
                .setTitle(R.string.settings_view_mpd_servers)
                .show();
    }

    private void editMPDServer(final MPDServer server) {
        LayoutInflater inflater = getLayoutInflater();
        View server_view = inflater.inflate(R.layout.layout_server_alert, null);
        final EditText name = (EditText) server_view.findViewById(R.id.mpd_server_name);
        final EditText hostname = (EditText) server_view.findViewById(R.id.mpd_server_hostname);
        final EditText port = (EditText) server_view.findViewById(R.id.mpd_server_port);

        if(server != null) {
            name.setText(server.name);
            hostname.setText(server.hostname);
            port.setText(String.valueOf(server.port));
        }

        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(server_view)
                .setPositiveButton(R.string.alert_select_mpd_server_save, null)
                .setNeutralButton(R.string.alert_select_mpd_server_remove, null)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if(Utils.getMPDServers(getContext()).size() > 0)
                            showAllMPDServers();

                        MPDClient.StopDiscovery();
                        MPDClient.StartDiscovery(new WeakReference<Context>(getActivity()));
                    }
                })
                .setTitle(R.string.alert_add_or_edit_mpd_server).create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button remove = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

                positive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        List<MPDServer> servers = Utils.getMPDServers(getContext());
                        String nameText = name.getText().toString().trim();
                        String hostnameText = hostname.getText().toString().trim();
                        int portText = Integer.parseInt(port.getText().toString().trim());

                        if(nameText.isEmpty() || hostnameText.isEmpty() || portText == 0)
                            return;

                        if(server != null) {
                            server.name = nameText;
                            server.hostname = hostnameText;
                            server.port = portText;
                            servers.set(server.id, server);
                        }
                        else {
                            MPDServer server = new MPDServer(servers.size(), false, nameText, hostnameText, portText);
                            servers.add(server);
                        }
                        Utils.saveMPDServers(servers, getContext());
                        dialog.cancel();
                    }
                });

                remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(server != null) {
                            List<MPDServer> servers = Utils.getMPDServers(getContext());
                            for (MPDServer mpdServer : servers) {
                                if(mpdServer.id > server.id)
                                    mpdServer.id --;
                            }
                            servers.remove(server.id);
                            Utils.saveMPDServers(servers, getContext());
                        }
                        dialog.cancel();
                    }
                });
            }
        });

        name.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
        name.requestFocus();
        dialog.show();
    }
}
