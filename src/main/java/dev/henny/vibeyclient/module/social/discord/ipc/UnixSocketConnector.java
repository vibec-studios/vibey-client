package dev.henny.vibeyclient.module.social.discord.ipc;

import dev.henny.vibeyclient.module.social.discord.core.IpcChannel;
import dev.henny.vibeyclient.module.social.discord.core.IpcConnector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class UnixSocketConnector implements IpcConnector {
    private final List<String> paths;

    public UnixSocketConnector(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public IpcChannel open() throws IOException {
        IOException last = null;
        for (String p : paths) {
            Path path = Path.of(p);
            if (!Files.exists(path)) continue;
            try {
                return wrap(connect(path));
            } catch (IOException e) {
                last = e;
            }
        }
        throw last != null ? last : new IOException("Discord is not running");
    }

    private static SocketChannel connect(Path path) throws IOException {
        SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
        try {
            ch.connect(UnixDomainSocketAddress.of(path));
            return ch;
        } catch (IOException e) {
            ch.close();
            throw e;
        }
    }

    private static IpcChannel wrap(SocketChannel ch) {
        InputStream in = Channels.newInputStream(ch);
        OutputStream out = Channels.newOutputStream(ch);
        return new IpcChannel() {
            @Override
            public InputStream in() {
                return in;
            }

            @Override
            public OutputStream out() {
                return out;
            }

            @Override
            public void close() throws IOException {
                ch.close();
            }
        };
    }
}
