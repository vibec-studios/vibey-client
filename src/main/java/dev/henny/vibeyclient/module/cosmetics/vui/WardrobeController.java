package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.registry.Item;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import dev.henny.vibeyclient.ui.vui.WatchedState;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

import java.util.ArrayList;
import java.util.List;

public final class WardrobeController {
    public final WatchedState<String> query = new WatchedState<>("");
    public final WatchedState<String> slot;
    public final ListState<SlotTab> slots = new ListState<>();
    public final ListState<ItemRow> items = new ListState<>();
    public final State<CurrentItem> current = new State<>(null);
    public final State<Boolean> playing = new State<>(false);
    public final String stillImage = "";
    final Stage stage = new Stage();
    private final Runnable close;
    private final Runnable wheel;
    private String selected;
    private String signature = "";

    WardrobeController(String startSlot, Runnable close, Runnable wheel) {
        this.close = close;
        this.wheel = wheel;
        this.slot = new WatchedState<>(startSlot);
        slot.onChange(s -> {
            selected = null;
            stage.reset();
            stage.face(Slots.of(s).slot());
            sync();
        });
        query.onChange(q -> sync());
        stage.face(Slots.of(startSlot).slot());
        sync();
    }

    static Wardrobe wardrobe() {
        return Services.get(Wardrobe.class).orElse(null);
    }

    public void sync() {
        Wardrobe wd = wardrobe();
        Slots.Def def = Slots.of(slot.peek());
        List<Item> list = ItemList.items(def, query.peek());
        List<ItemRow> rows = new ArrayList<>();
        StringBuilder sig = new StringBuilder(Lang.code()).append(Cosmetico.items().version()).append(selected);
        for (Item item : list) {
            ItemRow r = ItemList.row(item, wd, selected);
            rows.add(r);
            sig.append(r.thumb()).append(r.worn()).append(r.locked());
        }
        if (!sig.toString().equals(signature)) {
            signature = sig.toString();
            items.replaceAll(rows);
            slots.replaceAll(tabs(wd));
        }
        current.set(currentOf(wd, def));
        playing.set(stage.emote() != null);
    }

    private static List<SlotTab> tabs(Wardrobe wd) {
        List<SlotTab> out = new ArrayList<>();
        for (Slots.Def d : Slots.ALL) {
            boolean worn = wd != null && d.slot() != null && wd.equipped(d.slot()) != null;
            out.add(new SlotTab(d.id(), d.icon(), Lang.tr("ui.slot." + d.id()), ItemList.count(d), worn));
        }
        return out;
    }

    private CurrentItem currentOf(Wardrobe wd, Slots.Def def) {
        Item item = selected == null ? null : Cosmetico.items().get(selected);
        if (item == null && wd != null && def.slot() != null && wd.equipped(def.slot()) != null) item = Cosmetico.items().get(wd.equipped(def.slot()));
        if (item == null) return null;
        boolean locked = wd != null && !wd.canUse(item);
        String creator = item.creator() == null || item.creator().startsWith("\u0000") ? "" : item.creator();
        return new CurrentItem(item.id(), item.name(), ItemList.rarity(item), ItemList.color(item), creator,
                ItemList.worn(item, wd), locked, locked && wd != null ? wd.lockedReason(item) : "");
    }

    public void pick(ItemRow it) {
        selected = it.id();
        Item item = it.item();
        if (item.isEmote()) stage.play(item.id());
        else if (it.worn()) stage.show(null, null);
        else stage.show(item.slot(), item.id());
        sync();
    }

    public void wear(Object target) {
        Item item = target instanceof ItemRow r ? r.item() : target instanceof CurrentItem c ? Cosmetico.items().get(c.id()) : null;
        Wardrobe wd = wardrobe();
        if (item == null || wd == null || item.isEmote() || !wd.canUse(item)) return;
        if (item.id().equals(wd.equipped(item.slot()))) wd.unequip(item.slot());
        else wd.equip(item);
        selected = item.id();
        stage.show(null, null);
        sync();
    }

    public void playEmote() {
        CurrentItem c = current.peek();
        if (stage.emote() != null) stage.stop();
        else if (c != null) stage.play(c.id());
        sync();
    }

    public void openWheel() {
        wheel.run();
    }

    public void resetView() {
        stage.reset();
        stage.face(Slots.of(slot.peek()).slot());
        sync();
    }

    public void close() {
        close.run();
    }
}
