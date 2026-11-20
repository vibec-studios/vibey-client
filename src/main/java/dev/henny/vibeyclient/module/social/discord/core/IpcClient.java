package dev.henny.vibeyclient.module.social.discord.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

public final class IpcClient implements AutoCloseable {
    private static final int MAX_SKIPPED = 8;

    private final IpcConnector connector;
    private final long pid;
    private IpcChannel channel;
    private int nonce;

    public IpcClient(IpcConnector connector, long pid) {
        this.connector = connector;
        this.pid = pid;
    }

    public boolean connected() {
        return channel != null;
    }

    public JsonObject connect(String clientId) throws IOException {
        close();
        channel = connector.open();
        JsonObject hello = new JsonObject();
        hello.addProperty("v", 1);
        hello.addProperty("client_id", clientId);
        send(IpcOp.HANDSHAKE, hello);
        JsonObject ready = await(null);
        if (!"READY".equals(string(ready, "evt"))) throw new IpcException("Discord did not answer READY");
        return ready;
    }

    public JsonObject setActivity(JsonObject activity) throws IOException {
        if (channel == null) throw new IpcException("not connected");
        String n = Integer.toString(++nonce);
        JsonObject args = new JsonObject();
        args.addProperty("pid", pid);
        args.add("activity", activity == null ? JsonNull.INSTANCE : activity);
        JsonObject cmd = new JsonObject();
        cmd.addProperty("cmd", "SET_ACTIVITY");
        cmd.add("args", args);
        cmd.addProperty("nonce", n);
        send(IpcOp.FRAME, cmd);
        return await(n);
    }

    private void send(int op, JsonObject body) throws IOException {
        new IpcFrame(op, body.toString()).write(channel.out());
    }

    private JsonObject await(String wantedNonce) throws IOException {
        for (int i = 0; i < MAX_SKIPPED; i++) {
            IpcFrame f = IpcFrame.read(channel.in());
            if (f.op() == IpcOp.PING) {
                new IpcFrame(IpcOp.PONG, f.json()).write(channel.out());
                continue;
            }
            JsonObject o = parse(f.json());
            if (f.op() == IpcOp.CLOSE) throw new IpcException("Discord closed: " + string(o, "message"));
            if (wantedNonce != null && !wantedNonce.equals(string(o, "nonce"))) continue;
            if ("ERROR".equals(string(o, "evt"))) throw new IpcException("Discord error: " + errorMessage(o));
            return o;
        }
        throw new IpcException("no answer from Discord");
    }

    private static String errorMessage(JsonObject o) {
        JsonElement d = o.get("data");
        return d != null && d.isJsonObject() ? string(d.getAsJsonObject(), "message") : "";
    }

    private static JsonObject parse(String json) throws IpcException {
        try {
            JsonElement e = JsonParser.parseString(json);
            return e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException ex) {
            throw new IpcException("bad JSON from Discord");
        }
    }

    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }

    @Override
    public void close() {
        IpcChannel c = channel;
        channel = null;
        if (c == null) return;
        try {
            new IpcFrame(IpcOp.CLOSE, "{}").write(c.out());
        } catch (IOException ignored) {
        }
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }
}
