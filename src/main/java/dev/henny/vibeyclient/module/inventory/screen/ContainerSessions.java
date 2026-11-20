package dev.henny.vibeyclient.module.inventory.screen;

import dev.henny.vibeyclient.module.inventory.InventoryModule;
import dev.henny.vibeyclient.module.inventory.search.SlotHighlight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Map;
import java.util.WeakHashMap;

public final class ContainerSessions {
    private static final Map<AbstractContainerScreen<?>, ContainerSession> OPEN = new WeakHashMap<>();

    private ContainerSessions() {}

    static ContainerSession open(InventoryModule m, AbstractContainerScreen<?> screen) {
        return OPEN.computeIfAbsent(screen, s -> new ContainerSession(m, s));
    }

    static void close(AbstractContainerScreen<?> screen) {
        ContainerSession s = OPEN.remove(screen);
        if (s != null) s.close();
    }

    public static ContainerSession current() {
        return Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> s ? OPEN.get(s) : null;
    }

    public static void afterSlots(AbstractContainerScreen<?> screen, GuiGraphics g) {
        ContainerSession s = OPEN.get(screen);
        if (s != null && s.searching()) SlotHighlight.draw(g, screen.getMenu());
    }

    public static boolean typed(int codepoint) {
        ContainerSession s = current();
        return s != null && s.typed(codepoint);
    }
}
