package com.challenge.rental_cars_spring_api.performance;

public class MemoryMonitor extends Thread {
    private volatile boolean running = true;
    private long maxMemoryUsed = 0;
    private long initialMemoryUsed = 0; // Variável para armazenar a memória inicial
    private long checkInterval = 50; // ms

    @Override
    public synchronized void start() {
        super.start(); // Inicia a thread
        this.initialMemoryUsed = getCurrentMemoryMB(); // Captura a memória usada no momento em que o monitor é iniciado
    }

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

    public long getCurrentMemoryUsed() {
        return getCurrentMemoryMB();
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

    public long getInitialMemoryUsed() {
        return initialMemoryUsed;
    }

    public void setCheckInterval(long interval) {
        this.checkInterval = interval;
    }
}