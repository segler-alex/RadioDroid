package net.programmierecke.radiodroid2.players.mpd;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class MPDServerData {
    public enum Status {
        Idle,
        Paused,
        Playing,
    }

    // Persistent data
    public int id = -1;
    public String name;
    public String hostname;
    public int port;
    public String password;

    // Runtime status
    public boolean isReachable = false;
    public Status status = MPDServerData.Status.Idle;
    public int volume = 0;

    public boolean connected = false;

    public MPDServerData(@NonNull String name, @NonNull String hostname, int port, String password) {
        this.name = name;
        this.hostname = hostname;
        this.port = port;
        this.password = password;
    }

    public MPDServerData(MPDServerData other) {
        this.id = other.id;
        this.name = other.name;
        this.hostname = other.hostname;
        this.password = other.password;
        this.port = other.port;
        this.isReachable = other.isReachable;
        this.status = other.status;
        this.volume = other.volume;
        this.connected = other.connected;
    }

    public void updateStatus(@NonNull String str) {
        Map<String, String> statusMap = new HashMap<>();
        String[] lines = str.split("\\R");
        for (String line : lines) {
            String[] keyAndValue = line.split(": ", 2);
            if (keyAndValue.length == 2) {
                statusMap.put(keyAndValue[0], keyAndValue[1]);
            }
        }

        if (statusMap.containsKey("volume")) {
            volume = Integer.parseInt(statusMap.get("volume"));
        } else {
            volume = 0;
        }

        if (statusMap.containsKey("state")) {
            String stateStr = statusMap.get("state");
            assert stateStr != null;

            switch (stateStr) {
                case "stop":
                    status = Status.Idle;
                    break;
                case "pause":
                    status = Status.Paused;
                    break;
                case "play":
                    status = Status.Playing;
                    break;
            }
        }

        connected = true;
    }

    public boolean contentEquals(MPDServerData o) {
        if (o == null) return false;

        if (id != o.id) return false;
        if (port != o.port) return false;
        if (isReachable != o.isReachable) return false;
        if (volume != o.volume) return false;
        if (connected != o.connected) return false;
        if (password != null ? !password.equals(o.password) : o.password != null) return false;
        if (name != null ? !name.equals(o.name) : o.name != null) return false;
        if (hostname != null ? !hostname.equals(o.hostname) : o.hostname != null)
            return false;
        return status == o.status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MPDServerData that = (MPDServerData) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
