package com.challenge.rental_cars_spring_api.utils;

public class MemoryMonitor extends Thread {
    private volatile boolean running = true;
    private long maxMemoryUsed = 0;
    private long checkInterval = 50; // ms

    @Override
    public void run() {
        while (running) {
            long usedMemory = getCurrentMemoryMB();
            if (usedMemory > maxMemoryUsed) {
                maxMemoryUsed = usedMemory;
            }
            sleepSafe();
        }
    }

    private long getCurrentMemoryMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
    }

    private void sleepSafe() {
        try {
            Thread.sleep(checkInterval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stopMonitoring() {
        running = false;
    }

    public long getMaxMemoryUsed() {
        return maxMemoryUsed;
    }

    public void setCheckInterval(long interval) {
        this.checkInterval = interval;
    }
}