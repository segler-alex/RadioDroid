package net.programmierecke.radiodroid2.proxy;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static net.programmierecke.radiodroid2.Utils.parseIntWithDefault;

public class ProxySettingsDialog extends DialogFragment {

    final static private String TEST_ADDRESS = "http://radio-browser.info";

    private EditText editProxyHost;
    private EditText editProxyPort;
    private AppCompatSpinner spinnerProxyType;
    private EditText editLogin;
    private EditText editProxyPassword;
    private TextView textProxyTestResult;

    private ArrayAdapter<Proxy.Type> proxyTypeAdapter;

    private AsyncTask<Void, Void, Void> proxyTestTask;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View layout = inflater.inflate(R.layout.dialog_proxy_settings, null);

        editProxyHost = layout.findViewById(R.id.edit_proxy_host);
        editProxyPort = layout.findViewById(R.id.edit_proxy_port);
        spinnerProxyType = layout.findViewById(R.id.spinner_proxy_type);
        editLogin = layout.findViewById(R.id.edit_proxy_login);
        editProxyPassword = layout.findViewById(R.id.edit_proxy_password);
        textProxyTestResult = layout.findViewById(R.id.text_test_proxy_result);

        proxyTypeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                new Proxy.Type[]{Proxy.Type.DIRECT, Proxy.Type.HTTP, Proxy.Type.SOCKS});

        spinnerProxyType.setAdapter(proxyTypeAdapter);

        {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            ProxySettings proxySettings = ProxySettings.fromPreferences(sharedPref);

            if (proxySettings != null) {
                editProxyHost.setText(proxySettings.host);
                editProxyPort.setText(Integer.toString(proxySettings.port));
                editLogin.setText(proxySettings.login);
                editProxyPassword.setText(proxySettings.password);
                spinnerProxyType.setSelection(proxyTypeAdapter.getPosition(proxySettings.type));
            }
        }

        final Dialog dialog = builder.setView(layout)
                .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor editor = sharedPref.edit();

                        ProxySettings proxySettings = createProxySettings();
                        proxySettings.toPreferences(editor);
                        editor.apply();

                        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
                        radioDroidApp.rebuildHttpClient();
                    }
                })
                .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ProxySettingsDialog.this.getDialog().cancel();
                    }
                })
                .setNeutralButton(R.string.settings_proxy_action_test, null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ProxySettings proxySettings = createProxySettings();
                        testProxy(proxySettings);
                    }
                });
            }
        });

        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (proxyTestTask != null) {
            proxyTestTask.cancel(true);
        }
    }

    private ProxySettings createProxySettings() {
        ProxySettings settings = new ProxySettings();

        settings.host = editProxyHost.getText().toString();
        settings.port = parseIntWithDefault(editProxyPort.getText().toString(), 0);
        settings.login = editLogin.getText().toString();
        settings.password = editProxyPassword.getText().toString();
        settings.type = proxyTypeAdapter.getItem(spinnerProxyType.getSelectedItemPosition());

        return settings;
    }

    private static class ConnectionTesterTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<TextView> textProxyTestResult;

        private OkHttpClient okHttpClient;
        private Call call;

        private String connectionSuccessStr;
        private String connectionFailedStr;
        private String connectionInvalidInputStr;

        private boolean requestSucceeded = false;
        private String errorStr;

        private ConnectionTesterTask(@NonNull RadioDroidApp radioDroidApp, @NonNull TextView textProxyTestResult,
                                     @NonNull ProxySettings proxySettings) {
            this.textProxyTestResult = new WeakReference<>(textProxyTestResult);

            textProxyTestResult.setText("");

            connectionSuccessStr = radioDroidApp.getString(R.string.settings_proxy_working, TEST_ADDRESS);
            connectionFailedStr = radioDroidApp.getString(R.string.settings_proxy_not_working);
            connectionInvalidInputStr = radioDroidApp.getString(R.string.settings_proxy_invalid);

            OkHttpClient.Builder builder = radioDroidApp.newHttpClientWithoutProxy()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS);

            if (!Utils.setOkHttpProxy(builder, proxySettings)){
                // proxy settings are not valid
            }else{
                okHttpClient = builder.build();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (okHttpClient == null)
                return;

            Request.Builder builder = new Request.Builder().url(TEST_ADDRESS);
            call = okHttpClient.newCall(builder.build());
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (okHttpClient == null)
                return null;
            try {
                Response response = call.execute();
                requestSucceeded = response.isSuccessful();
                if (!requestSucceeded) {
                    errorStr = response.message();
                }
            } catch (IOException e) {
                requestSucceeded = false;
                errorStr = e.getMessage();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);

            TextView textResult = textProxyTestResult.get();
            if (textResult == null) {
                return;
            }

            if (okHttpClient == null){
                textResult.setText(connectionInvalidInputStr);
            } else {
                if (requestSucceeded) {
                    textResult.setText(connectionSuccessStr);
                } else {
                    textResult.setText(String.format(connectionFailedStr, TEST_ADDRESS, errorStr));
                }
            }
        }
    }

    private void testProxy(@NonNull ProxySettings proxySettings) {
        if (proxyTestTask != null) {
            proxyTestTask.cancel(true);
        }

        RadioDroidApp radioDroidApp = (RadioDroidApp) getActivity().getApplication();
        proxyTestTask = new ConnectionTesterTask(radioDroidApp, textProxyTestResult, proxySettings);
        proxyTestTask.execute();
    }
}
