package dev.henny.vibeyclient.module.clips.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.clips.ClipSaver;
import dev.henny.vibeyclient.module.clips.ClipsFiles;
import dev.henny.vibeyclient.module.clips.ClipsModule;
import dev.henny.vibeyclient.module.clips.ClipsPages;
import dev.henny.vibeyclient.module.clips.store.ClipStore;
import dev.henny.vibeyclient.ui.CardScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The in-game gallery (docs/CLIPS.md §8): thumbnails newest first, filter All / Kept / Auto / Hotkey, storage bar.
 * A tile opens the {@link ClipViewScreen}. The folder is rescanned when the gallery opens and every few seconds, so
 * clips the launcher changed show up too.
 */
public final class ClipsGalleryScreen extends CardScreen {
    public enum Filter { ALL, KEPT, AUTO, HOTKEY }

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());

    private static Filter lastFilter = Filter.ALL;
    private final Thumbs thumbs = new Thumbs();
    private volatile List<ClipStore.Clip> clips;
    private volatile long total = -1;
    private long lastScan;
    private Filter filter = lastFilter;

    public ClipsGalleryScreen(Screen parent) {
        super(Component.literal(Lang.tr("clips.gallery.title")), parent, "clips.gallery.");
        rescan();
    }

    /** Opens the gallery over {@code parent} (null = over the game). */
    public static void open(Screen parent) {
        ClientCompat.openScreen(new ClipsGalleryScreen(parent));
    }

    /** Opens one clip directly (toast "View"); the gallery is its parent. */
    public static void openClip(Screen parent, String id) {
        ClipsGalleryScreen g = new ClipsGalleryScreen(parent);
        ClipsFiles.io(() -> {
            ClipStore.Clip c = ClipsFiles.store().get(id);
            if (c != null) dev.henny.vibeyclient.api.Tasks.onMain(() -> ClientCompat.openScreen(new ClipViewScreen(g, c)));
        });
        ClientCompat.openScreen(g);
    }

    void rescan() {
        lastScan = System.currentTimeMillis();
        ClipsFiles.io(() -> {
            List<ClipStore.Clip> all = ClipsFiles.store().list();
            long t = 0;
            for (ClipStore.Clip c : all) t += c.filesBytes();
            clips = all;
            total = t;
        });
    }

    /** The clips currently shown (tests). */
    public List<ClipStore.Clip> shown() {
        List<ClipStore.Clip> all = clips;
        List<ClipStore.Clip> out = new ArrayList<>();
        if (all == null) return out;
        for (ClipStore.Clip c : all) {
            boolean ok = switch (filter) {
                case ALL -> true;
                case KEPT -> c.kept();
                case AUTO -> c.auto();
                case HOTKEY -> !c.auto();
            };
            if (ok) out.add(c);
        }
        return out;
    }

    public void filter(Filter f) {
        filter = f;
        lastFilter = f;
        scroll = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    protected int cardWidth() {
        return 560;
    }

    @Override
    protected int fixedHeight() {
        return height - 30;
    }

    @Override
    protected int headerHeight() {
        return 58;
    }

    @Override
    protected int footerHeight() {
        return 40;
    }

    @Override
    protected void drawHeader(Gfx g, int x, int y, int w) {
        if (System.currentTimeMillis() - lastScan > 3000) rescan();
        title(g, tr("title"), x + PAD, y + 13);
        List<ClipStore.Clip> all = clips;
        if (all != null) g.text(tr("count", all.size()), x + PAD + g.boldWidth(tr("title")) + 8, y + 13, DIM);
        closeButton(g, x, y, w);
        String[] labels = {tr("filter.all"), tr("filter.kept"), tr("filter.auto"), tr("filter.hotkey")};
        segmented(g, x + PAD, y + 30, Math.min(300, w - PAD * 2), labels, filter.ordinal(), i -> filter(Filter.values()[i]));
    }

    @Override
    protected int drawBody(Gfx g, int x, int y, int w) {
        List<ClipStore.Clip> list = shown();
        if (clips == null) {
            g.text(tr("loading") + dots(), x, y + 8, MUTED);
            return y + 30;
        }
        if (list.isEmpty()) {
            ClipsModule m = ClipsModule.get();
            String key = m == null ? "?" : ClientCompat.keyName(m.saveKey.mapping()).getString();
            String text = filter == Filter.ALL ? tr("empty", key) : tr("emptyFilter");
            int yy = y + 10;
            for (String line : wrap(g, text, w)) {
                g.textCenter(line, x + w / 2, yy, MUTED);
                yy += 11;
            }
            if (m != null && !m.buffer.on() && filter == Filter.ALL) {
                int bw = g.width(tr("turnOn")) + 14;
                button(g, x + (w - bw) / 2, yy + 6, bw, 18, tr("turnOn"), Style.PRIMARY, () -> {
                    m.buffer.set(true);
                });
                yy += 30;
            }
            return yy + 10;
        }
        int cols = w >= 470 ? 3 : 2;
        int gap = 8;
        int tw = (w - gap * (cols - 1)) / cols;
        int th = tw * 9 / 16;
        int rowH = th + 30;
        int i = 0;
        for (ClipStore.Clip c : list) {
            int col = i % cols, row = i / cols;
            int tx = x + col * (tw + gap);
            int ty = y + row * (rowH + gap);
            tile(g, c, tx, ty, tw, th);
            i++;
        }
        int rows = (list.size() + cols - 1) / cols;
        return y + rows * (rowH + gap);
    }

    private void tile(Gfx g, ClipStore.Clip c, int x, int y, int w, int th) {
        boolean hov = hover(x, y, w, th + 30);
        g.rect(x, y, w, th + 30, hov ? SURFACE_HOVER : SURFACE);
        g.outline(x, y, w, th + 30, hov ? Theme.alpha(ACCENT, 0.8f) : Theme.LINE2);
        // only visible tiles load their thumbnail
        boolean visible = y + th > 0 && y < height;
        Thumbs.Tex t = visible ? thumbs.get(c.id(), c.thumbnail()) : null;
        if (t != null) {
            g.image(t.id(), x + 1, y + 1, w - 2, th - 1, 0, 0, t.w(), t.h(), t.w(), t.h());
        } else {
            g.rect(x + 1, y + 1, w - 2, th - 1, 0xFF15151B);
            g.textCenter(c.thumbnail() == null ? "▶" : dots(), x + w / 2, y + th / 2 - 4, DIM);
        }
        // duration badge
        String dur = ClipSaver.duration(c.durationMs());
        int dw = g.width(dur) + 6;
        g.rect(x + w - dw - 4, y + th - 14, dw, 11, 0xC0000000);
        g.text(dur, x + w - dw - 1, y + th - 12, 0xFFFFFFFF);
        // reason badge
        String reason = Lang.tr("clips.reason." + ClipSaver.reasonKey(c.reason()));
        int rw = g.width(reason) + 6;
        g.rect(x + 4, y + 4, rw, 11, c.auto() ? Theme.alpha(0xFFFF6B4A, 0.9f) : 0xC0000000);
        g.text(reason, x + 7, y + 6, 0xFFFFFFFF);
        if (c.kept()) Icons.draw(g, Icons.STAR, x + w - 14, y + 4, Theme.WARN);
        String name = c.title() != null ? c.title() : where(c);
        g.text(g.clip(name, w - 10), x + 5, y + th + 5, TEXT);
        g.text(g.clip(DATE.format(c.createdAt()), w - 10), x + 5, y + th + 17, DIM);
        hit(x, y, w, th + 30, () -> ClientCompat.openScreen(new ClipViewScreen(this, c)));
    }

    /** "play.example.net", the world name, or the reason. */
    static String where(ClipStore.Clip c) {
        String s = c.str("server");
        if (s == null) s = c.str("world");
        if (s == null) s = Lang.tr("clips.reason." + ClipSaver.reasonKey(c.reason()));
        return s;
    }

    @Override
    protected void drawFooter(Gfx g, int x, int y, int w) {
        int by = y + 11;
        ClipsModule m = ClipsModule.get();
        long limit = m == null ? 0 : m.folderLimitBytes();
        long used = total;
        int barW = Math.min(180, w / 3);
        if (used >= 0) {
            String txt = tr("storage", gb(used), gb(limit));
            g.text(txt, x + PAD, by - 1, MUTED);
            bar(g, x + PAD, by + 11, barW, 3, limit <= 0 ? 0 : (float) used / limit, null);
        }
        int right = x + w - PAD;
        int left = buttonRight(g, right, by, 18, tr("folder"), Style.SECONDARY, ClipsFiles::openFolder);
        buttonRight(g, left - 6, by, 18, tr("settings"), Style.SECONDARY, ClipsPages::openSettings);
    }

    static String gb(long bytes) {
        double gb = bytes / (1024.0 * 1024 * 1024);
        if (gb < 0.1) return String.format(Locale.ROOT, "%d MB", Math.round(bytes / 1048576.0));
        return String.format(Locale.ROOT, "%.1f GB", gb);
    }

    void forget(String id) {
        thumbs.drop(id);
    }

    @Override
    public void onClose() {
        thumbs.close();
        super.onClose();
    }
}
