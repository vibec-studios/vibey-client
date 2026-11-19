package dev.henny.r3.scene;

import dev.henny.r3.anchor.AnchorContext;
import dev.henny.r3.anchor.Facing;
import dev.henny.r3.anchor.WorldQuery;
import dev.henny.r3.fx.Effect;
import dev.henny.r3.fx.EffectBudget;
import dev.henny.r3.fx.EffectContext;
import dev.henny.r3.object.ObjectContent;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.render.Light;
import dev.henny.r3.view.CameraState;
import dev.henny.r3.view.ViewFrustum;
import org.joml.Vector3d;

final class ObjectUpdater {
    static final double LIGHT_PROBE_OFFSET = 0.3;

    private final AnchorContext anchors = new AnchorContext();
    private final Vector3d normal = new Vector3d();
    private WorldQuery query;
    private CameraState camera;
    private ViewFrustum frustum;
    private EffectBudget budget;
    private String dimension;
    private float dt;

    void begin(WorldQuery query, CameraState camera, ViewFrustum frustum, EffectBudget budget, float dt) {
        this.query = query;
        this.camera = camera;
        this.frustum = frustum;
        this.budget = budget;
        this.dt = dt;
        this.dimension = query == null ? null : query.dimension();
        anchors.set(query, camera, dt);
    }

    boolean update(WorldObject o, boolean held, EffectContext effects) {
        if (o.dimension() != null && !o.dimension().equals(dimension)) {
            o.state().hide();
            return false;
        }
        Transform t = o.transform();
        boolean anchored = held || o.anchor().resolve(anchors, t);
        ObjectContent content = o.content();
        if (!held && content != null && content.facing() != Facing.KEEP) content.facing().apply(t, camera, 0f);
        double distance = camera.distanceTo(t.position());
        float alpha = anchored ? o.visibility().alphaAt(distance) : 0f;
        double radius = content == null ? t.boundingRadius() : content.boundingRadius(t);
        boolean visible = alpha > 0f && inFrustum(t, radius);
        o.state().update(visible, anchored, distance, alpha, light(o));
        updateContent(o, content, distance, effects);
        return visible;
    }

    private void updateContent(WorldObject o, ObjectContent content, double distance, EffectContext effects) {
        if (content instanceof Effect effect) {
            if (budget.culled(distance)) return;
            effects.origin(o.transform().position(), o.transform().orientation())
                    .detail(budget.lodAt(distance), o.state().alpha(), o.state().light(), budget);
            effect.simulate(dt, effects);
        } else if (content != null) {
            content.update(dt);
        }
    }

    private boolean inFrustum(Transform t, double radius) {
        Vector3d p = t.position();
        return frustum.isVisible(p.x - radius, p.y - radius, p.z - radius, p.x + radius, p.y + radius, p.z + radius);
    }

    private int light(WorldObject o) {
        if (o.fullBright() || query == null) return Light.FULL_BRIGHT;
        Transform t = o.transform();
        t.normal(normal);
        Vector3d p = t.position();
        return query.lightAt(p.x + normal.x * LIGHT_PROBE_OFFSET, p.y + normal.y * LIGHT_PROBE_OFFSET,
                p.z + normal.z * LIGHT_PROBE_OFFSET);
    }
}
