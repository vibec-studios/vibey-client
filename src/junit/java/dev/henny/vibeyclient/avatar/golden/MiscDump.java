package dev.henny.vibeyclient.avatar.golden;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.ClientVersion;
import dev.henny.vibeyclient.avatar.DevItems;
import dev.henny.vibeyclient.avatar.ItemSource;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.BundleParser;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

final class MiscDump {
    static final String PINNED_CONTENT_VERSION = "0.3.1";

    private MiscDump() {}

    static String versions() {
        Out o = new Out().line("contentVersion", PINNED_CONTENT_VERSION);
        for (String[] p : Tables.VERSIONS) o.line("compare", "'" + p[0] + "'", "'" + p[1] + "'", Integer.signum(ClientVersion.compare(p[0], p[1])));
        for (String[] p : Tables.VERSIONS) if (p[1] != null) o.line("satisfies", "'" + p[1] + "'", ClientVersion.compare(PINNED_CONTENT_VERSION, p[1]) >= 0);
        byte[][] thumbs = Tables.thumbnails();
        for (int i = 0; i < thumbs.length; i++) o.line("thumbnail", i, DevItems.checkThumbnail(thumbs[i]));
        return o.toString();
    }

    static String registry() throws Exception {
        Out o = new Out();
        JsonObject index = JsonParser.parseString(Files.readString(GoldenFiles.root().resolve("content/dist/index.json"))).getAsJsonObject();
        Map<String, AvatarItem> items = new HashMap<>();
        for (JsonElement el : index.getAsJsonArray("items")) {
            JsonObject e = el.getAsJsonObject();
            Bundle b = BundleParser.parse(GoldenFiles.read("dist/" + e.get("file").getAsString()));
            String thumb = e.has("thumbnail") ? "/assets/vibeyclient/builtin/" + e.get("thumbnail").getAsString() : null;
            int sort = e.has("sort") ? e.get("sort").getAsInt() : 1000;
            items.put(b.id, AvatarItem.of(b, ItemSource.BUILTIN, sort, thumb, "default", 1));
        }
        AvatarItems.putAll(ItemSource.BUILTIN, items, true);
        for (AvatarItem i : AvatarItems.all()) o.line("item", i.id(), i.kind(), i.slot(), i.sort(), i.thumbnail(), i.availability(), i.nameText().en(), i.rarity());
        for (Slot s : Slot.all()) o.line("slot", s, s.id(), AvatarItems.bySlot(s).size());
        o.line("emotes", AvatarItems.emotes().size());
        Bundle dev = AvatarItems.get("vibey:aurora_cape").bundle();
        AvatarItems.put(AvatarItem.of(dev, ItemSource.DEV, -1, "dev:x", null, 2));
        o.line("winner", AvatarItems.get("vibey:aurora_cape").source(), AvatarItems.all().get(0).id(), AvatarItems.all().get(0).sort());
        AvatarItems.remove("vibey:aurora_cape", ItemSource.DEV);
        o.line("winner", AvatarItems.get("vibey:aurora_cape").source());
        AvatarItems.putAll(ItemSource.BUILTIN, Map.of(), true);
        o.line("empty", AvatarItems.all().size());
        return o.toString();
    }
}
