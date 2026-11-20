package dev.henny.vibeyclient.module.cosmetics;

import com.google.gson.JsonElement;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.dev.DevItems;
import dev.henny.cosmetico.mc.compat.McSelf;
import dev.henny.vibeyclient.module.cosmetics.catalog.Catalog;
import dev.henny.vibeyclient.module.cosmetics.selftest.SelfTest;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.cosmetico.model.Slot;
import dev.henny.vibeyclient.module.cosmetics.preview.AvatarPreview;
import dev.henny.vibeyclient.module.cosmetics.preview.CosmeticoPreviews;
import dev.henny.vibeyclient.module.cosmetics.catalog.BuiltinItems;
import dev.henny.vibeyclient.module.cosmetics.host.VibeyCosmeticoHost;
import dev.henny.vibeyclient.client.Keys;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Capes, wings, auras, pets, shields, hats and back items (.vibey bundles). Owns the local loadout
 * ({@code config/vibeyclient/cosmetics.json}), the item registry loading and the loadout sync; registers the
 * {@link Wardrobe}, {@link DevItems} and {@link AvatarPreview} services.
 */
public final class CosmeticsModule extends VibeyModule implements Wardrobe {

    public final ActionSetting openWardrobeAction = action("openWardrobe", this::openWardrobe);
    public final BoolSetting showOwn = bool("showOwn", true).section("show");
    public final BoolSetting showOthers = bool("showOthers", true).section("show");
    public final BoolSetting hideWithArmor = bool("hideWithArmor", true).section("show");
    public final BoolSetting notifyNew = bool("notifyNew", true).section("show");
    public final BoolSetting showCapes = bool("show_cape", true).section("types").serverRule(SlotRules.key(Slot.CAPE));
    public final BoolSetting showWings = bool("show_wings", true).section("types").serverRule(SlotRules.key(Slot.WINGS));
    public final BoolSetting showAuras = bool("show_aura", true).section("types").serverRule(SlotRules.key(Slot.AURA));
    public final BoolSetting showPets = bool("show_pet", true).section("types").serverRule(SlotRules.key(Slot.PET));
    public final BoolSetting showShields = bool("show_shield", true).section("types").serverRule(SlotRules.key(Slot.SHIELD));
    public final BoolSetting showHats = bool("show_hat", true).section("types").serverRule(SlotRules.key(Slot.HAT));
    public final BoolSetting showBacks = bool("show_back", true).section("types").serverRule(SlotRules.key(Slot.BACK));
    public final KeybindSetting wardrobeKey = keybind("wardrobe", Keys.UNBOUND).onPress(this::openWardrobe);

    private final LoadoutSync sync = new LoadoutSync(this);
    private Loadout loadout = new Loadout();
    private boolean localDirty;
    private boolean loaded;
    private final CosmeticoPreviews preview = new CosmeticoPreviews();
    private final dev.henny.vibeyclient.module.cosmetics.ui.TitlePreview titlePreview = new dev.henny.vibeyclient.module.cosmetics.ui.TitlePreview();

    public CosmeticsModule() {
        super("cosmetics", ModuleCategory.COSMETICS);
    }

    @Override
    public int accentColor() {
        return 0xFF8B6CFF;
    }

    // ============================================================================================ lifecycle

