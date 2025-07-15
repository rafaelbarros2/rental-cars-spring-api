package com.challenge.rental_cars_spring_api.utils;

public class PerformanceRecorder {
    private long startTime;
    private long endTime;

    public void start() {
        startTime = System.nanoTime();
    }

    public void stop() {
        endTime = System.nanoTime();
    }

    public long getDurationMillis() {
        return (endTime - startTime) / 1_000_000;
    }

    public void recordStats(String testName, int linesProcessed, long memoryUsedMB) {
        System.out.println("\n--- Performance Report ---");
        System.out.println("Test: " + testName);
        System.out.println("Lines: " + linesProcessed);
        System.out.println("Time: " + getDurationMillis() + " ms");
        System.out.println("Memory: " + memoryUsedMB + " MB");
        System.out.println("Throughput: " + calculateThroughput(linesProcessed) + " lines/s");
        System.out.println("-------------------------");
    }

    private double calculateThroughput(int lines) {
        double seconds = getDurationMillis() / 1000.0;
        return seconds > 0 ? lines / seconds : 0;
    }
}