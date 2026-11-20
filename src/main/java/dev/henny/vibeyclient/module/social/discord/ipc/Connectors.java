package dev.henny.vibeyclient.module.social.discord.ipc;

import dev.henny.vibeyclient.module.social.discord.core.IpcConnector;
import dev.henny.vibeyclient.module.social.discord.core.PipePaths;

public final class Connectors {
    private Connectors() {}

    public static IpcConnector local() {
        if (PipePaths.windows(System.getProperty("os.name"))) return new WindowsPipeConnector(PipePaths.windowsPipes());
        return new UnixSocketConnector(PipePaths.unixSockets(System.getenv()));
    }
}
