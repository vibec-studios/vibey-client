package dev.henny.vibeyclient.module.hud.tab;

import dev.henny.vibeyclient.ui.Gfx;

final class PingBars {
    private PingBars() {}

    static final int WIDTH = 11;

    static int level(int ping) {
        if (ping < 0) return 0;
        if (ping < 75) return 5;
        if (ping < 150) return 4;
        if (ping < 300) return 3;
        if (ping < 600) return 2;
        return 1;
    }

    static int color(int level) {
        return switch (level) {
            case 5, 4 -> 0xFF5BE37D;
            case 3 -> 0xFFFFD166;
            case 2, 1 -> 0xFFFF5470;
            default -> 0xFF6B6B75;
        };
    }

    static void draw(Gfx g, int x, int y, int ping) {
        int level = level(ping);
        int on = color(level);
        for (int i = 0; i < 5; i++) {
            int h = 2 + i;
            int c = i < level ? on : 0x55FFFFFF;
            g.fill(x + i * 2, y + 7 - h, x + i * 2 + 1, y + 7, c);
        }
    }
}
