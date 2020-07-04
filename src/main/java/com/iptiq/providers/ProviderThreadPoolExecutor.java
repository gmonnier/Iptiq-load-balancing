package com.iptiq.providers;

import java.util.concurrent.*;

class ProviderThreadPoolExecutor extends ThreadPoolExecutor {

    private TaskExecutionListener taskListener;

    public ProviderThreadPoolExecutor(int poolSize, TaskExecutionListener taskListener) {
        super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        this.taskListener = taskListener;
    }

    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future<?>) {
            try {
                ((Future<?>) r).get();
            } catch (CancellationException ce) {
                t = ce;
            } catch (ExecutionException ee) {
                t = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            taskListener.taskCompleted();
        }
        if (t != null)
            t.printStackTrace();
    }
}