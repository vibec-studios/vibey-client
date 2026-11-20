package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.hud.HudIcons;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

final class TextWidgets {
    private TextWidgets() {}

    static String l(String key) {
        return Lang.tr(key);
    }

    static Minecraft mc() {
        return Minecraft.getInstance();
    }

    abstract static class Base extends TextWidget {
        private final String icon;

        Base(String icon) {
            this.icon = icon;
        }

        @Override
        protected String icon(HudPaint p) {
            return p.flag("icon") ? icon : null;
        }
    }

    static final class Fps extends Base {
        private final int[] samples = new int[24];
        private int n;

        Fps() {
            super(HudIcons.FPS);
        }

        @Override
        protected void lines(HudPaint p) {
            int fps = mc().getFps();
            samples[n++ % samples.length] = fps;
            int low = Integer.MAX_VALUE;
            for (int i = 0; i < Math.min(n, samples.length); i++) low = Math.min(low, samples[i]);
            if (p.detailed()) line(l("hud.label.fps"), fps + "  " + l("hud.label.low") + " " + low);
            else line(l("hud.label.fps"), Integer.toString(fps));
        }
    }

    static final class Cps extends Base {
        Cps() {
            super(HudIcons.CPS);
        }

        @Override
        protected int rate() {
            return 10;
        }

        @Override
        protected void lines(HudPaint p) {
            boolean world = mc().level != null;
            int l = p.preview && !world ? 9 : Clicks.left();
            int r = p.preview && !world ? 4 : Clicks.right();
            String which = p.option("buttons");
            String v = switch (which) {
                case "left" -> Integer.toString(l);
                case "right" -> Integer.toString(r);
                default -> l + " | " + r;
            };
            line(l("hud.label.cps"), p.detailed() && which.equals("both") ? l("hud.label.lmb") + " " + l + "  " + l("hud.label.rmb") + " " + r : v);
        }
    }

    static final class Coords extends Base {
        Coords() {
            super(HudIcons.COORDS);
        }

        @Override
        protected int rate() {
            return 10;
        }

        @Override
        protected void lines(HudPaint p) {
            Player pl = mc().player;
            double x = pl != null ? pl.getX() : 128.5, y = pl != null ? pl.getY() : 64, z = pl != null ? pl.getZ() : -302.4;
            float yaw = pl != null ? pl.getYRot() : 180f;
            if (pl == null && !p.preview) return;
            boolean facing = p.flag("facing"), biome = p.flag("biome");
            if (p.detailed()) {
                line(l("hud.label.x"), String.format(Locale.ROOT, "%.1f", x));
                line(l("hud.label.y"), String.format(Locale.ROOT, "%.1f", y));
                line(l("hud.label.z"), String.format(Locale.ROOT, "%.1f", z));
                if (facing) line(l("hud.label.facing"), Directions.full(yaw));
                if (biome) line(l("hud.label.biome"), biomeName(pl));
            } else {
                String v = (int) Math.floor(x) + " " + (int) Math.floor(y) + " " + (int) Math.floor(z);
                if (facing) v += "  " + Directions.shortName(yaw);
                line(l("hud.label.xyz"), v);
                if (biome) line(l("hud.label.biome"), biomeName(pl));
            }
        }

        private static String biomeName(Player pl) {
            if (pl == null || mc().level == null) return I18n.get("biome.minecraft.cherry_grove");
            try {
                return mc().level.getBiome(BlockPos.containing(pl.getX(), pl.getY(), pl.getZ())).unwrapKey()
                        .map(k -> I18n.get("biome." + k.identifier().getNamespace() + "." + k.identifier().getPath()))
                        .orElse("?");
            } catch (RuntimeException e) {
                return "?";
            }
        }
    }

    static final class Clock extends Base {
        Clock() {
            super(HudIcons.CLOCK);
        }

        @Override
        protected int rate() {
            return 2;
        }

