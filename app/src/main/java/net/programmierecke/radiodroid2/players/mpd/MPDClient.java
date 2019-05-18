package net.programmierecke.radiodroid2.players.mpd;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MPDClient {
    private static final String TAG = "MPDClient";

    private static int QUICK_REFRESH_TIMEOUT = 150;
    private static int ALIVE_REFRESH_TIMEOUT = 1000;
    private static int DEAD_REFRESH_TIMEOUT = 1000;

    private ScheduledExecutorService userTaskThreadPool;
    private ScheduledExecutorService connectionCheckerThreadPool;

    private MPDServersRepository mpdServersRepository;
    private LiveData<List<MPDServerData>> mpdServers;

    private Handler mainThreadHandler;

    // We queue changes to apply them in main thread instead of worrying about concurrent access to
    // the repository
    private ConcurrentLinkedQueue<MPDServerData> serverChangesQueue = new ConcurrentLinkedQueue<>();

    private Set<MPDServerData> aliveMpdServers = new HashSet<>();
    private Set<MPDServerData> deadMpdServers = new HashSet<>();
    private final Object serversLock = new Object();

    // The idea is to have quick frequent separate update of alive servers. And other rarer
    // update of presumable unavailable servers.
    private QuickMPDStatusChecker quickMPDStatusChecker = new QuickMPDStatusChecker();
    private Future quickCheckFuture;
    private final Object quickFutureLock = new Object();

    private AliveMPDStatusChecker aliveMPDStatusChecker = new AliveMPDStatusChecker();
    private Future aliveCheckFuture;
    private final Object aliveFutureLock = new Object();

    private DeadMPDStatusChecker deadMPDStatusChecker = new DeadMPDStatusChecker();
    private Future deadCheckFuture;
    private final Object deadFutureLock = new Object();

    private boolean mpdEnabled = false;

    private boolean autoUpdateEnabled = false;

    public MPDClient(Context context) {
        mpdServersRepository = new MPDServersRepository(context);
        mpdServers = mpdServersRepository.getAllServers();

        mainThreadHandler = new Handler(context.getMainLooper());
    }

    public MPDServersRepository getMpdServersRepository() {
        return mpdServersRepository;
    }

    public void enqueueTask(@NonNull MPDServerData server, @NonNull final MPDAsyncTask task) {
        if (!mpdEnabled) {
            Log.e(TAG, "Trying to enqueue task when mpd is not enabled!");
            return;
        }

        task.setTimeout(getTimeout(server.hostname));
        task.setParams(this, server);

        userTaskThreadPool.submit(task);
    }

    public void enableAutoUpdate() {
        if (!mpdEnabled) {
            setMPDEnabled(true);
            Log.w(TAG, "enableAutoUpdate called with mpd disabled, enabling mpd");
        }

        autoUpdateEnabled = true;

        mpdServersRepository.resetAllConnectionStatus();

        // First submit a quick check in optimistic prediction that alive servers would quickly
        // reply and user will quickly see servers status.
        synchronized (quickFutureLock) {
            quickMPDStatusChecker.setServers(new ArrayList<>(mpdServers.getValue()));
            quickCheckFuture = connectionCheckerThreadPool.submit(quickMPDStatusChecker);
        }
    }

    public void disableAutoUpdate() {
        autoUpdateEnabled = false;

        cancelCheckFutures();
    }

    public void launchQuickCheck() {
        if (!autoUpdateEnabled) {
            Log.e(TAG, "Trying to launch quick servers check while autoUpdateEnabled = false!");
            return;
        }

        cancelCheckFutures();

        synchronized (quickFutureLock) {
            quickMPDStatusChecker.setServers(new ArrayList<>(mpdServers.getValue()));
            quickCheckFuture = connectionCheckerThreadPool.submit(quickMPDStatusChecker);
        }
    }

    private void cancelCheckFutures() {
        synchronized (quickFutureLock) {
            if (quickCheckFuture != null) {
                quickCheckFuture.cancel(true);
                quickCheckFuture = null;
            }
        }

        synchronized (aliveFutureLock) {
            if (aliveCheckFuture != null) {
                aliveCheckFuture.cancel(true);
                aliveCheckFuture = null;
            }
        }

        synchronized (deadFutureLock) {
            if (deadCheckFuture != null) {
                deadCheckFuture.cancel(true);
                deadCheckFuture = null;
            }
        }

        aliveMpdServers.clear();
        deadMpdServers.clear();
    }

    public boolean isMpdEnabled() {
        return mpdEnabled;
    }

    public void setMPDEnabled(boolean enabled) {
        if (enabled != mpdEnabled) {
            if (enabled) {
                enableThreadPools();
            } else {
                disableAutoUpdate();
            }

            mpdEnabled = enabled;
        }
    }

    void notifyServerUpdate(@NonNull MPDServerData mpdServerData) {
        serverChangesQueue.add(mpdServerData);

        mainThreadHandler.post(() -> {
            MPDServerData changedData;
            while ((changedData = serverChangesQueue.poll()) != null) {
                mpdServersRepository.updateRuntimeData(changedData);
            }
        });
    }

    private void enableThreadPools() {
        if (userTaskThreadPool == null) {
            userTaskThreadPool = Executors.newScheduledThreadPool(1);
        }

        if (connectionCheckerThreadPool == null) {
            // One for alive and one for dead
            connectionCheckerThreadPool = Executors.newScheduledThreadPool(2);
        }
    }

    private static int getTimeout(String hostname) {
        return hostname.startsWith("192.168.")
                || hostname.startsWith("127.0.")
                || hostname.startsWith("localhost")
                || hostname.startsWith("10.")
                || hostname.contains(".local") ? 300
                : 2 * 1000;

    }

    private void checkServers(Iterable<MPDServerData> servers, Function<MPDServerData, Integer> timeoutFunc) {
        for (final MPDServerData mpdServerData : servers) {
            MPDAsyncTask task = new MPDAsyncTask();
            task.setStages(
                    new MPDAsyncTask.ReadStage[]{
                            MPDAsyncTask.okReadStage(),
                            MPDAsyncTask.statusReadStage(false),
                    },
                    new MPDAsyncTask.WriteStage[]{
                            MPDAsyncTask.statusWriteStage()},
                    task13 -> {
                        if (task13.getMpdServerData().connected) {
                            task13.getMpdServerData().connected = false;
                            task13.notifyServerUpdated();
                        }
                    }
            );

            task.setTimeout(timeoutFunc.apply(mpdServerData));
            task.setParams(this, mpdServerData);

            task.run();

            synchronized (serversLock) {
                if (mpdServerData.connected) {
                    aliveMpdServers.add(mpdServerData);
                    deadMpdServers.remove(mpdServerData);
                } else {
                    aliveMpdServers.remove(mpdServerData);
                    deadMpdServers.add(mpdServerData);
                }
            }
        }
    }

    private class QuickMPDStatusChecker implements Runnable {
        private List<MPDServerData> servers;

        public void setServers(List<MPDServerData> servers) {
            this.servers = servers;
        }

        @Override
        public void run() {
            checkServers(servers, (MPDServerData server) -> QUICK_REFRESH_TIMEOUT);

            synchronized (aliveFutureLock) {
                aliveCheckFuture = connectionCheckerThreadPool.schedule(aliveMPDStatusChecker, 2, TimeUnit.SECONDS);
            }

            synchronized (deadFutureLock) {
                deadCheckFuture = connectionCheckerThreadPool.schedule(deadMPDStatusChecker, 0, TimeUnit.SECONDS);
            }
        }
    }

    private class AliveMPDStatusChecker implements Runnable {

        @Override
        public void run() {
            Collection<MPDServerData> aliveServers;
            synchronized (serversLock) {
                aliveServers = new ArrayList<>(aliveMpdServers);
            }

            checkServers(aliveServers, (MPDServerData server) -> ALIVE_REFRESH_TIMEOUT);

            if (autoUpdateEnabled) {
                synchronized (aliveFutureLock) {
                    aliveCheckFuture = connectionCheckerThreadPool.schedule(aliveMPDStatusChecker, 2, TimeUnit.SECONDS);
                }
            }
        }
    }

    private class DeadMPDStatusChecker implements Runnable {

        @Override
        public void run() {
            Collection<MPDServerData> deadServers;
            synchronized (serversLock) {
                deadServers = new ArrayList<>(deadMpdServers);
            }

            checkServers(deadServers, (MPDServerData server) -> DEAD_REFRESH_TIMEOUT);

            if (autoUpdateEnabled) {
                synchronized (deadFutureLock) {
                    deadCheckFuture = connectionCheckerThreadPool.schedule(deadMPDStatusChecker, 8, TimeUnit.SECONDS);
                }
            }
        }
    }
}
