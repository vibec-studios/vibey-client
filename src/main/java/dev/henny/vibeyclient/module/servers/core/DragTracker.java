package dev.henny.vibeyclient.module.servers.core;

public final class DragTracker {
    public static final double THRESHOLD = 4;

    public enum Phase { IDLE, PRESSED, DRAGGING }

    private Phase phase = Phase.IDLE;
    private int from = -1;
    private double startX, startY, x, y;

    public void press(int index, double px, double py) {
        phase = Phase.PRESSED;
        from = index;
        startX = x = px;
        startY = y = py;
    }

    public boolean move(double px, double py) {
        x = px;
        y = py;
        if (phase != Phase.PRESSED) return false;
        if (Math.hypot(px - startX, py - startY) < THRESHOLD) return false;
        phase = Phase.DRAGGING;
        return true;
    }

    public int release() {
        int r = phase == Phase.DRAGGING ? from : -1;
        cancel();
        return r;
    }

    public void cancel() {
        phase = Phase.IDLE;
        from = -1;
    }

    public Phase phase() {
        return phase;
    }

    public boolean dragging() {
        return phase == Phase.DRAGGING;
    }

    public boolean pressed() {
        return phase != Phase.IDLE;
    }

    public int from() {
        return from;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double startY() {
        return startY;
    }
}
