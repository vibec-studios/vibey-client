package dev.henny.vibeyclient.module.screens.apply;

import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.mixin.ScreenAccess;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class WidgetScan {
    static final int MAX = 300;

    private WidgetScan() {}

    public static List<AbstractWidget> scan(Screen screen) {
        Set<AbstractWidget> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<AbstractWidget> out = new ArrayList<>();
        for (GuiEventListener l : screen.children()) visit(l, seen, out, 0);
        for (Renderable r : ((ScreenAccess) screen).vibey$renderables()) {
            if (r instanceof AbstractWidget w) add(w, seen, out);
        }
        return out;
    }

    private static void visit(GuiEventListener l, Set<AbstractWidget> seen, List<AbstractWidget> out, int depth) {
        if (out.size() >= MAX || depth > 3) return;
        if (l instanceof AbstractWidget w) {
            add(w, seen, out);
            return;
        }
        if (l instanceof ContainerEventHandler c && WidgetTypes.container(l)) {
            for (GuiEventListener child : c.children()) visit(child, seen, out, depth + 1);
        }
    }

    private static void add(AbstractWidget w, Set<AbstractWidget> seen, List<AbstractWidget> out) {
        if (out.size() >= MAX || ignored(w) || !seen.add(w)) return;
        out.add(w);
    }

    public static List<Box> taken(ScreenSession s) {
        List<Box> out = new ArrayList<>();
        if (s.panel != null) out.add(s.panel.visual);
        for (GuiEventListener l : s.screen.children()) {
            if (l instanceof AbstractWidget w && w.visible) out.add(Rects.of(w));
        }
        for (Item i : s.items) if (!i.hidden && i.shown()) out.add(i.visual);
        return out;
    }

    static boolean ignored(AbstractWidget w) {
        String n = w.getClass().getName();
        return n.startsWith("dev.henny.vibeyclient.ui.vui.corner.") || n.startsWith("dev.henny.vibeyclient.module.screens.");
    }
}
