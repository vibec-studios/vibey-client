package dev.henny.vibeyclient.module.cinematic.core.plan;

public enum Mode {
    BUILDS, PLAYERS, SELF, PANORAMA, WAYPOINTS, SKY, DRONIE, MIX;

    public static final Mode[] MIXED = {BUILDS, PLAYERS, PANORAMA, SELF, DRONIE, WAYPOINTS, SKY};
}