    @Override
    public void onLoad() {
        JsonElement saved = settings().data().get("loadout");
        loadout = Loadout.fromJson(saved);
        localDirty = settings().data().has("dirty") && settings().data().get("dirty").getAsBoolean();
        loaded = true;
        Cosmetico.init(new VibeyCosmeticoHost());
        BuiltinItems.load();
        Services.register(DevItems.class, Cosmetico.devItems());
        Services.register(AvatarPreview.class, preview);
        on(VibeyEvents.ServerHello.class, e -> dev.henny.vibeyclient.api.net.VibeyNet.sendLoadout(sharedSlots(), sync.ownToken()));
        on(VibeyEvents.RemoteLoadout.class, e -> sync.serverLoadout(e.player(), e.slots(), e.token()));
        on(VibeyEvents.WorldLeave.class, e -> sync.worldLeft());
        on(VibeyEvents.OnlineFeaturesChanged.class, e -> {
            if (e.enabled()) Catalog.refreshCatalog(true);
            else Catalog.dropCatalog();
        });
        on(VibeyEvents.BackendConnected.class, e -> Catalog.refreshCatalog(false));
        // another backend has another catalog
        GlobalSettings.backendUrl.onChange(v -> {
            Catalog.dropCatalog();
            Catalog.refreshCatalog(true);
        });
        for (BoolSetting s : new BoolSetting[]{showOwn, showOthers, hideWithArmor, showCapes, showWings, showAuras, showPets, showShields, showHats, showBacks}) {
            s.onChange(v -> applyConfig());
        }
        Cosmetico.items().listen(this::applySelf);
        Catalog.onNewItems(this::newItems);
        // server rules (PLAY-LINKS.md §5): cosmetics.pets etc. hide that slot for everyone while on the server
        for (Slot s : Slot.all()) dev.henny.vibeyclient.api.ServerRules.supports(SlotRules.key(s));
        dev.henny.vibeyclient.api.ServerRules.listen(CosmeticsModule::applyServerRules);
        applyServerRules();
        dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.register();
    }

    @Override
    public void onEnable() {
        Services.register(Wardrobe.class, this);
        Services.register(dev.henny.vibeyclient.api.service.MenuPreview.class, titlePreview);
        applyConfig();
        Catalog.refreshCatalog(false);
    }

    @Override
    public void onDisable() {
        Services.unregister(Wardrobe.class, this);
        Services.unregister(dev.henny.vibeyclient.api.service.MenuPreview.class, titlePreview);
        Cosmetico.config().cosmetics = false;
    }

    @Override
    public void tick() {
        Cosmetico.config().self = McSelf.id();
        applySelf();
        sync.tick();
        Catalog.refreshCatalog(false);
        SelfTest.tick();
    }

    @Override
    public void onWorldJoin() {
        Cosmetico.engine().avatars.clear();
        applySelf();
        // without a gateway nobody tells us about catalog changes: check on world join (with it: `catalog.updated`)
        if (!Catalog.catalogPushed()) Catalog.requestCatalog(3_000);
    }

    // ============================================================================================ OTA: new items

    private final java.util.Set<String> pendingNew = new java.util.LinkedHashSet<>();

    /** New catalog items or new grants (client thread): one toast per batch, a few seconds after the last change. */
    void newItems(java.util.Set<String> ids) {
        for (String id : ids) {
            Item i = Cosmetico.items().get(id);
            if (i != null && canUse(i)) pendingNew.add(id);
        }
        if (pendingNew.isEmpty() || !notifyNew.get()) {
            pendingNew.clear();
            return;
        }
        dev.henny.vibeyclient.api.Tasks.debounce("vibeyclient:cosmetics:new", 3_000, () -> dev.henny.vibeyclient.api.Tasks.onMain(() -> {
            int n = pendingNew.size();
            if (n == 0) return;
            Item first = Cosmetico.items().get(pendingNew.iterator().next());
            pendingNew.clear();
            if (!isEnabled()) return;
            String body = n == 1 && first != null ? tr("new.one", first.name()) : tr("new.many", n);
            dev.henny.vibeyclient.api.service.Notify.show(new dev.henny.vibeyclient.api.service.Notify.Toast(tr("new.title"), body,
                    dev.henny.vibeyclient.api.service.Notify.Kind.SUCCESS, null, this::openWardrobe));
        }));
    }

    private static void applyServerRules() {
        for (Slot s : Slot.all()) Cosmetico.config().slotServerOff[s.ordinal()] = dev.henny.vibeyclient.api.ServerRules.isDisabled(SlotRules.key(s));
    }

    @Override
    public java.util.List<String> serverRuleKeys() {
        return SERVER_RULE_KEYS;
    }

    private static final java.util.List<String> SERVER_RULE_KEYS = SlotRules.KEYS;

    private void applyConfig() {
        Cosmetico.config().cosmetics = isEnabled();
        Cosmetico.config().showOwn = showOwn.get();
        Cosmetico.config().showOthers = showOthers.get();
        Cosmetico.config().hideWithArmor = hideWithArmor.get();
        BoolSetting[] per = {showCapes, showWings, showAuras, showPets, showShields, showHats, showBacks};
        for (Slot s : Slot.all()) Cosmetico.config().slotShown[s.ordinal()] = per[s.ordinal()].get();
    }

