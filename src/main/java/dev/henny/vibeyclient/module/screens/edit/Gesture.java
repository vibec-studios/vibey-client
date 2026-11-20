package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.Hit;
import dev.henny.vibeyclient.module.screens.core.Placement;

record Gesture(String id, Hit.Zone zone, double startX, double startY, Box startVisual, Placement startPlacement,
               boolean scalable) {
    boolean move() {
        return zone == Hit.Zone.BODY;
    }
}
