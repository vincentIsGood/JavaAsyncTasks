package com.vincentcodes.async;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EventLoop {
    public static final EventLoop EVENT_LOOP = new EventLoop();

    // private final ExecutorService executorService;
    private final ThreadPoolExecutor executorService;

    public EventLoop(){
        executorService = (ThreadPoolExecutor)Executors.newFixedThreadPool(2);
    }

    public Future<?> submit(Runnable task){
        return executorService.submit(task);
    }

    public int getActiveThreadCount(){
        return executorService.getActiveCount();
    }

    public void stop(){
        // Disable new tasks from being submitted
        executorService.shutdown();
        try {
          // Wait a while for existing tasks to terminate
          if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                System.err.println("executorService did not terminate");
          }
        } catch (InterruptedException ignored) {
          executorService.shutdownNow();
          Thread.currentThread().interrupt();
        }
    }
}