package com.joanzapata.tilesview.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LIFOExecutor {

    private ThreadPoolExecutor threadPoolExecutor;

    private BlockingDeque<Runnable> workQueue;

    private Map<Runnable, Cancellable> cancellables;

    private int capacity;

    public LIFOExecutor(int nbThreads) {
        cancellables = new HashMap<Runnable, Cancellable>();
        workQueue = new LinkedBlockingDeque<Runnable>() {
            @Override
            public boolean offer(Runnable runnable) {
                while (size() >= capacity) {
                    Runnable futureTask = pollLast();
                    Cancellable cancellable = cancellables.get(futureTask);
                    if (cancellable != null) {
                        cancellable.cancel();
                    } else {
                        submit(futureTask);
                    }
                }
                return offerFirst(runnable);
            }
        };
        threadPoolExecutor = new ThreadPoolExecutor(nbThreads, nbThreads, 0, TimeUnit.SECONDS, workQueue);
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void submit(Runnable runnable) {
        Runnable futureTask = (Runnable) threadPoolExecutor.submit(runnable);
        if (runnable instanceof Cancellable)
            cancellables.put(futureTask, (Cancellable) runnable);
    }

    public void shutdownNow() {
        threadPoolExecutor.shutdownNow();
    }

    public interface Cancellable {
        void cancel();
    }
}
