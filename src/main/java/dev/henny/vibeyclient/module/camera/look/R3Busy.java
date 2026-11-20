package dev.henny.vibeyclient.module.camera.look;

import dev.henny.r3.interact.Interaction;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.mc.client.R3Client;

final class R3Busy {
    private R3Busy() {}

    static boolean busy() {
        if (!R3Client.started()) return false;
        Interaction i = R3Mc.scene().interaction();
        return i.capturing() || i.hover().target() != null;
    }
}
