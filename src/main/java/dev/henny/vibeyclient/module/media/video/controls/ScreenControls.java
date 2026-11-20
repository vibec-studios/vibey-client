package dev.henny.vibeyclient.module.media.video.controls;

import dev.henny.r3.object.VideoOverlay;
import dev.henny.r3.object.VideoSurface;
import dev.henny.r3.vui.VuiPanelContent;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.MediaSource;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.module.media.video.ui.SetupNeed;
import dev.henny.vibeyclient.module.media.video.ui.SetupScreens;
import dev.henny.vibeyclient.module.media.video.web.Web;
import dev.henny.vibeyclient.module.media.video.world.ScreenObjects;
import dev.henny.vui.bind.State;

import java.util.List;

public final class ScreenControls {
    public static final String URL = "url";
    public static final String MORE = "more";
    public static final String GATE = "gate";

    public final State<String> mode = new State<>("");
    public final State<String> url = new State<>("");
    public final State<String> time = new State<>("0:00");
    public final State<String> shape = new State<>(Shapes.label(16f / 9f));
    public final State<Float> progress = new State<>(0f);
    public final State<Float> volume = new State<>(1f);
    public final State<Boolean> playing = new State<>(false);
    public final State<Boolean> seekable = new State<>(false);
    public final State<Boolean> remote = new State<>(false);
    public final State<Boolean> locked = new State<>(false);
    public final State<Boolean> glow = new State<>(true);
    public final State<Boolean> loop = new State<>(true);
    public final State<Boolean> shared = new State<>(true);
    public final State<Boolean> canShare = new State<>(false);
    public final State<Boolean> overlay = new State<>(false);
    public final State<Boolean> interactive = new State<>(false);
    public final State<Boolean> needsAccept = new State<>(false);
    public final State<Boolean> needsSetup = new State<>(false);
    public final List<String> shapes = Shapes.LABELS;

    private final MediaScreen screen;
    private final ScreenState state;
    private VuiPanelContent strip;
    private boolean focusUrl;

    public ScreenControls(MediaScreen screen) {
        this.screen = screen;
        this.state = new ScreenState(this, screen);
    }

    public static ScreenControls of(MediaScreen s) {
        VuiPanelContent strip = ScreenObjects.strip(s.object);
        return strip != null && strip.document().controller() instanceof ScreenControls c ? c : null;
    }

    public void attach(VuiPanelContent strip) {
        this.strip = strip;
    }

    public String t(String key) {
        return Video.tr(key);
    }

    public void refresh() {
        state.refresh(video());
        if (focusUrl && strip != null && strip.document().find(URL) != null) {
            focusUrl = false;
            strip.focus(URL);
        }
    }

    public void toggle() {
        video().actions.setPlaying(screen, !screen.playing());
    }

    public void stop() {
        video().actions.stop(screen);
    }

    public void back() {
        video().actions.skip(screen, -1);
    }

    public void next() {
        video().actions.skip(screen, 1);
    }

    public void seekTo(Object value) {
        long d = screen.timeline.durationMs();
        if (d > 0 && value instanceof Number n) video().actions.seek(screen, (long) (n.doubleValue() * d));
    }

    public void setVolume(Object value) {
        if (!(value instanceof Number n)) return;
        screen.volume = Math.max(0f, Math.min(1f, n.floatValue()));
        video().actions.changed(screen);
    }

    public void editSource() {
        url.set(screen.source == null ? "" : screen.source.uri());
        switchMode(URL);
        focusUrl = true;
    }

    public void applyUrl() {
        MediaSource src = MediaSource.parse(url.peek());
        if (src == null) return;
        video().actions.setSource(screen, src);
        closeMode();
    }

    public void pickFile() {
        FilePicker.pick(t("source.file"), t("source.filetypes"), path -> {
            url.set(path);
            applyUrl();
        });
    }

    public void openMore() {
        switchMode(MORE.equals(mode.peek()) ? "" : MORE);
    }

    public void closeMode() {
        if (strip != null) strip.releaseKeyFocus();
        switchMode("");
    }

    public void toggleLock() {
        video().actions.toggleLock(screen);
    }

    public void close() {
        if (screen.remote) video().actions.unload(screen);
        else video().actions.remove(screen);
    }

    public void toggleGlow() {
        screen.glowing = !screen.glowing;
        video().actions.changed(screen);
    }

    public void toggleLoop() {
        screen.loop = !screen.loop;
        video().actions.changed(screen);
    }

    public void toggleShare() {
        screen.shared = !screen.shared;
        video().actions.changed(screen);
    }

    public void toggleOverlay() {
        video().actions.toggleOverlay(screen);
    }

    public void setShape(Object label) {
        video().actions.setShape(screen, Shapes.aspect(String.valueOf(label)));
    }

    public void usePage() {
        if (screen.player != null && screen.player.interactive() && Web.installed()) Web.openBrowser(null, screen.player);
    }

    public void load() {
        screen.accepted = true;
    }

    public void setup() {
        SetupNeed need = video().status.missing(screen);
        if (need != null) ClientCompat.openScreen(SetupScreens.of(null, need));
    }

    void switchMode(String next) {
        mode.set(next);
        VideoSurface surface = ScreenObjects.surface(screen.object);
        VideoOverlay o = surface == null ? null : surface.overlay();
        if (o != null) o.pinned(!next.isEmpty() && !GATE.equals(next));
    }

    private static Video video() {
        return Video.get();
    }
}
