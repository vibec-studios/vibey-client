package dev.henny.vibeyclient.module.cinematic.core.shot;

import dev.henny.vibeyclient.module.cinematic.core.Ease;
import dev.henny.vibeyclient.module.cinematic.core.Look;
import dev.henny.vibeyclient.module.cinematic.core.Pose;
import dev.henny.vibeyclient.module.cinematic.core.V3;

public record PanShot(V3 from, V3 to, float yaw0, float yaw1, float pitch0, float pitch1, double duration,
                      float fov) implements Shot {

    @Override
    public Pose at(double t) {
        double u = Ease.gentle(t / duration);
        return new Pose(from.lerp(to, Ease.inOutSine(t / duration)), Look.lerpAngle(yaw0, yaw1, u),
                (float) Ease.lerp(pitch0, pitch1, Ease.inOutSine(t / duration)), fov);
    }
}
