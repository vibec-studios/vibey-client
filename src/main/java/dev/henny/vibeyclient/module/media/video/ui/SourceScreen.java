package dev.henny.vibeyclient.module.media.video.ui;

import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.MediaSource;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.ui.CardScreen;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

/** "New screen" / "Change source": a link or file, the picture shape, then place it or show it as overlay. */
public final class SourceScreen extends CardScreen {
    private static final String[] ASPECT_LABELS = {"16:9", "4:3", "1:1", "21:9", "9:16"};
    private static final float[] ASPECTS = {16f / 9f, 4f / 3f, 1f, 21f / 9f, 9f / 16f};
    private final MediaScreen existing;
    private EditBox url;
    private int aspect;
    private boolean picking;

    public SourceScreen(Screen parent, MediaScreen existing) {
        super(Component.literal(Video.tr(existing == null ? "source.new" : "source.change")), parent, "media.video.");
        this.existing = existing;
        if (existing != null) {
            float a = existing.aspect();
            for (int i = 0; i < ASPECTS.length; i++) if (Math.abs(ASPECTS[i] - a) < 0.02f) aspect = i;
        }
    }

    @Override
    protected void init() {
        super.init();
        if (url == null) {
            url = new EditBox(font, 0, 0, 200, 12, Component.literal(Video.tr("source.url")));
            url.setMaxLength(MediaSource.MAX_URI);
            url.setHint(Component.literal(Video.tr("source.hint")));
            if (existing != null && existing.source != null) url.setValue(existing.source.uri());
        }
        input(url, () -> confirm(true));
        setInitialFocus(url);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected int cardWidth() {
        return 330;
    }

    @Override
    protected int headerHeight() {
        return 34;
    }

    @Override
    protected int footerHeight() {
        return 40;
    }

    @Override
    protected void drawHeader(Gfx g, int x, int y, int w) {
        title(g, Video.tr(existing == null ? "source.new" : "source.change"), x + PAD, y + 13);
        closeButton(g, x, y, w);
    }

    @Override
    protected int drawBody(Gfx g, int x, int y, int w) {
        y = section(g, tr("source.url"), x, y);
        int bw = g.width(tr("source.file")) + 14;
        inputBox(g, url, x, y, w - bw - 6);
        button(g, x + w - bw, y, bw, 16, tr("source.file"), picking ? Style.DISABLED : Style.SECONDARY, this::pickFile);
        y += 22;
        MediaSource src = MediaSource.parse(url.getValue());
        String kind = url.getValue().isBlank() ? tr("source.kind.empty")
                : src == null ? tr("source.kind.unknown") : tr("source.kind." + src.kind().name().toLowerCase(java.util.Locale.ROOT));
        g.text(g.clip(kind, w), x, y, src == null ? DIM : MUTED);
        y += 12;
        if (src != null && src.kind() == MediaSource.Kind.FILE) {
            g.text(g.clip(tr("source.private"), w), x, y, DIM);
            y += 12;
        }
        y += 8;
        y = section(g, tr("source.shape"), x, y);
        y = segmented(g, x, y, w, ASPECT_LABELS, aspect, i -> aspect = i) + 8;
        return y;
    }

    @Override
    protected void drawFooter(Gfx g, int x, int y, int w) {
        boolean ok = MediaSource.parse(url.getValue()) != null;
        int by = y + 11;
        if (existing == null) {
            int left = buttonRight(g, x + w - PAD, by, 18, tr("source.place"), ok ? Style.PRIMARY : Style.DISABLED, () -> confirm(true));
            buttonRight(g, left - 6, by, 18, tr("source.overlay"), ok ? Style.SECONDARY : Style.DISABLED, () -> confirm(false));
        } else {
            buttonRight(g, x + w - PAD, by, 18, tr("source.apply"), ok ? Style.PRIMARY : Style.DISABLED, () -> confirm(true));
        }
    }

    private void confirm(boolean inWorld) {
        MediaSource src = MediaSource.parse(url.getValue());
        if (src == null) return;
        Video v = Video.get();
        if (existing != null) {
            v.setSource(existing, src, ASPECTS[aspect]);
            v.openControls(existing);
            return;
        }
        MediaScreen s = v.create(src, ASPECTS[aspect], inWorld);
        if (!inWorld) {
            SetupNeed need = v.missing(s);
            Minecraft.getInstance().setScreen(need != null ? SetupScreens.of(null, need) : null);
        }
    }

    private void pickFile() {
        picking = true;
        Thread t = new Thread(() -> {
            String path;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                String[] pats = {"*.mp4", "*.webm", "*.mkv", "*.mov", "*.avi", "*.m4v", "*.mp3", "*.ogg", "*.wav", "*.flac", "*.gif"};
                PointerBuffer filters = stack.mallocPointer(pats.length);
                for (String p : pats) filters.put(stack.UTF8(p));
                filters.flip();
                path = TinyFileDialogs.tinyfd_openFileDialog(tr("source.file"), null, filters, tr("source.filetypes"), false);
            } catch (Throwable e) {
                path = null;
            }
            String chosen = path;
            Minecraft.getInstance().execute(() -> {
                picking = false;
                if (chosen != null && url != null) url.setValue(chosen);
            });
        }, "Vibey video file picker");
        t.setDaemon(true);
        t.start();
    }
}
