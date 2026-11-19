package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.Engine;
import dev.henny.cosmetico.dev.ThumbnailCheck;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.cosmetico.version.SemVer;

import java.util.Locale;
import java.util.Map;

public final class MiscDump {
    private MiscDump() {}

    public static final String BUILTIN = "/assets/vibeyclient/builtin/";

    public static String versions() {
        Out o = new Out().line("contentVersion", GoldenVersion.value());
        for (String[] p : Tables.VERSIONS) o.line("compare", "'" + p[0] + "'", "'" + p[1] + "'", Integer.signum(SemVer.compare(p[0], p[1])));
        for (String[] p : Tables.VERSIONS) if (p[1] != null) o.line("satisfies", "'" + p[1] + "'", GoldenVersion.gate().satisfies(p[1]));
        byte[][] thumbs = Tables.thumbnails();
        for (int i = 0; i < thumbs.length; i++) o.line("thumbnail", i, ThumbnailCheck.check(thumbs[i]));
        return o.toString();
    }

    public static String registry() {
        Out o = new Out();
        Engine e = TestHost.engine();
        Map<String, Item> items = e.indexLoader().read(ContentReader.dist(BUILTIN), BUILTIN, ItemSource.BUILTIN);
        e.items.putAll(ItemSource.BUILTIN, items, true);
        for (Item i : e.items.all()) o.line("item", i.id(), i.kind(), i.slot(), i.sort(), i.thumbnail(), i.availability(), i.nameText().en(), i.rarity());
        for (Slot s : Slot.all()) o.line("slot", s, s.id(), e.items.bySlot(s).size());
        o.line("emotes", e.items.emotes().size());
        Item cape = e.items.get("vibey:aurora_cape");
        e.items.put(Item.of(cape.bundle(), ItemSource.DEV, -1, "dev:x", null, 2));
        o.line("winner", source(e.items.get("vibey:aurora_cape")), e.items.all().get(0).id(), e.items.all().get(0).sort());
        e.items.remove("vibey:aurora_cape", ItemSource.DEV);
        o.line("winner", source(e.items.get("vibey:aurora_cape")));
        e.items.putAll(ItemSource.BUILTIN, Map.of(), true);
        o.line("empty", e.items.all().size());
        return o.toString();
    }

    private static String source(Item i) {
        return i.source().id().toUpperCase(Locale.ROOT);
    }
}
