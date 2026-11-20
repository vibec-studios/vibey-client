package dev.henny.vibeyclient.module.cinematic.core;

public record Pose(V3 pos, float yaw, float pitch, float fov) {
    public static Pose looking(V3 pos, V3 target, float fov) {
        return new Pose(pos, Look.yaw(pos, target), Look.pitch(pos, target), fov);
    }

    public Pose lerp(Pose o, double t) {
        return new Pose(pos.lerp(o.pos, t), Look.lerpAngle(yaw, o.yaw, t), (float) Ease.lerp(pitch, o.pitch, t),
                (float) Ease.lerp(fov, o.fov, t));
    }

    public Pose withPos(V3 p) {
        return new Pose(p, yaw, pitch, fov);
    }
}
