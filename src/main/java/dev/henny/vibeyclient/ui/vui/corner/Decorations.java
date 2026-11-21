package dev.henny.vibeyclient.ui.vui.corner;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.title.BackendStatus;
import dev.henny.vibeyclient.ui.toast.ToastHooks;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.function.Consumer;

public final class Decorations {
    private Decorations() {}

    private static Decor current;
    private static boolean hooked;

    public static void title(Screen screen, List<? extends GuiEventListener> widgets, Consumer<AbstractWidget> add) {
        TitleCorner c = new TitleCorner(screen, widgets);
        if (c.entries.isEmpty()) return;
        BackendStatus.refresh();
        install(new Decor(screen, TitleCorner.DOC, c, add));
    }

    public static void access(Screen screen, Consumer<AbstractWidget> add) {
        install(new Decor(screen, AccessHint.DOC, new AccessHint(), add));
    }

    public static void pause(Screen screen, List<? extends GuiEventListener> widgets, Consumer<AbstractWidget> add) {
        PauseRow r = new PauseRow(screen, widgets);
        if (r.main == null) return;
        install(new Decor(screen, PauseRow.DOC, r, add));
    }

    public static void tick() {
        if (current != null && ClientCompat.screen() != current.screen) {
            current.close();
            current = null;
        }
    }

    public static int[] center(String id) {
        Decor d = current;
        if (d == null) return null;
        for (ProxyButton p : d.proxies()) {
            if (p.id.equals(id)) return new int[] {p.getX() + p.getWidth() / 2, p.getY() + p.getHeight() / 2};
        }
        return null;
    }

    private static void install(Decor d) {
        if (current != null) current.close();
        current = d;
        if (hooked) return;
        hooked = true;
        ToastHooks.addScreenOverlay((screen, g, mx, my) -> {
            Decor now = current;
            if (now == null || now.screen != screen) return;
            try {
                now.render(g.g, mx, my);
            } catch (RuntimeException e) {
                Vibey.LOG.error("Screen decoration failed", e);
            }
        });
    }
}
