package dev.henny.vibeyclient.module.social.discord.core;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

public interface IpcChannel extends Closeable {
    InputStream in();

    OutputStream out();
}
