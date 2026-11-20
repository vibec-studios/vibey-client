package dev.henny.vibeyclient.module.social.discord.core;

import java.io.IOException;

public final class IpcException extends IOException {
    public IpcException(String message) {
        super(message);
    }
}
