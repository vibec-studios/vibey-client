package dev.henny.vibeyclient.module.inventory.click;

import dev.henny.vibeyclient.module.inventory.core.BoundedQueue;
import dev.henny.vibeyclient.module.inventory.core.Click;
import dev.henny.vibeyclient.module.inventory.core.ClickPacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

import java.util.ArrayDeque;
import java.util.List;

public final class ClickSender {
    public static final int MAX_INTENTS = 32;

    private static final ClickPacer PACER = new ClickPacer(ClickPacer.DEFAULT_RATE);
    private static final BoundedQueue<Intent> INTENTS = new BoundedQueue<>(MAX_INTENTS);
    private static final ArrayDeque<Click> PENDING = new ArrayDeque<>();
    private static Intent current;
    private static int containerId = -1;
    private static long sent;

    private ClickSender() {}

    public static void rate(int perSecond) {
        PACER.rate(perSecond);
    }

    public static boolean offer(Intent intent) {
        return INTENTS.offer(intent);
    }

    public static boolean offer(Intent intent, int maxQueued) {
        return INTENTS.size() < maxQueued && INTENTS.offer(intent);
    }

    public static boolean busy() {
        return !PENDING.isEmpty() || current != null || !INTENTS.isEmpty();
    }

    public static boolean midPlan() {
        return !PENDING.isEmpty();
    }

    public static int queued() {
        return INTENTS.size() + (current == null ? 0 : 1);
    }

    public static long sent() {
        return sent;
    }

    public static void clear() {
        PENDING.clear();
        INTENTS.clear();
        current = null;
        containerId = -1;
    }

    public static void pump() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) {
            clear();
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (!PENDING.isEmpty() && menu.containerId != containerId) {
            clear();
            return;
        }
        long now = System.nanoTime();
        if (!PACER.ready(now)) return;
        if (PENDING.isEmpty()) refill(menu);
        Click c = PENDING.poll();
        if (c == null) return;
        PACER.take(now);
        sent++;
        mc.gameMode.handleInventoryMouseClick(containerId, c.slot(), c.button(), type(c), player);
    }

    private static void refill(AbstractContainerMenu menu) {
        for (int guard = 0; guard < MAX_INTENTS * 2 && PENDING.isEmpty(); guard++) {
            if (current == null) current = INTENTS.poll();
            if (current == null) return;
            List<Click> next = current.valid(menu) ? current.next(menu) : List.of();
            if (next.isEmpty()) {
                current = null;
                continue;
            }
            PENDING.addAll(next);
            containerId = menu.containerId;
        }
    }

    private static ClickType type(Click c) {
        return switch (c.kind()) {
            case PICKUP -> ClickType.PICKUP;
            case QUICK_MOVE -> ClickType.QUICK_MOVE;
            case SWAP -> ClickType.SWAP;
        };
    }
}
