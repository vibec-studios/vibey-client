package dev.henny.companio.mc.screen;

import dev.henny.companio.brain.BehaviorRun;
import dev.henny.companio.mc.client.CompanioClient;
import dev.henny.companio.pack.Pack;
import dev.henny.companio.pack.Problem;
import dev.henny.companio.world.Companion;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.dom.Document;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Overlay {
    private static final int MAX_LINES = 6, SHOW_TICKS = 240;

    public final ListState<PackRow.ProblemLine> lines = new ListState<>();
    public final ListState<String> debugLines = new ListState<>();
    public final State<String> debug = new State<>("");
    public final State<String> title = new State<>("");
    private final CompanioClient client;
    private Document doc;
    private int hideIn;
    private boolean debugOn;

    public Overlay(CompanioClient client) {
        this.client = client;
        client.onPacksLoaded(this::loaded);
    }

    private void loaded(List<Pack> packs) {
        List<PackRow.ProblemLine> out = new ArrayList<>();
        int errors = 0, i = 0;
        for (Pack p : packs) {
            if (p.builtin() && p.errors() == 0) continue;
            for (Problem pr : p.problems()) {
                if (!pr.error()) continue;
                errors++;
                if (out.size() < MAX_LINES) out.add(new PackRow.ProblemLine("e" + i++, name(p) + " · " + pr.format(), true));
            }
        }
        lines.replaceAll(out);
        title.set(I18n.get("companio.errors.title", errors));
        if (errors > 0) {
            hideIn = SHOW_TICKS;
            ensure();
        }
    }

    private static String name(Pack p) {
        return p.id() != null ? p.id() : p.location().replace('\\', '/').replaceAll(".*/", "");
    }

    public void toggleDebug() {
        debugOn = !debugOn;
        if (!debugOn) debug.set("");
        ensure();
    }

    public boolean showing() {
        return doc != null && !lines.snapshot().isEmpty();
    }

    public void tick() {
        if (hideIn > 0 && --hideIn == 0) lines.replaceAll(List.of());
        if (debugOn) debug(client.own());
        if (doc != null && lines.snapshot().isEmpty() && !debugOn) {
            VuiMc.overlays().close(doc);
            doc = null;
        }
    }

    private void ensure() {
        if (doc == null) doc = VuiMc.overlays().show("companio:errors", this);
    }

    private void debug(Companion c) {
        List<String> l = new ArrayList<>();
        if (c == null) {
            l.add("no companion");
        } else {
            BehaviorRun r = c.brain().current();
            l.add(c.def.key() + " · " + c.mode().id() + " · " + c.loco().id());
            l.add("behavior: " + (r == null ? "-" : r.def.id() + String.format(Locale.ROOT, " (%.1fs)", r.time(c.now()))));
            c.brain().needs().snapshot().forEach((k, v) -> l.add(String.format(Locale.ROOT, "%s %.2f", k, v)));
            l.add("events: " + String.join(", ", c.brain().recentEvents()));
        }
        debugLines.replaceAll(l);
        debug.set("on");
    }
}
