package dev.henny.vibeyclient.module.social.discord.core;

import java.io.IOException;

public interface IpcConnector {
    IpcChannel open() throws IOException;
}