    // ============================================================================================ loadout

    UUID selfId() {
        return McSelf.id();
    }

    Loadout loadout() {
        return loadout;
    }

    boolean localDirty() {
        return localDirty;
    }

    void markSynced() {
        localDirty = false;
        save();
    }

    /** The backend has a newer loadout (another device): take it, keeping local-only items in empty slots. */
    void adoptRemoteLoadout(Loadout remote) {
        Loadout l = remote.copy();
        for (Slot s : Slot.all()) {
            String local = loadout.get(s);
            Item item = Cosmetico.items().get(local);
            if (l.get(s) == null && item != null && item.source() != ItemSource.REMOTE) l.slots[s.ordinal()] = local;
        }
        for (int i = 0; i < WHEEL_SLOTS; i++) if (l.wheel[i] == null) l.wheel[i] = loadout.wheel[i];
        loadout = l;
        save();
        applySelf();
    }

    private void changed() {
        localDirty = true;
        save();
        applySelf();
        sync.loadoutChanged();
    }

    private void save() {
        if (!loaded) return;
        settings().data().add("loadout", loadout.toJson());
        settings().data().addProperty("dirty", localDirty);
        settings().markDirty();
    }

    /** Slots others may see (no dev items, only usable ones). */
    Map<String, String> sharedSlots() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Slot s : Slot.all()) {
            Item item = Cosmetico.items().get(loadout.get(s));
            if (item != null && !item.isDev() && sync.canUse(item)) m.put(s.id(), item.id());
        }
        return m;
    }

    /** Our own avatar state follows the local loadout (usable items only). */
    private void applySelf() {
        UUID self = McSelf.id();
        if (self == null) return;
        Avatar st = Cosmetico.engine().avatars.get(self);
        for (Slot s : Slot.all()) {
            String id = loadout.get(s);
            Item item = Cosmetico.items().get(id);
            st.loadout.base[s.ordinal()] = item != null && sync.canUse(item) ? id : null;
        }
    }

    public LoadoutSync sync() {
        return sync;
    }

    // ============================================================================================ Wardrobe

    @Override
    public String equipped(Slot slot) {
        return loadout.get(slot);
    }

    @Override
    public void equip(Item item) {
        if (item == null || item.isEmote() || item.slot() == null || !canUse(item)) return;
        if (item.id().equals(loadout.get(item.slot()))) return;
        loadout.slots[item.slot().ordinal()] = item.id();
        changed();
    }

    @Override
    public void unequip(Slot slot) {
        if (loadout.get(slot) == null) return;
        loadout.slots[slot.ordinal()] = null;
        changed();
    }

    @Override
    public boolean canUse(Item item) {
        return sync.canUse(item);
    }

    @Override
    public String lockedReason(Item item) {
        if (canUse(item)) return null;
        if (item.needsUpdate()) return tr("locked.update");
        return GlobalSettings.online() && sync.backendReady() ? tr("locked.notUnlocked") : tr("locked.signIn");
    }

    @Override
    public String wheel(int index) {
        return index < 0 || index >= WHEEL_SLOTS ? null : loadout.wheel[index];
    }

    @Override
    public void setWheel(int index, String emoteId) {
        if (index < 0 || index >= WHEEL_SLOTS) return;
        if (java.util.Objects.equals(loadout.wheel[index], emoteId)) return;
        loadout.wheel[index] = emoteId;
        changed();
    }

    @Override
    public void playEmote(String emoteId) {
        EmoteHook.play.accept(emoteId);
    }

    @Override
    public void stopEmote() {
        EmoteHook.stop.run();
    }

    @Override
    public String playingEmote() {
        return Cosmetico.emotes().playing(McSelf.id());
    }

    /** Called by the emotes module for our plays (net + backend). */
    public void broadcastEmote(String emoteId, boolean play) {
        sync.sendEmote(emoteId, play);
    }

    public void openWardrobe() {
        dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.openWardrobe(dev.henny.vibeyclient.client.ClientCompat.screen());
    }

    public static CosmeticsModule get() {
        return ModuleManager.get().get(CosmeticsModule.class);
    }
}
