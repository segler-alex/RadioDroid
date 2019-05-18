package net.programmierecke.radiodroid2.utils;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

/**
 * Periodical refreshes which allows the object it refreshes be garbage collected
 */
public final class RefreshHandler {
    private final Handler handler;
    private RunnableDecorator runnableDecorator;

    public RefreshHandler() {
        handler = new Handler(Looper.getMainLooper());
    }

    public final void executePeriodically(final ObjectBoundRunnable task, final long interval) {
        if (runnableDecorator != null) {
            handler.removeCallbacks(runnableDecorator);
        }

        runnableDecorator = new RunnableDecorator(task, interval);
        handler.post(runnableDecorator);
    }

    public final void cancel() {
        if (runnableDecorator != null) {
            handler.removeCallbacks(runnableDecorator);
        }

        runnableDecorator = null;
    }

    private class RunnableDecorator implements Runnable {
        private final ObjectBoundRunnable runnable;
        private final long interval;

        RunnableDecorator(ObjectBoundRunnable runnable, long interval) {
            this.runnable = runnable;
            this.interval = interval;
        }

        @Override
        public void run() {
            runnable.run();
            if (runnable.objectRef.get() != null && !runnable.terminate) {
                handler.postDelayed(this, interval);
            } else {
                handler.removeCallbacks(this);
                runnableDecorator = null;
            }
        }
    }

    public static abstract class ObjectBoundRunnable<T> implements Runnable {
        private final WeakReference<T> objectRef;
        private boolean terminate = false;

        public ObjectBoundRunnable(T obj) {
            objectRef = new WeakReference<>(obj);
        }

        @Override
        public void run() {
            T obj = objectRef.get();
            if (obj != null) {
                run(obj);
            }
        }

        protected final void terminate() {
            terminate = true;
        }

        protected abstract void run(T obj);
    }
}
