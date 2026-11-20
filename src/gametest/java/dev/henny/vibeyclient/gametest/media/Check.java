package dev.henny.vibeyclient.gametest.media;

import dev.henny.r3.interact.HoverZone;
import dev.henny.r3.interact.InteractionState;
import dev.henny.r3.mc.R3Mc;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.util.Locale;

final class Check {
    static final String P = "[media-r3-gametest] ";

    private Check() {}

    static void that(boolean ok, String message) {
        if (!ok) throw new AssertionError(P + message);
    }

    static void log(String format, Object... args) {
        System.out.println(P + String.format(Locale.ROOT, format, args));
    }

    static InteractionState state(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> R3Mc.scene().interaction().state());
    }

    static HoverZone zone(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> R3Mc.scene().interaction().hover().zone());
    }

    static String control(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> R3Mc.scene().interaction().hover().controlId());
    }
}
