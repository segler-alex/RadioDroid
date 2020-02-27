package net.programmierecke.radiodroid2.players.selector;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.players.mpd.MPDClient;
import net.programmierecke.radiodroid2.players.mpd.MPDServerData;
import net.programmierecke.radiodroid2.players.mpd.MPDServersRepository;
import net.programmierecke.radiodroid2.service.PlayerService;
import net.programmierecke.radiodroid2.station.DataRadioStation;

import java.util.List;

import static net.programmierecke.radiodroid2.Utils.parseIntWithDefault;

public class PlayerSelectorDialog extends BottomSheetDialogFragment {

    public static final String FRAGMENT_TAG = "mpd_servers_dialog_fragment";

    private MPDClient mpdClient;
    private DataRadioStation stationToPlay;

    private BroadcastReceiver updateUIReceiver;

    private RecyclerView recyclerViewServers;
    private PlayerSelectorAdapter playerSelectorAdapter;

    private MPDServersRepository serversRepository;

    private Button btnEnableMPD;
    private Button btnAddMPDServer;

    public PlayerSelectorDialog(@NonNull MPDClient mpdClient) {
        this.mpdClient = mpdClient;
    }

    public PlayerSelectorDialog(@NonNull MPDClient mpdClient, @NonNull DataRadioStation stationToPlay) {
        this.mpdClient = mpdClient;
        this.stationToPlay = stationToPlay;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setRetainInstance(true);

        View view = inflater.inflate(R.layout.dialog_mpd_servers, container, false);

        RadioDroidApp radioDroidApp = (RadioDroidApp) requireActivity().getApplication();
        serversRepository = radioDroidApp.getMpdClient().getMpdServersRepository();

        recyclerViewServers = view.findViewById(R.id.recyclerViewMPDServers);
        GridLayoutManager llm = new GridLayoutManager(getContext(), 2, RecyclerView.VERTICAL, false);
        recyclerViewServers.setLayoutManager(llm);

        playerSelectorAdapter = new PlayerSelectorAdapter(requireContext(), stationToPlay);
        playerSelectorAdapter.setActionListener(new PlayerSelectorAdapter.ActionListener() {
            @Override
            public void editServer(@NonNull MPDServerData mpdServerData) {
                editOrAddServer(new MPDServerData(mpdServerData));
            }

            @Override
            public void removeServer(@NonNull MPDServerData mpdServerData) {
                serversRepository.removeServer(mpdServerData);
            }
        });
        recyclerViewServers.setAdapter(playerSelectorAdapter);

        btnEnableMPD = view.findViewById(R.id.btnEnableMPD);
        btnAddMPDServer = view.findViewById(R.id.btnAddMPDServer);

        btnEnableMPD.setOnClickListener(view12 -> {
            boolean mpdEnabled = !mpdClient.isMpdEnabled();

            mpdClient.setMPDEnabled(mpdEnabled);

            if (mpdEnabled) {
                mpdClient.enableAutoUpdate();
            } else {
                mpdClient.disableAutoUpdate();
            }

            updateEnableMpdButton();
        });

        btnAddMPDServer.setOnClickListener(view1 -> editOrAddServer(null));


        LiveData<List<MPDServerData>> servers = serversRepository.getAllServers();
        servers.observe(this, mpdServers -> playerSelectorAdapter.setEntries(mpdServers));

        updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (PlayerService.PLAYER_SERVICE_STATE_CHANGE.equals(intent.getAction())) {
                    playerSelectorAdapter.notifyRadioDroidPlaybackStateChanged();
                }
            }
        };

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();

        if (mpdClient.isMpdEnabled()) {
            mpdClient.enableAutoUpdate();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayerService.PLAYER_SERVICE_STATE_CHANGE);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(updateUIReceiver, filter);

        updateEnableMpdButton();
    }

    @Override
    public void onPause() {
        super.onPause();

        mpdClient.disableAutoUpdate();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(updateUIReceiver);
    }

    private void updateEnableMpdButton() {
        if (mpdClient.isMpdEnabled()) {
            btnEnableMPD.setText(R.string.action_disable_mpd);
        } else {
            btnEnableMPD.setText(R.string.action_enable_mpd);
        }
    }

    private void editOrAddServer(final @Nullable MPDServerData server) {
        LayoutInflater inflater = getLayoutInflater();
        View server_view = inflater.inflate(R.layout.layout_server_alert, null);
        final EditText editName = server_view.findViewById(R.id.mpd_server_name);
        final EditText editHostnameH = server_view.findViewById(R.id.mpd_server_hostname);
        final EditText editPassword = server_view.findViewById(R.id.mpd_server_password);
        final EditText editPort = server_view.findViewById(R.id.mpd_server_port);

        if (server != null) {
            editName.setText(server.name);
            editHostnameH.setText(server.hostname);
            editPort.setText(String.valueOf(server.port));
        }

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(server_view)
                .setPositiveButton(R.string.alert_select_mpd_server_save, null)
                .setNeutralButton(R.string.alert_select_mpd_server_remove, null)
                .setTitle(R.string.alert_add_or_edit_mpd_server).create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnRemove = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

            btnPositive.setOnClickListener(v -> {
                String serverName = editName.getText().toString().trim();
                String hostname = editHostnameH.getText().toString().trim();
                String password = editPassword.getText().toString().trim();
                int port = parseIntWithDefault(editPort.getText().toString().trim(), 0);

                if (serverName.isEmpty() || hostname.isEmpty() || port == 0) {
                    return;
                }

                if (server != null) {
                    server.name = serverName;
                    server.hostname = hostname;
                    server.port = port;
                    server.password = password;

                    serversRepository.updatePersistentData(server);
                } else {
                    MPDServerData server1 = new MPDServerData(serverName, hostname, port, password);
                    serversRepository.addServer(server1);
                }

                mpdClient.launchQuickCheck();

                dialog.cancel();
            });

            btnRemove.setOnClickListener(v -> {
                if (server != null) {
                    serversRepository.removeServer(server);
                    mpdClient.launchQuickCheck();
                }

                dialog.cancel();
            });
        });

        editName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
        editName.requestFocus();
        dialog.show();
    }
}
