package dev.henny.companio.sync;

import dev.henny.companio.anim.Animator;
import dev.henny.companio.def.AnimRef;
import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.def.PropDef;
import dev.henny.companio.move.Angles;
import dev.henny.companio.move.Body;
import dev.henny.companio.world.CompanionView;
import dev.henny.cosmetico.model.Bundle;

import java.util.ArrayList;
import java.util.List;

public final class Puppet implements CompanionView {
    public static final double TIMEOUT = 10;

    private final CompanionDef def;
    private final Body body = new Body();
    private final Animator animator;
    private Snapshot last;
    private double received;
    private final List<String> playing = new ArrayList<>();
    private boolean placed;

    public Puppet(CompanionDef def, Snapshot first, double now) {
        this.def = def;
        this.animator = new Animator(def.model(first.variant()));
        update(first, now);
    }

    public void update(Snapshot s, double now) {
        if (last == null || !last.variant().equals(s.variant())) animator.bundle(def.model(s.variant()));
        last = s;
        received = now;
        body.width = def.box().width() * s.size();
        body.height = def.box().height() * s.size();
        if (!placed || body.distance(s.x(), s.y(), s.z()) > 8) {
            body.place(s.x(), s.y(), s.z());
            body.yaw = body.pyaw = s.yaw();
            placed = true;
        }
    }

    public void tick(float dt) {
        body.beginTick();
        body.x += (last.x() - body.x) * 0.3;
        body.y += (last.y() - body.y) * 0.3;
        body.z += (last.z() - body.z) * 0.3;
        body.vx = body.x - body.px;
        body.vz = body.z - body.pz;
        body.yaw = Angles.approach(body.yaw, last.yaw(), 360f * dt);
        animator.base(last.base().isEmpty() ? null : last.base(), 1f, dt);
        for (String l : last.layers()) if (!playing.contains(l)) animator.play(AnimRef.layer(l), true);
        for (String l : playing) if (!last.layers().contains(l)) animator.stop(l);
        playing.clear();
        playing.addAll(last.layers());
        animator.tick(dt);
    }

    public boolean stale(double now) {
        return now - received > TIMEOUT;
    }

    public Snapshot last() {
        return last;
    }

    public CompanionDef def() { return def; }
    public Bundle model() { return def.model(last.variant()); }
    public Body body() { return body; }
    public Animator animator() { return animator; }
    public float headYaw(float partial) { return 0f; }
    public float headPitch(float partial) { return 0f; }
    public String bubbleText() { return last.bubble(); }
    public String name() { return last.name(); }
    public float size() { return last.size(); }
    public boolean held() { return last.held(); }

    public boolean[] hiddenBones(boolean[] out) {
        Bundle b = model();
        int n = b == null ? 0 : b.model.bones.length;
        if (out == null || out.length < n) out = new boolean[n];
        java.util.Arrays.fill(out, false);
        for (PropDef p : def.props().values()) if (!last.props().contains(p.id())) for (int i : p.boneIndices()) if (i < n) out[i] = true;
        return out;
    }
}
