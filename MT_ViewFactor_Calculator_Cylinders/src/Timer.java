// Tracks how long the program has been running

import java.lang.System;

public class Timer {

    private long startTime;

    public void start() {
        startTime = System.nanoTime();
    }

    public double stop() {
        return (double)(System.nanoTime() - startTime) / Math.pow(10, 9);
    }
}