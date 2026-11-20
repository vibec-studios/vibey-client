package dev.henny.vibeyclient.module.social.discord;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.module.social.discord.core.IpcClient;
import dev.henny.vibeyclient.module.social.discord.core.IpcConnector;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class DiscordWorker {
    public static final long FIRST_RETRY_MS = 15_000;
    public static final long MAX_RETRY_MS = 120_000;
    public static final long REFRESH_MS = 60_000;

    private final ExecutorService thread = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "vibeyclient-discord");
        t.setDaemon(true);
        return t;
    });
    private final IpcClient client;
    private final String appId;
    private final Consumer<String> log;
    private volatile boolean connected;
    private String sent;
    private boolean sentAny;
    private long sentAt;
    private long nextAttempt;
    private long retry = FIRST_RETRY_MS;

    public DiscordWorker(IpcConnector connector, String appId, Consumer<String> log) {
        this.client = new IpcClient(connector, ProcessHandle.current().pid());
        this.appId = appId;
        this.log = log;
    }

    public boolean connected() {
        return connected;
    }

    public void push(JsonObject activity) {
        String json = activity == null ? null : activity.toString();
        thread.execute(() -> apply(activity, json));
    }

    public void stop() {
        thread.execute(() -> {
            if (client.connected()) {
                try {
                    client.setActivity(null);
                } catch (IOException ignored) {
                }
            }
            client.close();
            connected = false;
            sentAny = false;
        });
        thread.shutdown();
    }

    private void apply(JsonObject activity, String json) {
        if (!client.connected() && !connect()) return;
        long now = System.currentTimeMillis();
        if (sentAny && Objects.equals(json, sent) && now - sentAt < REFRESH_MS) return;
        try {
            client.setActivity(activity);
            sent = json;
            sentAny = true;
            sentAt = now;
        } catch (IOException e) {
            log.accept("Discord presence update failed: " + e.getMessage());
            drop();
        }
    }

    private boolean connect() {
        long now = System.currentTimeMillis();
        if (now < nextAttempt) return false;
        try {
            client.connect(appId);
            connected = true;
            retry = FIRST_RETRY_MS;
            sentAny = false;
            return true;
        } catch (IOException e) {
            drop();
            return false;
        }
    }

    private void drop() {
        client.close();
        connected = false;
        sentAny = false;
        nextAttempt = System.currentTimeMillis() + retry;
        retry = Math.min(MAX_RETRY_MS, retry * 2);
    }
}
