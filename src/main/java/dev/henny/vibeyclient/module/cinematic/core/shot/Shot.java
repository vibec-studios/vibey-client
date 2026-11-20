package dev.henny.vibeyclient.module.cinematic.core.shot;

import dev.henny.vibeyclient.module.cinematic.core.Pose;

public interface Shot {
    double duration();

    Pose at(double t);

    default String subject() {
        return "";
    }

    default boolean live() {
        return false;
    }
}
