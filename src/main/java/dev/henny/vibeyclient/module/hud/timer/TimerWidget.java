package dev.henny.vibeyclient.module.hud.timer;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.TextWidget;

import java.util.List;

public final class TimerWidget extends TextWidget {
    static final String MODE = "mode", LENGTH = "length";
    public static final String[] LENGTHS = TimerFormat.LENGTHS;

    final TimerClock clock = new TimerClock();
    private HudElement self;
    private String mode = "stopwatch";
    private long length = 60_000L;

    public TimerWidget() {
        Timers.add(this);
    }

    @Override
    protected int rate() {
        return clock.running() ? 20 : 4;
    }

    @Override
    protected String icon(HudPaint p) {
        return "hud_clock";
    }

    @Override
    protected void lines(HudPaint p) {
        self = p.element();
        mode = p.option(MODE);
        length = TimerFormat.lengthMs(p.option(LENGTH));
        long now = System.currentTimeMillis();
        switch (mode) {
            case "countdown" -> countdown(now);
            case "splits" -> splits(now);
            default -> line(null, TimerFormat.precise(clock.elapsed(now)));
        }
    }

    private void countdown(long now) {
        if (clock.finishOnce(now, length)) TimerSound.ding();
        boolean blink = clock.finished(now, length) && (now / 400) % 2 == 0;
        line(null, blink ? " " : TimerFormat.clock(clock.left(now, length)));
    }

    private void splits(long now) {
        line(null, TimerFormat.precise(clock.elapsed(now)));
        List<Long> s = clock.splits();
        long prev = s.size() > 3 ? s.get(s.size() - 4) : 0;
        for (int i = Math.max(0, s.size() - 3); i < s.size(); i++) {
            line("#" + (i + 1), TimerFormat.precise(s.get(i) - prev));
            prev = s.get(i);
        }
    }

    void toggle(long now) {
        if ("countdown".equals(mode) && clock.finished(now, length)) clock.reset();
        clock.toggle(now);
        invalidate();
    }

    HudElement element() {
        return self;
    }

    String mode() {
        return mode;
    }

    @Override
    public boolean wheel(HudElement e, int dir) {
        if (!"countdown".equals(HudLayout.placement(e).opts.getOrDefault(MODE, "stopwatch"))) return false;
        String cur = HudLayout.placement(e).opts.getOrDefault(LENGTH, "1m");
        int i = java.util.Arrays.asList(TimerFormat.LENGTHS).indexOf(cur);
        int next = Math.max(0, Math.min(TimerFormat.LENGTHS.length - 1, (i < 0 ? 1 : i) + dir));
        HudLayout.setOption(e, LENGTH, TimerFormat.LENGTHS[next]);
        if (!clock.running()) clock.reset();
        HudLayout.save();
        return true;
    }
}
