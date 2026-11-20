package dev.henny.vibeyclient.module.social.discord.core;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public record IpcFrame(int op, String json) {
    public static final int MAX_BODY = 64 * 1024;

    public byte[] encode() {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer b = ByteBuffer.allocate(8 + body.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(op).putInt(body.length).put(body);
        return b.array();
    }

    public void write(OutputStream out) throws IOException {
        out.write(encode());
        out.flush();
    }

    public static IpcFrame read(InputStream in) throws IOException {
        ByteBuffer head = ByteBuffer.wrap(readFully(in, 8)).order(ByteOrder.LITTLE_ENDIAN);
        int op = head.getInt();
        int len = head.getInt();
        if (len < 0 || len > MAX_BODY) throw new IOException("Discord IPC frame too large: " + len);
        return new IpcFrame(op, new String(readFully(in, len), StandardCharsets.UTF_8));
    }

    private static byte[] readFully(InputStream in, int n) throws IOException {
        byte[] out = new byte[n];
        int at = 0;
        while (at < n) {
            int r = in.read(out, at, n - at);
            if (r < 0) throw new EOFException("Discord IPC closed");
            at += r;
        }
        return out;
    }
}
