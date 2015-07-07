package com.joanzapata.tilesview.internal;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ReverseExecutor {

    private ThreadPoolExecutor threadPoolExecutor;
    private BlockingDeque<Runnable> workQueue;

    public ReverseExecutor(int nbThreads) {
        workQueue = new LinkedBlockingDeque<Runnable>() {
            @Override
            public boolean add(Runnable runnable) {
                addFirst(runnable);
                return true;
            }
        };
        threadPoolExecutor = new ThreadPoolExecutor(nbThreads, nbThreads, 0, TimeUnit.SECONDS, workQueue);
    }

    public void submit(Runnable runnable) {
        workQueue.addFirst(runnable);
        threadPoolExecutor.submit(runnable);
    }

    public void shutdownNow() {
        threadPoolExecutor.shutdown();
    }
}
