package dev.henny.vibeyclient.module.cinematic.core.shot;

import dev.henny.vibeyclient.module.cinematic.core.Ease;
import dev.henny.vibeyclient.module.cinematic.core.Pose;
import dev.henny.vibeyclient.module.cinematic.core.V3;

import java.util.function.Supplier;

public record OrbitShot(Supplier<V3> center, double radius0, double radius1, double height0, double height1,
                        double fromDeg, double toDeg, double lookHeight, double duration, float fov,
                        String subject, boolean live) implements Shot {

    public static OrbitShot around(V3 center, double radius, double height, double fromDeg, double toDeg,
                                   double lookHeight, double duration, float fov) {
        return new OrbitShot(() -> center, radius, radius, height, height, fromDeg, toDeg, lookHeight, duration, fov, "", false);
    }

    public OrbitShot following(Supplier<V3> c, String name) {
        return new OrbitShot(c, radius0, radius1, height0, height1, fromDeg, toDeg, lookHeight, duration, fov, name, true);
    }

    public OrbitShot drift(double radiusTo, double heightTo) {
        return new OrbitShot(center, radius0, radiusTo, height0, heightTo, fromDeg, toDeg, lookHeight, duration, fov, subject, live);
    }

    @Override
    public Pose at(double t) {
        double u = Ease.gentle(t / duration);
        double a = Math.toRadians(Ease.lerp(fromDeg, toDeg, u));
        double r = Ease.lerp(radius0, radius1, Ease.inOutSine(t / duration));
        double h = Ease.lerp(height0, height1, Ease.inOutSine(t / duration));
        V3 c = center.get();
        V3 pos = c.add(Math.sin(a) * r, h, Math.cos(a) * r);
        return Pose.looking(pos, c.add(0, lookHeight, 0), fov);
    }
}
