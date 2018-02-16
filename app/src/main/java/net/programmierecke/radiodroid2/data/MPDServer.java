package net.programmierecke.radiodroid2.data;

public class MPDServer {
    public int id;
    public boolean selected;
    public boolean connected;
    public String name;
    public String hostname;
    public int port;

    public MPDServer(int id, boolean selected, String name, String hostname, int port) {
        this.id = id;
        this.selected = selected;
        this.name = name;
        this.hostname = hostname;
        this.port = port;
    }
}
