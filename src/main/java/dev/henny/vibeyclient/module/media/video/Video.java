package dev.henny.vibeyclient.module.media.video;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.media.video.player.FfmpegPlayer;
import dev.henny.vibeyclient.module.media.video.player.Ffmpeg;
import dev.henny.vibeyclient.module.media.video.player.MediaPlayer;
import dev.henny.vibeyclient.module.media.video.render.ScreenRenderer;
import dev.henny.vibeyclient.module.media.video.ui.AcceptScreen;
import dev.henny.vibeyclient.module.media.video.ui.ControlsScreen;
import dev.henny.vibeyclient.module.media.video.ui.OverlayEditScreen;
import dev.henny.vibeyclient.module.media.video.ui.SetupScreen;
import dev.henny.vibeyclient.module.media.video.ui.SourceScreen;
import dev.henny.vibeyclient.module.media.video.web.Web;
import dev.henny.vibeyclient.ui.Gfx;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The video screens feature. Keeps every screen (own and other players'), decides which ones get a player (near,
 * in this dimension, allowed), keeps the players on each screen's timeline, and wires rendering, the overlay,
 * placement and sync. One instance, see {@link #get()}.
 */
public final class Video implements VideoFeature {
    /** At most this many screens decode at the same time (the nearest ones). */
    public static final int MAX_ACTIVE = 6;
    /** Closed after being far away / hidden this long (a paused player keeps processes or a browser alive). */
    private static final long CLOSE_AFTER_MS = 15_000;
    private static Video instance;

    public final VideoSettings cfg;
    public final Map<UUID, MediaScreen> screens = new LinkedHashMap<>();
    public final Placement placement = new Placement(this);
    public final VideoNet net = new VideoNet(this);
    public final Overlay overlay = new Overlay(this);
    private boolean enabled;
    private boolean hooks;
    private String worldKey;
    public UUID overlayId;
    private long lastSave;

    public Video(VideoSettings cfg) {
        this.cfg = cfg;
        instance = this;
    }

    public static Video get() {
        return instance;
    }

    public static String tr(String key, Object... args) {
        return Lang.tr("video." + key, args);
    }

    // ============================================================================================ lifecycle

    @Override
    public void onLoad() {
        net.register();
        VideoDev.register(this);
        if (hooks) return;
        hooks = true;
        WorldRenderEvents.END_EXTRACTION.register(ctx -> {
            if (enabled) frame(ctx.frustum());
        });
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (enabled) ScreenRenderer.render(this, ctx.matrices(), ctx.commandQueue());
        });
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            if (enabled) placement.preTick(mc);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> closeAllPlayers());
    }

    @Override
    public void onEnable() {
        enabled = true;
        if (Minecraft.getInstance().level != null) onWorldJoin();
    }

    @Override
    public void onDisable() {
        saveOwn();
        enabled = false;
        placement.cancel();
        closeAllPlayers();
        screens.clear();
        overlayId = null;
        worldKey = null;
    }

    @Override
    public void onWorldJoin() {
        if (!enabled) return;
        worldKey = worldKey();
        screens.clear();
        loadOwn();
        net.onJoin();
    }

    @Override
    public void onWorldLeave() {
        saveOwn();
        placement.cancel();
        closeAllPlayers();
        screens.clear();
        overlayId = null;
        worldKey = null;
    }

    @Override
    public List<HudElement> hudElements() {
        return List.of(overlay);
    }

    public boolean enabled() {
        return enabled;
    }

    // ============================================================================================ tick

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.level == null || mc.player == null) return;
        if (worldKey == null) onWorldJoin();
        Vec3 eye = mc.player.getEyePosition();
        String dim = dimension();
        long now = System.currentTimeMillis();
        int range = cfg.distance.get();

        List<MediaScreen> wanted = new ArrayList<>();
        for (MediaScreen s : screens.values()) {
            boolean here = s.placed() && dim.equals(s.dimension);
            s.distance = here ? eye.distanceTo(s.center()) : Double.MAX_VALUE;
            boolean want = s.source != null && allowed(s) && (s.id.equals(overlayId) || here && s.distance <= range);
            if (want) wanted.add(s);
        }
        // Overlay first, then playing screens, then by distance (paused ones still show their picture).
        wanted.sort(Comparator.comparingDouble(s -> s.id.equals(overlayId) ? -1 : s.distance + (s.playing ? 0 : 10_000)));
        for (int i = 0; i < wanted.size(); i++) {
            MediaScreen s = wanted.get(i);
            if (i >= MAX_ACTIVE) {
                park(s, now);
                continue;
            }
            s.farSince = 0;
            if (s.player == null) s.player = createPlayer(s);
            if (s.player != null) follow(s, range);
        }
        for (MediaScreen s : screens.values()) {
            if (!wanted.contains(s) && s.player != null) park(s, now);
        }
        placement.tick(mc);
        net.tick(now);
        if (now - lastSave > 30_000) {
            lastSave = now;
            saveOwn();
        }
    }

    /** Out of range (or over the limit): pause, close a while later. The timeline goes on. */
    private void park(MediaScreen s, long now) {
        if (s.player == null) return;
        s.player.pause();
        s.player.audio(0f, null);
        appliedPlaying.remove(s.id);
        if (s.farSince == 0) s.farSince = now;
        else if (now - s.farSince > CLOSE_AFTER_MS) closePlayer(s);
    }

    private final Map<UUID, Boolean> appliedPlaying = new java.util.HashMap<>();
    private final Map<UUID, Long> lastCorrection = new java.util.HashMap<>();

    /** Keeps the player on the screen's timeline and sets its volume. */
    private void follow(MediaScreen s, int range) {
        MediaPlayer p = s.player;
        long now = System.currentTimeMillis();
        if (p.durationMs() > 0) s.durationMs = p.durationMs();
        Boolean applied = appliedPlaying.get(s.id);
        if (applied == null || applied != s.playing) {
            if (s.playing) p.play();
            else p.pause();
            appliedPlaying.put(s.id, s.playing);
            if (applied == null && Math.abs(p.positionMs() - s.expectedMs()) > 1500 && (p.seekable() || s.durationMs > 0)) p.seek(s.expectedMs());
        }
        MediaPlayer.Status st = p.status();
        if (!s.remote && !s.started && st == MediaPlayer.Status.PLAYING) {
            // Own screen: the timeline starts when the picture does (loading time is not drift).
            s.started = true;
            if (Math.abs(p.positionMs() - s.expectedMs()) < 5000) {
                s.setTimeline(true, p.positionMs());
                net.sendState(s);
            }
        }
        if (st == MediaPlayer.Status.ENDED && s.playing) {
            if (s.loop) {
                if (!s.remote) {
                    s.setTimeline(true, 0);
                    net.sendState(s);
                }
                p.seek(0);
                p.play();
            } else if (!s.remote) {
                s.setTimeline(false, s.durationMs > 0 ? s.durationMs : p.positionMs());
                appliedPlaying.put(s.id, false);
                net.sendState(s);
            }
        } else if (!s.remote && st == MediaPlayer.Status.PLAYING && s.playing && s.started && (p.seekable() || s.durationMs > 0)) {
            // Own screen: what the owner actually sees is the truth (buffering, slow streams) – others follow it.
            long behind = s.expectedMs() - p.positionMs();
            long last = lastCorrection.getOrDefault(s.id, 0L);
            if (Math.abs(behind) > 750 && now - last > 5000) {
                lastCorrection.put(s.id, now);
                s.setTimeline(true, p.positionMs());
                net.sendState(s);
            }
        } else if (s.remote && (st == MediaPlayer.Status.PLAYING || st == MediaPlayer.Status.PAUSED) && (p.seekable() || s.durationMs > 0)) {
            long behind = s.expectedMs() - p.positionMs();
            if (s.correcting && st == MediaPlayer.Status.PLAYING) {
                // How long the last seek took to show a picture: aim that much further next time.
                s.correcting = false;
                s.lagMs = Math.max(0, Math.min(5000, s.lagMs + behind));
            }
            long last = lastCorrection.getOrDefault(s.id, 0L);
            long limit = s.playing ? (s.source.kind() == MediaSource.Kind.FILE ? 2000 : 3000) : 600;
            if (Math.abs(behind) > limit && now - last > 5000) {
                lastCorrection.put(s.id, now);
                s.correcting = s.playing;
                p.seek(s.expectedMs() + (s.playing ? s.lagMs : 0));
            }
        }
        float gain = cfg.volume.get() / 100f * s.volume * soundVolume();
        boolean asOverlay = s.id.equals(overlayId);
        if (asOverlay) {
            p.audio(gain, null);
        } else {
            double d = s.distance;
            float t = (float) Math.max(0, Math.min(1, 1 - (d - 3) / Math.max(1, range - 3)));
            p.audio(gain * t * t, s.center());
        }
    }

    private static float soundVolume() {
        var o = Minecraft.getInstance().options;
        return o.getSoundSourceVolume(SoundSource.MASTER) * o.getSoundSourceVolume(SoundSource.RECORDS);
    }

    /** Render thread, once per frame before drawing: visibility + texture uploads. */
    private void frame(net.minecraft.client.renderer.culling.Frustum frustum) {
        for (MediaScreen s : screens.values()) {
            if (s.player == null) continue;
            boolean vis = s.id.equals(overlayId) && !ClientCompat.hudHidden()
                    || s.placed() && s.distance < 160 && frustum.isVisible(s.bounds());
            s.visible = vis;
            try {
                s.player.frame(vis);
            } catch (RuntimeException e) {
                Vibey.LOG.warn("Video frame failed: {}", e.toString());
                closePlayer(s);
            }
        }
    }

    // ============================================================================================ players

    public boolean allowed(MediaScreen s) {
        if (!s.remote) return true;
        return switch (cfg.others.get()) {
            case NEVER -> false;
            case ALWAYS -> true;
            case ASK -> s.accepted;
        };
    }

    public boolean browserConsent() {
        JsonObject d = cfg.group.data();
        return d.has("browserConsent") && d.get("browserConsent").getAsBoolean();
    }

    public void setBrowserConsent() {
        cfg.group.data().addProperty("browserConsent", true);
        cfg.group.markDirty();
    }

    /** Picture size for the decoder/browser: quality lines, the screen's aspect, even numbers. */
    public int[] pixelSize(MediaScreen s, boolean web) {
        int lines = cfg.quality.get().lines;
        float a = Math.max(0.3f, Math.min(4f, s.aspect()));
        int w = Math.round(lines * a);
        int h = lines;
        if (a < 1f) {
            w = lines;
            h = Math.round(lines / a);
        }
        if (web) { // YouTube: viewport at least 480x270 (we stay well above 200x200)
            float k = Math.max(1f, Math.max(480f / w, 270f / h));
            w = Math.round(w * k);
            h = Math.round(h * k);
        }
        w = Math.min(2560, w) & ~1;
        h = Math.min(2560, h) & ~1;
        return new int[] {w, h};
    }

    private MediaPlayer createPlayer(MediaScreen s) {
        try {
            if (s.source.kind().web()) {
                Web.State st = Web.state(browserConsent());
                if (st == Web.State.STARTING) Web.start();
                if (Web.state(browserConsent()) != Web.State.READY) return null;
                int[] px = pixelSize(s, true);
                appliedPlaying.remove(s.id);
                return Web.create(s.source, px[0], px[1], s.expectedMs(), s.playing);
            }
            String ff = Ffmpeg.find();
            if (ff == null) return null;
            int[] px = pixelSize(s, false);
            appliedPlaying.put(s.id, s.playing);
            return new FfmpegPlayer(ff, s.source, px[0], px[1], cfg.fps.get(), s.expectedMs(), s.playing);
        } catch (RuntimeException | LinkageError e) {
            Vibey.LOG.warn("Could not start a player for {}: {}", s.source.label(), e.toString());
            return null;
        }
    }

    public void closePlayer(MediaScreen s) {
        if (s.player != null) {
            try {
                s.player.close();
            } catch (RuntimeException e) {
                Vibey.LOG.debug("Closing player: {}", e.toString());
            }
        }
        s.player = null;
        s.started = false;
        s.farSince = 0;
        appliedPlaying.remove(s.id);
    }

    private void closeAllPlayers() {
        for (MediaScreen s : screens.values()) closePlayer(s);
    }

    /** One line for screens without a picture (also shown in the controls). Null when the picture is fine. */
    public String statusLine(MediaScreen s) {
        if (s.source == null) return tr("status.nosource");
        if (s.player != null && s.player.status() != MediaPlayer.Status.ERROR && s.player.texture() != null) {
            return s.player.audioOnly() ? "♪ " + s.source.label() : null;
        }
        if (s.remote && !allowed(s)) {
            return cfg.others.get() == VideoSettings.Others.NEVER ? tr("status.hidden") : tr("status.ask", s.ownerName, s.source.label());
        }
        if (s.source.kind().web()) {
            Web.State st = Web.state(browserConsent());
            switch (st) {
                case NOT_INSTALLED, NEEDS_SETUP -> {
                    return dev.henny.vibeyclient.module.media.video.web.McefInstaller.installedThisSession
                            ? tr("status.restart") : tr("status.needbrowser");
                }
                case FAILED -> {
                    return tr("status.browserfailed");
                }
                case STARTING -> {
                    float p = Web.progress();
                    return p >= 0 && p <= 100 && !Web.stage().isEmpty() && !Web.stage().equals("INITIALIZING")
                            ? tr("status.browserdownload", Math.round(p)) : tr("status.browserstart");
                }
                default -> {
                }
            }
        } else if (!Ffmpeg.available()) {
            return tr("status.needffmpeg");
        }
        if (s.player == null) return s.placed() && s.distance > cfg.distance.get() ? tr("status.far") : tr("status.loading");
        MediaPlayer.Status st = s.player.status();
        if (st == MediaPlayer.Status.ERROR) return tr("status.error", s.player.error() == null ? "" : s.player.error());
        if (s.player.audioOnly()) return "♪ " + s.source.label();
        if (s.player.texture() == null) return tr("status.loading");
        return null;
    }

    /** What the setup prompt is about for this screen, or null when nothing is missing. */
    public SetupScreen.Need missing(MediaScreen s) {
        if (s.source == null) return null;
        if (s.source.kind().web()) {
            Web.State st = Web.state(browserConsent());
            if (st == Web.State.NOT_INSTALLED) {
                return dev.henny.vibeyclient.module.media.video.web.McefInstaller.installedThisSession ? null : SetupScreen.Need.BROWSER_MOD;
            }
            if (st == Web.State.NEEDS_SETUP) return SetupScreen.Need.BROWSER_NATIVES;
            return null;
        }
        return Ffmpeg.available() ? null : SetupScreen.Need.FFMPEG;
    }

    // ============================================================================================ actions

    /** The key: controls of the screen you look at, else a new screen. */
    public void openMenu() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (placement.active()) {
            placement.cancel();
            return;
        }
        MediaScreen s = lookedAt();
        if (s != null) {
            openControls(s);
        } else {
            ClientCompat.openScreen(new SourceScreen(null, null));
        }
    }

    public void openControls(MediaScreen s) {
        if (s.remote && !allowed(s) && cfg.others.get() == VideoSettings.Others.ASK) {
            ClientCompat.openScreen(new AcceptScreen(null, s));
            return;
        }
        SetupScreen.Need need = missing(s);
        if (need != null && !s.remote) {
            ClientCompat.openScreen(new SetupScreen(new ControlsScreen(null, s), need));
            return;
        }
        ClientCompat.openScreen(new ControlsScreen(null, s));
    }

    /** The screen under the crosshair (closer than any block in the way), or null. */
    public MediaScreen lookedAt() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 dir = mc.player.getViewVector(1f);
        double block = mc.hitResult != null ? mc.hitResult.getLocation().distanceTo(eye) + 0.1 : 64;
        String dim = dimension();
        MediaScreen best = null;
        double bestT = Math.min(64, block);
        for (MediaScreen s : screens.values()) {
            if (!s.placed() || !dim.equals(s.dimension)) continue;
            double[] h = s.hit(eye, dir, bestT);
            if (h != null && h[2] <= bestT) {
                best = s;
                bestT = h[2];
            }
        }
        return best;
    }

    /** A new own screen with this source; {@code inWorld}: start placing it, else show it as the overlay. */
    public MediaScreen create(MediaSource source, float aspect, boolean inWorld) {
        Minecraft mc = Minecraft.getInstance();
        MediaScreen s = new MediaScreen(UUID.randomUUID());
        s.owner = mc.player != null ? mc.player.getUUID() : null;
        s.ownerName = ClientCompat.playerName();
        s.source = source;
        s.width = 3f;
        s.height = 3f / aspect;
        s.loop = source.kind() == MediaSource.Kind.FILE;
        s.setTimeline(true, source.youTubeStartSeconds() * 1000L);
        if (inWorld) {
            placement.start(s, false);
        } else {
            screens.put(s.id, s);
            overlayId = s.id;
            saveOwn();
        }
        return s;
    }

    /** Placement finished (new or moved screen). */
    public void placed(MediaScreen s, boolean moved) {
        placed(s, moved, true);
    }

    /** {@code prompt}: open the setup prompt right away if something is missing (interactive placing). */
    public void placed(MediaScreen s, boolean moved, boolean prompt) {
        screens.put(s.id, s);
        saveOwn();
        if (moved) closePlayer(s);
        net.sendScreen(s);
        if (!moved && prompt) {
            SetupScreen.Need need = missing(s);
            if (need != null) dev.henny.vibeyclient.api.Tasks.onMain(() -> ClientCompat.openScreen(new SetupScreen(null, need)));
        }
    }

    public void changed(MediaScreen s) {
        if (s.remote) return;
        saveOwn();
        net.sendScreen(s);
    }

    public void setSource(MediaScreen s, MediaSource source, float aspect) {
        boolean resize = Math.abs(aspect - s.aspect()) > 0.01f;
        s.source = source;
        if (resize) s.height = s.width / aspect;
        s.durationMs = -1;
        s.setTimeline(true, source.youTubeStartSeconds() * 1000L);
        closePlayer(s);
        changed(s);
    }

    public void setPlaying(MediaScreen s, boolean playing) {
        long pos = s.player != null && s.player.status() != MediaPlayer.Status.LOADING ? s.player.positionMs() : s.expectedMs();
        s.setTimeline(playing, pos);
        if (!s.remote) net.sendState(s);
    }

    public void seek(MediaScreen s, long ms) {
        s.setTimeline(s.playing, ms);
        if (s.player != null) s.player.seek(ms);
        lastCorrection.put(s.id, System.currentTimeMillis());
        if (!s.remote) net.sendState(s);
    }

    public void resize(MediaScreen s, float width) {
        float a = s.aspect();
        s.width = Math.max(0.5f, Math.min(16f, width));
        s.height = s.width / a;
        changed(s);
    }

    public void remove(MediaScreen s) {
        closePlayer(s);
        screens.remove(s.id);
        if (s.id.equals(overlayId)) overlayId = null;
        if (!s.remote) {
            net.sendRemove(s);
            saveOwn();
        }
    }

    public void removeAllOwn() {
        for (MediaScreen s : new ArrayList<>(screens.values())) if (!s.remote) remove(s);
    }

    public void toggleOverlay(MediaScreen s) {
        overlayId = s.id.equals(overlayId) ? null : s.id;
        if (overlayId == null && !s.placed()) remove(s);
    }

    public void editOverlay() {
        ClientCompat.openScreen(new OverlayEditScreen(ClientCompat.screen(), overlay));
    }

    // ============================================================================================ remote

    /** From the net: a screen of another player (already filtered by {@link UrlPolicy}). */
    public void upsertRemote(MediaScreen incoming) {
        MediaScreen s = screens.get(incoming.id);
        if (s != null && (!s.remote || !s.owner.equals(incoming.owner))) return; // someone else's id
        if (s == null) {
            screens.put(incoming.id, incoming);
            return;
        }
        boolean sourceChanged = !s.source.equals(incoming.source);
        s.dimension = incoming.dimension;
        s.x = incoming.x;
        s.y = incoming.y;
        s.z = incoming.z;
        s.normal = incoming.normal;
        s.up = incoming.up;
        s.width = incoming.width;
        s.height = incoming.height;
        s.glowing = incoming.glowing;
        s.loop = incoming.loop;
        s.ownerName = incoming.ownerName;
        s.setTimeline(incoming.playing, incoming.baseMs);
        if (sourceChanged) {
            s.source = incoming.source;
            s.accepted = false;
            s.durationMs = -1;
            closePlayer(s);
        }
    }

    public void remoteState(UUID owner, UUID id, boolean playing, long pos) {
        MediaScreen s = screens.get(id);
        if (s == null || !s.remote || !owner.equals(s.owner)) return;
        s.setTimeline(playing, pos);
    }

    public void remoteRemove(UUID owner, UUID id) {
        MediaScreen s = screens.get(id);
        if (s == null || !s.remote || !owner.equals(s.owner)) return;
        closePlayer(s);
        screens.remove(id);
        if (id.equals(overlayId)) overlayId = null;
    }

    public int countRemote(UUID owner) {
        int n = 0;
        for (MediaScreen s : screens.values()) if (s.remote && owner.equals(s.owner)) n++;
        return n;
    }

    public void forgetRemote(UUID owner) {
        for (MediaScreen s : new ArrayList<>(screens.values())) if (s.remote && owner.equals(s.owner)) remoteRemove(owner, s.id);
    }

    // ============================================================================================ persistence

    public String dimension() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? "" : mc.level.dimension().identifier().toString();
    }

    private static String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        ServerData sd = mc.getCurrentServer();
        return sd != null ? "mp:" + sd.ip.toLowerCase(java.util.Locale.ROOT) : "unknown";
    }

    private void loadOwn() {
        JsonObject all = cfg.group.data().getAsJsonObject("screens");
        if (all == null || worldKey == null || !all.has(worldKey)) return;
        UUID me = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        for (JsonElement e : all.getAsJsonArray(worldKey)) {
            try {
                MediaScreen s = MediaScreen.fromJson(e.getAsJsonObject());
                if (!s.placed()) continue; // overlays are per session
                s.owner = me;
                s.ownerName = ClientCompat.playerName();
                screens.put(s.id, s);
            } catch (RuntimeException ex) {
                Vibey.LOG.warn("Skipping a saved video screen: {}", ex.toString());
            }
        }
    }

    public void saveOwn() {
        if (worldKey == null) return;
        JsonObject data = cfg.group.data();
        JsonObject all = data.getAsJsonObject("screens");
        if (all == null) {
            all = new JsonObject();
            data.add("screens", all);
        }
        JsonArray arr = new JsonArray();
        for (MediaScreen s : screens.values()) if (!s.remote && s.placed()) arr.add(s.toJson());
        if (arr.isEmpty()) all.remove(worldKey);
        else all.add(worldKey, arr);
        cfg.group.markDirty();
    }

    // ============================================================================================ HUD

    @Override
    public void renderHud(Gfx g) {
        if (!enabled) return;
        if (!(ClientCompat.screen() instanceof OverlayEditScreen)) overlay.renderAtLayout(g);
        placement.renderHud(g);
    }

    public MediaScreen overlayScreen() {
        return overlayId == null ? null : screens.get(overlayId);
    }
}
