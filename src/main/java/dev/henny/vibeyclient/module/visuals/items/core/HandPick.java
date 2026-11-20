package dev.henny.vibeyclient.module.visuals.items.core;

public final class HandPick {
    public static final float GRAB_SLACK = 1.6f;

    private HandPick() {}

    public static int nearest(EditSession session, int mainSide, double x, double y, float width, float height) {
        double main = distance(session.pose(EditSession.MAIN), mainSide, x, y, width, height);
        double off = distance(session.pose(EditSession.OFF), -mainSide, x, y, width, height);
        return off < main ? EditSession.OFF : EditSession.MAIN;
    }

    public static boolean over(HandPose pose, int side, double x, double y, float width, float height) {
        float r = HandProjection.radius(pose, side, height) * GRAB_SLACK;
        return distance(pose, side, x, y, width, height) <= r;
    }

    public static double distance(HandPose pose, int side, double x, double y, float width, float height) {
        float[] s = HandProjection.toScreen(HandProjection.anchor(pose, side), width, height);
        return Math.hypot(s[0] - x, s[1] - y);
    }
}
