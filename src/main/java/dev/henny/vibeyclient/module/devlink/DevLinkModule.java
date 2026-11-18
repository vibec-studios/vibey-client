package dev.henny.vibeyclient.module.devlink;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.avatar.DevItems;
import dev.henny.vibeyclient.ui.Panel;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.net.BindException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Blockbench live link (SPEC §8): while the module is on and "Creator mode" is set, a WebSocket server on
 * 127.0.0.1:{@link #port} accepts the Blockbench plugin (token from {@code devlink.json}) and loads pushed
 * {@code .vibey} bundles as local DEV items through the engine's {@link DevItems} service.
 * Server start/stop and file I/O run on one background thread in order; game work hops to the client thread.
 */
public final class DevLinkModule extends VibeyModule {
    static final int MAX_MESSAGE_BYTES = 8 * 1024 * 1024;
    private static final int MAX_CONNECTIONS = 8;
    private static final String TOKEN_KEY = "token";

    public enum State { OFF, LISTENING, PORT_IN_USE, FAILED }

    public final BoolSetting creatorMode = bool("creatorMode", Vibey.platform().isDevelopment());
    public final IntSetting port = intRange("port", 47821, 1024, 65535).field().advanced();

    private final ExecutorService control = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VibeyClient-DevLink");
        t.setDaemon(true);
        return t;
    });
    private final long pid = ProcessHandle.current().pid();
    private final Map<String, Long> lastToast = new ConcurrentHashMap<>();

    // Only touched on the control thread (except reads of the volatile status fields).
    private WebSocketServer server;
    private volatile DevLinkProtocol protocol;
    /** Backend we listen to for submission.updated (client thread). */
    private dev.henny.vibeyclient.api.service.VibeBackend backend;
    private AutoCloseable submissionListener;
    private Path writtenFile;
    private volatile State state = State.OFF;
    private volatile int listeningPort;
    private volatile String failure = "";
    private volatile String token = "";

    public DevLinkModule() {
        super("devlink", ModuleCategory.CREATOR);
    }

    @Override
    public int accentColor() {
        return 0xFF6B6B75;
    }

    // ============================================================================================ lifecycle

    @Override
    public void onLoad() {
        JsonElement saved = settings().data().get(TOKEN_KEY);
        String t = saved != null && saved.isJsonPrimitive() ? saved.getAsString() : "";
        if (t.length() < 24) {
            t = DevLinkFile.newToken();
            settings().data().addProperty(TOKEN_KEY, t);
            settings().markDirty();
        }
        token = t;
        creatorMode.onChange(v -> sync(false));
        port.onChange(v -> Tasks.debounce("devlink.port", 600, () -> sync(true)));
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "VibeyClient-DevLink-Exit"));
    }

    @Override
    public void onEnable() {
        sync(false);
    }

    @Override
    public void onDisable() {
        sync(false);
    }

    @Override
    public void tick() {
        // our submissions were reviewed: tell the Blockbench plugin (VIBE-API-2 §3, gateway submission.updated)
        dev.henny.vibeyclient.api.service.VibeBackend b = Services.get(dev.henny.vibeyclient.api.service.VibeBackend.class).orElse(null);
        if (b == backend) return;
        if (submissionListener != null) {
            try {
                submissionListener.close();
            } catch (Exception ignored) {
            }
            submissionListener = null;
        }
        backend = b;
        if (b != null) submissionListener = b.onEvent("submission.updated", d -> {
            DevLinkProtocol p = protocol;
            if (p == null || !(d.get("submission") instanceof JsonObject sub)) return;
            JsonObject o = new JsonObject();
            o.addProperty("type", "submission");
            o.add("submission", sub);
            p.broadcast(o);
        });
    }

    @Override
    public Panel createSettingsPanel() {
        return new DevLinkPanel(this);
    }

    /** Brings the server in line with the settings; {@code restart} rebinds even if it is already running. */
    private void sync(boolean restart) {
        boolean want = isEnabled() && creatorMode.get();
        int p = port.get();
        control.execute(() -> apply(want, p, restart));
    }

    /** Control thread. */
    private void apply(boolean want, int p, boolean restart) {
        if (server != null && (!want || restart || p != listeningPort)) {
            stopServer(want ? "restarting" : "creator mode off");
        }
        if (!want) {
            state = State.OFF;
            failure = "";
            return;
        }
        if (server != null) return;
        DevLinkProtocol proto = new DevLinkProtocol(new GameSide(), Tasks::onMain);
        WebSocketServer s = new WebSocketServer(p, MAX_MESSAGE_BYTES, MAX_CONNECTIONS,
                java.net.InetAddress::isLoopbackAddress, proto);
        try {
            s.start();
        } catch (BindException e) {
            state = State.PORT_IN_USE;
            listeningPort = p;
            failure = String.valueOf(e.getMessage());
            log.warn("Devlink: port {} is in use ({})", p, e.getMessage());
            return;
        } catch (IOException | RuntimeException e) {
            state = State.FAILED;
            listeningPort = p;
            failure = String.valueOf(e.getMessage());
            log.warn("Devlink: could not start the server on port {}", p, e);
            return;
        }
        server = s;
        protocol = proto;
        listeningPort = s.localPort();
        state = State.LISTENING;
        failure = "";
        log.info("Devlink listening on 127.0.0.1:{}", listeningPort);
        writeFile();
    }

    private void stopServer(String reason) {
        server.stop(WebSocketServer.GOING_AWAY, reason);
        server = null;
        deleteFile();
        log.info("Devlink stopped ({})", reason);
    }

    private void writeFile() {
        Path file = devlinkFile();
        try {
            DevLinkFile.write(file, listeningPort, token, pid);
            writtenFile = file;
        } catch (IOException | RuntimeException e) {
            log.warn("Devlink: could not write {}", file, e);
        }
    }

    private void deleteFile() {
        if (writtenFile != null) DevLinkFile.delete(writtenFile, pid);
        writtenFile = null;
    }

    /** Game exit: stop and remove devlink.json (shutdown hook thread). */
    private void shutdown() {
        try {
            control.submit(() -> {
                if (server != null) stopServer("game closed");
            }).get(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            DevLinkFile.delete(devlinkFile(), pid);
        }
    }

    public Path devlinkFile() {
        return Vibey.platform().gameDir().resolve("vibeyclient").resolve("devlink.json");
    }

    // ============================================================================================ token

    public String token() {
        return token;
    }

    /** New token: kicks connected plugins (4001) and rewrites devlink.json. */
    public void regenerateToken() {
        String t = DevLinkFile.newToken();
        token = t;
        settings().data().addProperty(TOKEN_KEY, t);
        settings().markDirty();
        control.execute(() -> {
            if (server == null) return;
            for (WebSocketServer.Connection c : server.connections()) c.close(WebSocketServer.UNAUTHORIZED, "token regenerated");
            writeFile();
        });
    }

    // ============================================================================================ status (any thread)

    public State state() {
        return state;
    }

    public int listeningPort() {
        return listeningPort;
    }

    public String failure() {
        return failure;
    }

    /** Authenticated plugin connections. */
    public int clients() {
        WebSocketServer s = server;
        if (s == null) return 0;
        int n = 0;
        for (WebSocketServer.Connection c : s.connections()) if (DevLinkProtocol.isAuthed(c)) n++;
        return n;
    }

    // ============================================================================================ game side

    private final class GameSide implements DevLinkProtocol.Game {
        @Override
        public String token() {
            return token;
        }

        @Override
        public String gameVersion() {
            return BuildInfo.MINECRAFT;
        }

        @Override
        public String modVersion() {
            return BuildInfo.VERSION;
        }

        @Override
        public String playerName() {
            try {
                return Minecraft.getInstance().getUser().getName();
            } catch (RuntimeException e) {
                return "";
            }
        }

        @Override
        public Optional<DevItems> devItems() {
            return Services.get(DevItems.class);
        }

        @Override
        public void connected(String client, String version) {
            toast(Notify.Kind.SUCCESS, "toast.connected", new Object[0], "toast.connectedBody", version.isEmpty() ? "?" : version);
        }

        @Override
        public void pushed(JsonObject bundle, DevItems.PushResult result) {
            String id = result.id() != null ? result.id() : "";
            // Auto-push sends every ~300 ms while editing: one toast per item and second is plenty.
            if (!throttle("ok:" + id, 1500)) return;
            int warnings = result.warnings() == null ? 0 : result.warnings().size();
            Tasks.onMain(() -> {
                String title = tr("toast.pushed", displayName(bundle, id));
                if (warnings == 0) Notify.success(title, null);
                else Notify.warn(title, tr(warnings == 1 ? "toast.warning" : "toast.warnings", warnings));
            });
        }

        @Override
        public void rejected(String id, JsonObject bundle, String message) {
            if (!throttle("err:" + id + ":" + message, 3000)) return;
            Tasks.onMain(() -> Notify.error(tr("toast.rejected", displayName(bundle, id == null ? "?" : id)), message));
        }

        // ---------------------------------------------------------------- submit to Vibe (through our login)

        private java.util.concurrent.CompletableFuture<dev.henny.vibeyclient.api.service.VibeBackend> ready() {
            var b = Services.get(dev.henny.vibeyclient.api.service.VibeBackend.class).orElse(null);
            if (b == null || !b.isReady())
                return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException(tr("submit.signIn")));
            return java.util.concurrent.CompletableFuture.completedFuture(b);
        }

        private static JsonObject obj(JsonElement e) {
            if (e instanceof JsonObject o) return o;
            throw new IllegalStateException("unexpected answer from the backend");
        }

        @Override
        public java.util.concurrent.CompletableFuture<JsonObject> submit(JsonObject bundle, String name, String description, byte[] thumbnail) {
            byte[] bytes = bundle.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try {
                dev.henny.vibeyclient.avatar.bundle.BundleParser.parse(bytes); // same checks as the backend, faster feedback
            } catch (RuntimeException e) {
                return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("Invalid bundle: " + e.getMessage()));
            }
            String id = bundle.get("id") instanceof com.google.gson.JsonPrimitive p ? p.getAsString() : "bundle";
            java.util.List<dev.henny.vibeyclient.api.service.VibeBackend.Part> parts = new java.util.ArrayList<>();
            parts.add(new dev.henny.vibeyclient.api.service.VibeBackend.Part("bundle", id.replaceAll("[^a-z0-9_.-]", "_") + ".vibey", "application/json", bytes));
            if (!name.isBlank()) parts.add(dev.henny.vibeyclient.api.service.VibeBackend.Part.field("name", name));
            parts.add(dev.henny.vibeyclient.api.service.VibeBackend.Part.field("description", description));
            if (thumbnail != null) parts.add(new dev.henny.vibeyclient.api.service.VibeBackend.Part("thumbnail", "thumbnail.png", "image/png", thumbnail));
            return ready().thenCompose(b -> b.requestMultipart("/api/v1/cosmetics/submissions", parts)).thenApply(GameSide::obj)
                    .whenComplete((r, e) -> {
                        if (e == null) Tasks.onMain(() -> Notify.success(tr("submit.sent", displayName(bundle, id)), tr("submit.sentBody")));
                    });
        }

        @Override
        public java.util.concurrent.CompletableFuture<JsonObject> submissions() {
            return ready().thenCompose(b -> b.request("GET", "/api/v1/cosmetics/submissions", null)).thenApply(GameSide::obj);
        }

        @Override
        public java.util.concurrent.CompletableFuture<JsonObject> withdraw(String submissionId) {
            String path = "/api/v1/cosmetics/submissions/" + java.net.URLEncoder.encode(submissionId, java.nio.charset.StandardCharsets.UTF_8);
            return ready().thenCompose(b -> b.request("DELETE", path, null)).thenApply(GameSide::obj);
        }

        private void toast(Notify.Kind kind, String titleKey, Object[] titleArgs, String bodyKey, Object... bodyArgs) {
            Tasks.onMain(() -> Notify.show(new Notify.Toast(tr(titleKey, titleArgs), tr(bodyKey, bodyArgs), kind, null, null)));
        }
    }

    private boolean throttle(String key, long ms) {
        long now = System.currentTimeMillis();
        Long last = lastToast.put(key, now);
        if (lastToast.size() > 64) lastToast.clear();
        return last == null || now - last >= ms;
    }

    /** The bundle's name in the current language, else its id. Client thread. */
    private static String displayName(JsonObject bundle, String fallback) {
        try {
            JsonElement n = bundle == null ? null : bundle.get("name");
            if (n != null && n.isJsonObject()) {
                JsonObject o = n.getAsJsonObject();
                String lang = Lang.german() ? "de" : "en";
                JsonElement v = o.has(lang) ? o.get(lang) : o.get("en");
                if (v != null && v.isJsonPrimitive() && !v.getAsString().isBlank()) return v.getAsString();
            }
        } catch (RuntimeException ignored) {
            // fall back to the id
        }
        return fallback;
    }
}
