package dev.henny.vibeyclient.module.social.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.module.social.discord.core.IpcFrame;
import dev.henny.vibeyclient.module.social.discord.core.IpcOp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class FakeDiscord implements AutoCloseable {
    final Path socket;
    final List<JsonObject> commands = new CopyOnWriteArrayList<>();
    final List<String> handshakes = new CopyOnWriteArrayList<>();
    final CountDownLatch gotCommand = new CountDownLatch(1);
    volatile boolean pingFirst;
    volatile String errorFor;
    private final ServerSocketChannel server;
    private final Thread thread;

    FakeDiscord(Path dir) throws IOException {
        socket = dir.resolve("discord-ipc-0");
        Files.deleteIfExists(socket);
        server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        server.bind(UnixDomainSocketAddress.of(socket));
        thread = new Thread(this::serve, "fake-discord");
        thread.setDaemon(true);
        thread.start();
    }

    private void serve() {
        while (server.isOpen()) {
            try (SocketChannel c = server.accept()) {
                session(Channels.newInputStream(c), Channels.newOutputStream(c));
            } catch (IOException e) {
                if (!server.isOpen()) return;
            }
        }
    }

    private void session(InputStream in, OutputStream out) throws IOException {
        IpcFrame hello = IpcFrame.read(in);
        if (hello.op() != IpcOp.HANDSHAKE) return;
        handshakes.add(JsonParser.parseString(hello.json()).getAsJsonObject().get("client_id").getAsString());
        new IpcFrame(IpcOp.FRAME, "{\"cmd\":\"DISPATCH\",\"evt\":\"READY\",\"data\":{\"v\":1}}").write(out);
        while (true) {
            IpcFrame f = IpcFrame.read(in);
            if (f.op() == IpcOp.CLOSE) return;
            if (f.op() == IpcOp.PONG) continue;
            JsonObject cmd = JsonParser.parseString(f.json()).getAsJsonObject();
            String nonce = cmd.get("nonce").getAsString();
            if (pingFirst) new IpcFrame(IpcOp.PING, "{\"p\":1}").write(out);
            commands.add(cmd);
            gotCommand.countDown();
            String evt = nonce.equals(errorFor) ? "\"ERROR\",\"data\":{\"message\":\"nope\"}" : "null,\"data\":{}";
            new IpcFrame(IpcOp.FRAME, "{\"cmd\":\"SET_ACTIVITY\",\"nonce\":\"" + nonce + "\",\"evt\":" + evt + "}").write(out);
        }
    }

    boolean awaitCommand() throws InterruptedException {
        return gotCommand.await(5, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws IOException {
        server.close();
        Files.deleteIfExists(socket);
    }
}
