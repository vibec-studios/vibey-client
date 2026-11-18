package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.ui.title.BackendStatus;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Regression (launcher test, 0.2.0): with an offline Minecraft profile (no Microsoft login) and online features on,
 * the title status said "Signing in…" forever although nothing signs in. It must say "No Vibe login".
 * The dev client of the game test always has an offline profile.
 */
public final class BackendStatusGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        if (BackendClient.devAuth()) return; // dev login can sign in without Mojang: not the offline case
        ctx.runOnClient(mc -> {
            GlobalSettings.onlineFeatures.set(true);
            ModuleManager.get().setEnabled("social", true);
        });
        // the backend client settles in LOGGED_OUT right away (no Mojang session → no login attempt)
        BackendClient.State st = null;
        for (int i = 0; i < 100; i++) {
            st = ctx.computeOnClient(mc -> Services.get(VibeBackend.class)
                    .map(b -> b instanceof BackendClient c ? c.state() : null).orElse(null));
            if (st == BackendClient.State.LOGGED_OUT) break;
            ctx.waitTicks(2);
        }
        if (st != BackendClient.State.LOGGED_OUT) throw new AssertionError("backend client not LOGGED_OUT with an offline profile: " + st);

        ctx.setScreen(net.minecraft.client.gui.screens.TitleScreen::new);
        ctx.waitTicks(10);
        BackendStatus.State shown = ctx.computeOnClient(mc -> BackendStatus.state());
        if (shown != BackendStatus.State.NO_ACCOUNT) throw new AssertionError("title status for an offline profile: " + shown + " (expected NO_ACCOUNT)");
        String label = ctx.computeOnClient(mc -> BackendStatus.label());
        if (label.contains("…")) throw new AssertionError("status still looks like it is signing in: " + label);
        ctx.takeScreenshot("status_offline_profile_title");
        ctx.setScreen(() -> null);
    }
}
