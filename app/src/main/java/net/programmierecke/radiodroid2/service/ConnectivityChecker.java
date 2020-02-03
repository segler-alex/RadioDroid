package net.programmierecke.radiodroid2.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.core.net.ConnectivityManagerCompat;

public class ConnectivityChecker {

    public enum ConnectionType {
        NOT_METERED,
        METERED
    }

    public interface ConnectivityCallback {
        void onConnectivityChanged(boolean connected, ConnectionType connectionType);
    }

    private ConnectivityManager connectivityManager;

    private ConnectivityManager.NetworkCallback networkCallback;
    private BroadcastReceiver networkBroadcastReceiver;

    private ConnectivityCallback connectivityCallback;

    private ConnectionType lastConnectionType;

    public static ConnectionType getCurrentConnectionType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return ConnectivityManagerCompat.isActiveNetworkMetered(connectivityManager) ? ConnectionType.METERED : ConnectionType.NOT_METERED;
    }

    public void startListening(Context context, ConnectivityCallback connectivityCallback) {
        this.connectivityCallback = connectivityCallback;

        if (networkCallback != null || networkBroadcastReceiver != null) {
            return;
        }

        lastConnectionType = getCurrentConnectionType(context);

        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    boolean connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    boolean metered = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
                    onConnectivityChanged(connected, metered ? ConnectionType.METERED : ConnectionType.NOT_METERED);
                }
                // -Snip-
            };
            connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback);
        } else {
            networkBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    boolean connected = !intent.hasExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY);
                    onConnectivityChanged(connected, ConnectivityManagerCompat.isActiveNetworkMetered(connectivityManager) ? ConnectionType.METERED : ConnectionType.NOT_METERED);
                }
            };
            context.registerReceiver(networkBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    public void stopListening(Context context) {
        this.connectivityCallback = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && networkCallback != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        } else if (networkBroadcastReceiver != null) {
            context.unregisterReceiver(networkBroadcastReceiver);
            networkBroadcastReceiver = null;
        }
    }

    private void onConnectivityChanged(boolean connected, ConnectionType connectionType) {
        if (lastConnectionType == connectionType) {
            return;
        } else {
            lastConnectionType = connectionType;
        }

        if (connectivityCallback != null) {
            connectivityCallback.onConnectivityChanged(connected, connectionType);
        }
    }
}