        @Override
        protected void lines(HudPaint p) {
            LocalDateTime now = LocalDateTime.now();
            boolean h12 = "12h".equals(p.option("format"));
            String pattern = (h12 ? "h:mm" : "HH:mm") + (p.flag("seconds") ? ":ss" : "") + (h12 ? " a" : "");
            String t = now.format(DateTimeFormatter.ofPattern(pattern, Locale.ROOT));
            if (p.detailed()) {
                Locale loc = Lang.german() ? Locale.GERMAN : Locale.ENGLISH;
                String day = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, loc);
                String date = now.format(DateTimeFormatter.ofPattern(Lang.german() ? "d. MMM" : "MMM d", loc));
                line(null, day + " " + date + "  " + t);
            } else {
                line(null, t);
            }
        }
    }

    static final class Ping extends Base {
        Ping() {
            super(HudIcons.PING);
        }

        @Override
        protected int rate() {
            return 1;
        }

        @Override
        protected void lines(HudPaint p) {
            Minecraft mc = mc();
            int ms = -1;
            if (mc.player != null && mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info != null) ms = info.getLatency();
            }
            if (ms < 0) {
                if (!p.preview) return;
                ms = 23;
            }
            String v = ms + " " + l("hud.label.ms");
            if (p.detailed()) v += "  " + l(ms < 60 ? "hud.label.ping.good" : ms < 150 ? "hud.label.ping.ok" : "hud.label.ping.bad");
            line(l("hud.label.ping"), v);
        }
    }

    static final class Server extends Base {
        Server() {
            super(HudIcons.SERVER);
        }

        @Override
        protected int rate() {
            return 1;
        }

        @Override
        protected void lines(HudPaint p) {
            Minecraft mc = mc();
            ServerData d = mc.getCurrentServer();
            boolean hideIp = p.flag("hideIp");
            String v;
            if (d != null) {
                String name = d.name == null || d.name.isBlank() ? null : d.name;
                if (d.isLan()) v = l("hud.label.lan");
                else if (hideIp) v = name != null ? name : l("hud.label.server.hidden");
                else if (p.detailed() && name != null) v = name + "  " + d.ip;
                else v = d.ip;
            } else if (mc.hasSingleplayerServer() || mc.level != null) {
                v = l("hud.label.singleplayer");
            } else if (p.preview) {
                v = hideIp ? "Vibey SMP" : "play.example.net";
            } else {
                return;
            }
            line(p.detailed() ? l("hud.label.server") : null, v);
        }
    }

    static final class Speed extends Base {
        Speed() {
            super(HudIcons.SPEED);
        }

        @Override
        protected int rate() {
            return 8;
        }

        @Override
        protected void lines(HudPaint p) {
            if (mc().player == null && !p.preview) return;
            double v = p.flag("vertical") ? Motion.full() : Motion.horizontal();
            if (mc().player == null) v = 5.61;
            String s = String.format(Locale.ROOT, "%.2f", v) + " " + l("hud.label.bps");
            line(p.detailed() ? l("hud.label.speed") : null, s);
        }
    }

    static final class GameTime extends Base {
        GameTime() {
            super(HudIcons.TIME);
        }

        @Override
        protected int rate() {
            return 2;
        }

        @Override
        protected void lines(HudPaint p) {
            long t;
            if (mc().level != null) t = mc().level.getLevelData().getDayTime();
            else if (p.preview) t = 6000 + 24000L * 11;
            else return;
            long day = t / 24000L + 1;
            int ticks = (int) (t % 24000L);
            int minutes = (int) ((ticks / 1000.0 * 60 + 6 * 60) % (24 * 60));
            String clock = String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
            if (p.detailed()) line(l("hud.label.day") + " " + day, clock);
            else line(null, clock);
        }
    }

    static final class Memory extends Base {
        Memory() {
            super(HudIcons.MEMORY);
        }

        @Override
        protected int rate() {
            return 1;
        }

        @Override
        protected void lines(HudPaint p) {
            Runtime r = Runtime.getRuntime();
            long used = r.totalMemory() - r.freeMemory(), max = r.maxMemory();
            int pct = (int) Math.round(used * 100.0 / Math.max(1, max));
            if (p.detailed()) {
                line(l("hud.label.mem"), pct + "%  " + String.format(Locale.ROOT, "%.1f / %.1f GB", used / 1e9, max / 1e9));
            } else {
                line(l("hud.label.mem"), pct + "%");
            }
        }
    }
}
