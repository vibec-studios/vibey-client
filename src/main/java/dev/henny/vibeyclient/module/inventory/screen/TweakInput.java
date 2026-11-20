package dev.henny.vibeyclient.module.inventory.screen;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.inventory.InventoryModule;
import dev.henny.vibeyclient.module.inventory.click.ClickSender;
import dev.henny.vibeyclient.module.inventory.click.MenuSlots;
import dev.henny.vibeyclient.module.inventory.click.QuickMoveIntent;
import dev.henny.vibeyclient.module.inventory.click.WheelIntent;
import dev.henny.vibeyclient.module.inventory.core.DragTracker;
import dev.henny.vibeyclient.module.inventory.mixin.ContainerScreenAccess;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

final class TweakInput {
    static final int MAX_WHEEL_QUEUED = 3;

    private final InventoryModule m;
    private final DragTracker drag = new DragTracker();

    TweakInput(InventoryModule m) {
        this.m = m;
    }

    void press(AbstractContainerScreen<?> screen, int button, boolean shift) {
        drag.stop();
        if (!m.tweaksActive() || !m.dragMove.on() || button != 0 || !shift) return;
        AbstractContainerMenu menu = screen.getMenu();
        Slot slot = hovered(screen);
        if (slot == null || !slot.hasItem() || !menu.getCarried().isEmpty()) return;
        drag.start(slot.index, MenuSlots.key(slot.getItem()), m.dragMatching.on());
    }

    void release() {
        drag.stop();
    }

    void frame(AbstractContainerScreen<?> screen) {
        if (!drag.active()) return;
        if (!m.tweaksActive() || !ClientCompat.shiftDown()) {
            drag.stop();
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        Slot slot = hovered(screen);
        if (slot == null || !menu.getCarried().isEmpty()) return;
        if (drag.visit(MenuSlots.view(menu, slot, slot.getItem()))) ClickSender.offer(new QuickMoveIntent(menu.containerId, slot.index));
    }

    boolean scroll(AbstractContainerScreen<?> screen, double dy) {
        if (!m.tweaksActive() || !m.wheelMove.on() || dy == 0) return false;
        AbstractContainerMenu menu = screen.getMenu();
        Slot slot = hovered(screen);
        if (slot == null || !slot.hasItem() || !menu.getCarried().isEmpty()) return false;
        if (slot.getItem().has(DataComponents.BUNDLE_CONTENTS)) return false;
        boolean push = (dy < 0) != m.wheelInvert.on();
        ClickSender.offer(new WheelIntent(menu.containerId, slot.index, push, ClientCompat.shiftDown()), MAX_WHEEL_QUEUED);
        return true;
    }

    boolean dragging() {
        return drag.active();
    }

    void stop() {
        drag.stop();
    }

    private static Slot hovered(AbstractContainerScreen<?> screen) {
        return ((ContainerScreenAccess) screen).vibey$hoveredSlot();
    }
}
