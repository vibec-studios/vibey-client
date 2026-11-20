package dev.henny.vibeyclient.module.social.discord.ipc;

import dev.henny.vibeyclient.module.social.discord.core.IpcChannel;
import dev.henny.vibeyclient.module.social.discord.core.IpcConnector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;

public final class WindowsPipeConnector implements IpcConnector {
    private final List<String> pipes;

    public WindowsPipeConnector(List<String> pipes) {
        this.pipes = pipes;
    }

    @Override
    public IpcChannel open() throws IOException {
        IOException last = null;
        for (String p : pipes) {
            try {
                return wrap(new RandomAccessFile(p, "rw"));
            } catch (IOException e) {
                last = e;
            }
        }
        throw last != null ? last : new IOException("Discord is not running");
    }

    private static IpcChannel wrap(RandomAccessFile file) {
        InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                return file.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return file.read(b, off, len);
            }
        };
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                file.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                file.write(b, off, len);
            }
        };
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
                file.close();
            }
        };
    }
}
