package dev.henny.companio.mc.screen;

import dev.henny.companio.config.CompanioConfig;
import dev.henny.companio.mc.client.CompanioClient;
import dev.henny.companio.pack.Pack;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

public final class PacksController {
    public final ListState<PackRow> available = new ListState<>();
    public final ListState<PackRow> active = new ListState<>();
    public final ListState<PackRow.ProblemLine> problems = new ListState<>();
    public final State<PackRow> sel = new State<>(null);
    public final State<String> selKey = new State<>("");
    public final State<String> selected = new State<>("");
    public final State<Boolean> loading = new State<>(false);
    private final CompanioClient client;
    private Runnable close = () -> {};

    public PacksController(CompanioClient client) {
        this.client = client;
        client.screenListener(this::rebuild);
        rebuild();
    }

    void onClose(Runnable r) {
        close = r;
    }

    public void rebuild() {
        CompanioConfig c = client.config();
        List<PackRow> on = new ArrayList<>(), off = new ArrayList<>();
        for (String id : c.active()) {
            Pack p = client.registry().pack(id);
            if (p != null) on.add(PackRows.of(p, true));
        }
        for (Pack p : client.registry().all()) {
            if (p.id() == null || !c.active().contains(p.id())) off.add(PackRows.of(p, false));
        }
        available.replaceAll(off);
        active.replaceAll(on);
        selected.set(c.selected);
        loading.set(client.repository().loading());
        PackRow s = find(selKey.peek());
        if (s == null && !on.isEmpty()) s = on.get(0);
        show(s);
    }

    private PackRow find(String key) {
        for (PackRow r : active.snapshot()) if (r.key().equals(key)) return r;
        for (PackRow r : available.snapshot()) if (r.key().equals(key)) return r;
        return null;
    }

    private void show(PackRow r) {
        sel.set(r);
        selKey.set(r == null ? "" : r.key());
        problems.replaceAll(r == null ? List.of() : r.problems());
    }

    public void select(PackRow r) {
        show(r);
    }

    public void toggle(PackRow r) {
        List<String> ids = client.config().editActive();
        if (r.isActive()) ids.remove(r.id());
        else if (r.loaded() && !r.id().isEmpty() && !ids.contains(r.id())) ids.add(r.id());
        client.changed();
        selKey.set(r.key());
        rebuild();
    }

    public void up(PackRow r) {
        move(r, -1);
    }

    public void down(PackRow r) {
        move(r, 1);
    }

    private void move(PackRow r, int d) {
        List<String> ids = client.config().editActive();
        int i = ids.indexOf(r.id()), j = i + d;
        if (i < 0 || j < 0 || j >= ids.size()) return;
        ids.set(i, ids.get(j));
        ids.set(j, r.id());
        client.changed();
        rebuild();
    }

    public void pick(PackRow.CompanionChip c) {
        client.config().selected = c.key();
        client.config().variant = "";
        client.config().size = 0;
        client.config().enabled = true;
        client.changed();
        selected.set(c.key());
    }

    public void openFolder() {
        Util.getPlatform().openPath(client.repository().folder());
    }

    public void reload() {
        loading.set(true);
        client.repository().reload();
    }

    public void done() {
        client.screenListener(null);
        close.run();
    }

    public static void open() {
        CompanioClient client = CompanioClient.get();
        if (client == null) return;
        PacksController c = new PacksController(client);
        Minecraft mc = Minecraft.getInstance();
        var screen = dev.henny.vui.mc.VuiMc.screen("companio:packs", c).parent(mc.screen).pause(false);
        c.onClose(screen::onClose);
        screen.open();
    }
}
