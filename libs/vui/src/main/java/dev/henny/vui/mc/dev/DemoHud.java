package dev.henny.vui.mc.dev;

import dev.henny.vui.bind.Model;
import dev.henny.vui.hud.HudPlacement;
import dev.henny.vui.layout.Anchor;
import dev.henny.vui.mc.compat.Mc;
import dev.henny.vui.mc.host.HudHost;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

final class DemoHud {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Model fps = new Model().value("fps", 0);
    private final Model clock = new Model().value("clock", "");

    static DemoHud install(HudHost hud) {
        DemoHud demo = new DemoHud();
        demo.tick();
        hud.add("vui:fps", "vui:hud/fps", demo.fps, HudPlacement.at(Anchor.TOP_LEFT, 4, 4));
        hud.add("vui:clock", "vui:hud/clock", demo.clock, HudPlacement.at(Anchor.TOP_RIGHT, -4, 4));
        return demo;
    }

    void tick() {
        fps.value("fps", Mc.fps());
        clock.value("clock", LocalTime.now().format(CLOCK));
    }
}
