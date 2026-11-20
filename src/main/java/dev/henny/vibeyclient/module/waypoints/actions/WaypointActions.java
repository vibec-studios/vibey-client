package dev.henny.vibeyclient.module.waypoints.actions;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import dev.henny.vibeyclient.module.waypoints.core.Colors;
import dev.henny.vibeyclient.module.waypoints.core.Names;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.ShareCode;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.core.WaypointList;
import dev.henny.vibeyclient.module.waypoints.data.WaypointStore;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class WaypointActions {
    private final WaypointsRuntime runtime;

    public WaypointActions(WaypointsRuntime runtime) {
        this.runtime = runtime;
    }

    private WaypointStore store() {
        return runtime.store();
    }

    private WaypointList list() {
        return store().list();
    }

    public Waypoint addHere() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !store().loaded()) return null;
        Waypoint w = add(WorldContext.feet(mc.player), WorldContext.dimension(mc.level));
        if (w != null) Notify.success(w.name(), Lang.tr("waypoints.toast.added"));
        return w;
    }

    public Waypoint add(Pos pos, String dimension) {
        if (!store().loaded() || dimension == null) return null;
        Waypoint w = new Waypoint(list().newId(), Names.next(Lang.tr("waypoints.default"), list().all()), pos, dimension,
                Colors.next(list().normalCount())).created(System.currentTimeMillis());
        if (!list().add(w)) return null;
        store().save();
        return w;
    }

    public void remove(String id) {
        if (list().remove(id) == null) return;
        if (id.equals(runtime.navigator().targetId())) runtime.navigator().clear();
        store().save();
    }

    public void rename(Waypoint w, String name) {
        String clean = Names.clean(name);
        if (clean.isEmpty() || clean.equals(w.name())) return;
        w.name(clean);
        store().save();
    }

    public void color(Waypoint w, int rgb) {
        w.color(rgb);
        store().save();
    }

    public void toggleVisible(Waypoint w) {
        w.visible(!w.visible());
        store().changed();
    }

    public void edited(Waypoint w) {
        store().save();
    }

    public void toggleNavigate(Waypoint w) {
        Navigator nav = runtime.navigator();
        if (w.id().equals(nav.targetId())) nav.clear();
        else nav.target(w.id());
    }

    public void copy(List<Waypoint> list) {
        if (list.isEmpty()) return;
        ClientCompat.setClipboard(ShareCode.encode(list));
        Notify.info(list.size() == 1 ? list.get(0).name() : String.valueOf(list.size()), Lang.tr("waypoints.toast.copied"));
    }

    public int importClipboard() {
        String text = ClientCompat.clipboard();
        try {
            List<Waypoint> in = ShareCode.decode(text, list().ids());
            int added = 0;
            for (Waypoint w : in) if (list().add(w.created(System.currentTimeMillis()))) added++;
            store().save();
            Notify.success(String.valueOf(added), Lang.tr("waypoints.toast.imported"));
            return added;
        } catch (ShareCode.Invalid e) {
            Notify.warn(Lang.tr("waypoints.toast.bad"), "");
            return 0;
        }
    }

    public void markDeath(Pos pos, String dimension) {
        if (!store().loaded()) return;
        list().markDeath(Lang.tr("waypoints.death"), pos, dimension, System.currentTimeMillis());
        store().save();
    }
}
