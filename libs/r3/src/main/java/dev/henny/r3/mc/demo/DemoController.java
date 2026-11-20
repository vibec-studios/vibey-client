package dev.henny.r3.mc.demo;

public final class DemoController {
    public String title = "r3 demo";
    public String status = "ready";
    private int plays;
    private int nexts;
    private int stops;

    public void play() {
        plays++;
        status = "playing #" + plays;
    }

    public void next() {
        nexts++;
        status = "next #" + nexts;
    }

    public void stop() {
        stops++;
        status = "stopped";
    }

    public int plays() {
        return plays;
    }

    public int nexts() {
        return nexts;
    }

    public int stops() {
        return stops;
    }
}
