package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.gametest.media.MediaR3Run;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class MediaR3GameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        MediaR3Run.run(ctx);
    }
}
