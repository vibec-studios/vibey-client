package dev.henny.vibeyclient.module.cinematic.core.shot;

import dev.henny.vibeyclient.module.cinematic.core.Ease;
import dev.henny.vibeyclient.module.cinematic.core.Pose;
import dev.henny.vibeyclient.module.cinematic.core.Spline;
import dev.henny.vibeyclient.module.cinematic.core.V3;

import java.util.function.Supplier;

public final class PathShot implements Shot {
    private final Supplier<V3> origin;
    private final Spline path;
    private final Spline look;
    private final double duration;
    private final float fov0;
    private final float fov1;
    private final String subject;
    private final boolean live;

    private PathShot(Builder b) {
        origin = b.origin;
        path = b.path;
        look = b.look;
        duration = b.duration;
        fov0 = b.fov0;
        fov1 = b.fov1;
        subject = b.subject;
        live = b.live;
    }

    public static Builder along(Spline path, double duration) {
        return new Builder(path, duration);
    }

    @Override
    public double duration() {
        return duration;
    }

    @Override
    public Pose at(double t) {
        double u = Ease.gentle(t / duration);
        V3 o = origin.get();
        V3 pos = o.add(path.at(u));
        V3 target = o.add(look.at(Ease.inOutSine(t / duration)));
        return Pose.looking(pos, target, (float) Ease.lerp(fov0, fov1, Ease.inOutSine(t / duration)));
    }

    @Override
    public String subject() {
        return subject;
    }

    @Override
    public boolean live() {
        return live;
    }

    public static final class Builder {
        private final Spline path;
        private final double duration;
        private Supplier<V3> origin = () -> V3.ZERO;
        private Spline look;
        private float fov0 = 70;
        private float fov1 = 70;
        private String subject = "";
        private boolean live;

        private Builder(Spline path, double duration) {
            this.path = path;
            this.duration = Math.max(0.5, duration);
        }

        public Builder relativeTo(Supplier<V3> o, String name) {
            origin = o;
            subject = name;
            live = true;
            return this;
        }

        public Builder lookAt(V3 target) {
            look = Spline.of(target);
            return this;
        }

        public Builder lookAlong(Spline targets) {
            look = targets;
            return this;
        }

        public Builder fov(float from, float to) {
            fov0 = from;
            fov1 = to;
            return this;
        }

        public Builder subject(String name) {
            subject = name;
            return this;
        }

        public PathShot build() {
            if (look == null) look = Spline.of(path.at(1));
            return new PathShot(this);
        }
    }
}
