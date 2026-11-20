package dev.henny.companio.mc.interact;

import dev.henny.companio.world.Companion;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.interact.InteractionState;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.mc.client.R3Client;
import dev.henny.r3.object.WorldObject;
import org.joml.Vector3d;

import java.util.function.Predicate;

public final class CompanionTarget {
    public static final String KIND = "companio:companion";

    private WorldObject object;
    private Companion companion;
    private FollowAnchor anchor;

    public void update(Companion c, Predicate<double[]> renderPosition) {
        if (!R3Client.started()) return;
        if (c != companion) remove();
        companion = c;
        if (c == null) return;
        if (object == null) {
            anchor = new FollowAnchor(p -> {
                if (!renderPosition.test(p)) return false;
                p[1] += c.body.height * 0.5;
                return true;
            });
            object = WorldObject.create(KIND).content(new TargetContent()).anchoredTo(anchor)
                    .rules(new InteractionRules().snapToBlocks(false).keepAspect(true).doubleSided(true).turnToViewer(true)
                            .distanceAdjustable(true));
            object.dimension(R3Mc.scene().query().dimension());
            object.visibility().maxDistance(24).fadeDistance(0);
            R3Mc.add(object);
        }
        float w = Math.max(0.35f, c.body.width * 1.15f), h = Math.max(0.35f, c.body.height * 1.05f);
        if (!holding() && (Math.abs(object.transform().width() - w) > 1e-3 || Math.abs(object.transform().height() - h) > 1e-3 || object.transform().scale() != 1f)) {
            object.transform().size(w, h).scale(1f);
        }
        if (c.held() && holding()) {
            Vector3d p = object.transform().position();
            c.holdAt(p.x, p.y - c.body.height * 0.5, p.z);
        }
    }

    public void reanchor() {
        if (object != null && anchor != null) object.anchor(anchor);
    }

    public boolean holding() {
        return object != null && R3Mc.scene().interaction().state() == InteractionState.HOLDING;
    }

    public boolean hovered() {
        return object != null && R3Client.started() && R3Mc.scene().interaction().hover().target() == object;
    }

    public WorldObject object() {
        return object;
    }

    public void remove() {
        if (object != null && R3Client.started()) R3Mc.remove(object);
        object = null;
        anchor = null;
        companion = null;
    }
}
