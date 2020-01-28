package net.programmierecke.radiodroid2.players.mpd;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class MPDAsyncTask implements Runnable {
    private static String TAG = "MPDAsyncTask";

    public interface ReadStage {
        boolean onRead(@NonNull MPDAsyncTask task, @NonNull String result);
    }

    public interface WriteStage {
        boolean onWrite(@NonNull MPDAsyncTask task, @NonNull BufferedWriter bufferedWriter) throws IOException;
    }

    public interface FailureCallback {
        void onFailure(@NonNull MPDAsyncTask task);
    }

    private LinkedList<ReadStage> readStages;
    private LinkedList<WriteStage> writeStages;
    private FailureCallback failureCallback;

    private long timeoutMs;

    private MPDServerData mpdServerData;
    private MPDClient mpdClient;

    public MPDAsyncTask() {
    }

    protected void setStages(ReadStage[] readStages, WriteStage[] writeStages, @Nullable FailureCallback failureCallback) {
        this.readStages = new LinkedList<>(Arrays.asList(readStages));
        this.writeStages = new LinkedList<>(Arrays.asList(writeStages));
        this.failureCallback = failureCallback;
    }

    protected void setTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    protected void fail() {
        if (failureCallback != null) {
            failureCallback.onFailure(MPDAsyncTask.this);
        }
    }

    @Override
    public void run() {
        try {
            if (!TextUtils.isEmpty(mpdServerData.password)){
                this.readStages.addFirst(okReadStage());
                this.writeStages.addFirst(loginWriteStage(mpdServerData.password));
            }

            Socket s = new Socket();
            s.connect(new InetSocketAddress(mpdServerData.hostname, mpdServerData.port), (int) timeoutMs);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), Charset.forName("UTF-8")));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), Charset.forName("UTF-8")));

            onConnected(reader, writer);

            reader.close();
            writer.close();
            s.close();
        } catch (IOException ex) {
            fail();
        }
    }

    private void onConnected(@NonNull BufferedReader reader, @NonNull BufferedWriter writer) throws IOException {
        CharBuffer readBuffer = CharBuffer.allocate(1024);
        boolean c = true;
        while (c) {
            readBuffer.clear();

            ReadStage readStage = MPDAsyncTask.this.readStages.poll();
            if (readStage != null) {
                int read = reader.read(readBuffer);
                readBuffer.position(0);
                Log.d(TAG, readBuffer.toString());
                c = readStage.onRead(MPDAsyncTask.this, readBuffer.toString());
            } else {
                c = false;
            }

            if (c) {
                WriteStage writeStage = MPDAsyncTask.this.writeStages.poll();
                if (writeStage != null) {
                    c = writeStage.onWrite(MPDAsyncTask.this, writer);
                    writer.flush();
                } else {
                    c = false;
                }
            }
        }
    }


    public void setParams(@NonNull MPDClient mpdClient, @NonNull MPDServerData mpdServerData) {
        this.mpdClient = mpdClient;
        this.mpdServerData = new MPDServerData(mpdServerData);
    }

    public MPDServerData getMpdServerData() {
        return mpdServerData;
    }

    public void notifyServerUpdated() {
        mpdClient.notifyServerUpdate(mpdServerData);
    }

    protected static MPDAsyncTask.ReadStage okReadStage() {
        return (task, result) -> {
            boolean ok = result.startsWith("OK");
            if (!ok) {
                task.fail();
            }

            return ok;
        };
    }

    protected static MPDAsyncTask.WriteStage statusWriteStage() {
        return (task, bufferedWriter) -> {
            bufferedWriter.write("status\n");
            return true;
        };
    }

    protected static MPDAsyncTask.WriteStage loginWriteStage(String password) {
        return (task, bufferedWriter) -> {
            bufferedWriter.write("password " + password + "\n");
            return true;
        };
    }

    protected static MPDAsyncTask.ReadStage statusReadStage(boolean c) {
        return (task, result) -> {
            task.getMpdServerData().updateStatus(result);
            task.notifyServerUpdated();
            return c;
        };
    }
}
