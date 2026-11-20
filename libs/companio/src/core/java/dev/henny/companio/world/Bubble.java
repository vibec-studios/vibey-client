package dev.henny.companio.world;

public final class Bubble {
    private String text = "";
    private double until, since;

    public void show(String text, double now, float seconds) {
        this.text = text == null ? "" : text;
        this.since = now;
        this.until = now + seconds;
    }

    public String text(double now) {
        return now < until ? text : "";
    }

    public double age(double now) {
        return now - since;
    }

    public double left(double now) {
        return until - now;
    }

    public void clear() {
        until = 0;
    }
}
