package dev.henny.vibeyclient.module.waypoints.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import dev.henny.vibeyclient.module.waypoints.core.Colors;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.core.WaypointList;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class ListController {
    static final long REFRESH_MS = 500;

    public final ListState<WaypointRow> rows = new ListState<>();
    public final State<String> selId = new State<>("");
    public final State<WaypointRow> sel = new State<>(null);
    public final State<String> editName = new State<>("");
    public final List<String> palette = new ArrayList<>();

    private final WaypointsRuntime rt;
    private int revision = -1;
    private long nextRefresh;
    private String navId;

    ListController(WaypointsRuntime rt, String selectId) {
        this.rt = rt;
        for (int c : Colors.PALETTE) palette.add("#" + Colors.hex(c));
        poll();
        if (selectId != null) select(row(selectId));
    }

    public String t(String key) {
        return Lang.tr(key);
    }

    void poll() {
        WaypointList list = rt.store().list();
        long now = System.currentTimeMillis();
        String nav = rt.navigator().targetId();
        if (list.revision() == revision && now < nextRefresh && java.util.Objects.equals(nav, navId)) return;
        revision = list.revision();
        nextRefresh = now + REFRESH_MS;
        navId = nav;
        Minecraft mc = Minecraft.getInstance();
        String dim = mc.level == null ? null : WorldContext.dimension(mc.level);
        List<WaypointRow> next = new ArrayList<>();
        for (Waypoint w : list.all()) next.add(new WaypointRow(w, mc.player, dim, nav));
        rows.replaceAll(next);
        WaypointRow current = row(selId.peek());
        sel.set(current);
        if (current == null) selId.set("");
    }

    private WaypointRow row(String id) {
        if (id == null || id.isEmpty()) return null;
        for (WaypointRow r : rows.snapshot()) if (r.id.equals(id)) return r;
        return null;
    }

    private Waypoint waypoint(WaypointRow r) {
        return r == null ? null : rt.store().list().get(r.id);
    }

    private void refreshNow() {
        nextRefresh = 0;
        poll();
    }

    public void select(WaypointRow r) {
        if (r == null) return;
        selId.set(r.id);
        sel.set(r);
        editName.set(r.name);
    }

    public void add() {
        Waypoint w = rt.actions().addHere();
        refreshNow();
        if (w != null) select(row(w.id()));
    }

    public void importCode() {
        rt.actions().importClipboard();
        refreshNow();
    }

    public void shareAll() {
        rt.actions().copy(rt.store().list().all());
    }

    public void share(WaypointRow r) {
        Waypoint w = waypoint(r);
        if (w != null) rt.actions().copy(List.of(w));
    }

    public void remove(WaypointRow r) {
        if (r == null) return;
        rt.actions().remove(r.id);
        refreshNow();
    }

    public void toggle(WaypointRow r) {
        Waypoint w = waypoint(r);
        if (w != null) rt.actions().toggleVisible(w);
        refreshNow();
    }

    public void navigate(WaypointRow r) {
        Waypoint w = waypoint(r);
        if (w != null) rt.actions().toggleNavigate(w);
        refreshNow();
    }

    public void rename() {
        Waypoint w = waypoint(sel.peek());
        if (w == null) return;
        rt.actions().rename(w, editName.peek());
        refreshNow();
    }

    public void pickColor(String hex) {
        Waypoint w = waypoint(sel.peek());
        if (w == null) return;
        rt.actions().color(w, Colors.parseHex(hex));
        refreshNow();
    }

    public void openMap() {
        Screens.map();
    }

    public void close() {
        Screens.closeCurrent();
    }
}
