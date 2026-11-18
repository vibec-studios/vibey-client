package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.client.VibeyPlayers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
//? if >=1.21.9
import net.minecraft.network.chat.FontDescription;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds "[seal][badge] Name" components. The icons are glyphs of the own bitmap font {@code vibeyclient:icons}
 * (U+E000 seal, U+E001 staff, U+E002 supporter, U+E003 creator), so they go through the normal text pipeline on every
 * version: nametag background width, see-through pass, shadows and other mods that restyle the name keep working. We
 * only prepend a component; the original name component is kept as it is.
 */
public final class NameIcons {
    private NameIcons() {}

    public static final char SEAL = '', STAFF = '', SUPPORTER = '', CREATOR = '';

    private static final Style ICON_STYLE = font(Style.EMPTY).withColor(0xFFFFFF).withItalic(false).withBold(false);

    /** Per player: last input and output, so the per-frame call does not allocate when nothing changed. */
    private record Cached(Component in, long version, int flags, Component out) {}

    private static final Map<UUID, Cached> NAMETAG = new ConcurrentHashMap<>(), TAB = new ConcurrentHashMap<>();

    /** Nametag in the world (render thread). Returns {@code name} unchanged when there is nothing to add. */
    public static Component nametag(UUID player, Component name) {
        NametagsModule m = NametagsModule.instance;
        if (m == null || !m.isEnabled() || !m.nametag.get()) return name;
        return decorate(NAMETAG, player, name, m.badges.get());
    }

    /** Tab list (render thread). */
    public static Component tabList(UUID player, Component name) {
        NametagsModule m = NametagsModule.instance;
        if (m == null || !m.isEnabled() || !m.tabList.get()) return name;
        return decorate(TAB, player, name, m.badges.get());
    }

    private static Component decorate(Map<UUID, Cached> cache, UUID player, Component name, boolean badges) {
        if (player == null || name == null) return name;
        long v = VibeyPlayers.version();
        int flags = badges ? 1 : 0;
        Cached c = cache.get(player);
        if (c != null && c.version == v && c.flags == flags && (c.in == name || c.in.equals(name))) return c.out;
        String icons = icons(player, badges);
        Component out = icons.isEmpty() ? name : Component.empty().append(glyphs(icons))
                .append(Component.literal(" ")).append(name);
        if (cache.size() > 512) cache.clear();
        cache.put(player, new Cached(name, v, flags, out));
        return out;
    }

    /** The icon glyphs for a player ("" = none). */
    public static String icons(UUID player, boolean badges) {
        StringBuilder sb = new StringBuilder(3);
        if (VibeyPlayers.isVibey(player)) sb.append(SEAL);
        if (badges) {
            List<String> list = VibeyPlayers.badges(player);
            for (int i = 0; i < list.size() && i < 2; i++) {
                switch (list.get(i)) {
                    case "staff" -> sb.append(STAFF);
                    case "supporter" -> sb.append(SUPPORTER);
                    case "creator" -> sb.append(CREATOR);
                    default -> {}
                }
            }
        }
        return sb.toString();
    }

    /** A literal of icon glyphs in the icon font (for UIs that want to show them). */
    public static MutableComponent glyphs(String icons) {
        return Component.literal(icons).setStyle(ICON_STYLE);
    }

    public static void clearCaches() {
        NAMETAG.clear();
        TAB.clear();
    }

    private static Style font(Style style) {
        //? if >=1.21.9 {
        return style.withFont(new FontDescription.Resource(Compat.id("icons")));
        //?} else
        /*return style.withFont(Compat.id("icons"));*/
    }
}
