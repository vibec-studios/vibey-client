package dev.henny.vibeyclient.module.cosmetics;

import com.google.gson.JsonElement;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.avatar.AvatarConfig;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.AvatarStates;
import dev.henny.vibeyclient.avatar.AvatarTicker;
import dev.henny.vibeyclient.avatar.DevItems;
import dev.henny.vibeyclient.avatar.DevItemsImpl;
import dev.henny.vibeyclient.avatar.EngineCompat;
import dev.henny.vibeyclient.avatar.ItemLoader;
import dev.henny.vibeyclient.avatar.ItemSource;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.Wardrobe;
import dev.henny.vibeyclient.avatar.preview.AvatarPreview;
import dev.henny.vibeyclient.avatar.preview.PreviewService;
import dev.henny.vibeyclient.avatar.render.AvatarDraw;
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

    public final BoolSetting showOwn = bool("showOwn", true);
    public final BoolSetting showOthers = bool("showOthers", true);
    public final BoolSetting hideWithArmor = bool("hideWithArmor", true);
    public final BoolSetting showCapes = bool("show_cape", true);
    public final BoolSetting showWings = bool("show_wings", true);
    public final BoolSetting showAuras = bool("show_aura", true);
    public final BoolSetting showPets = bool("show_pet", true);
    public final BoolSetting showShields = bool("show_shield", true);
    public final BoolSetting showHats = bool("show_hat", true);
    public final BoolSetting showBacks = bool("show_back", true);
    public final BoolSetting notifyNew = bool("notifyNew", true);
    public final KeybindSetting wardrobeKey = keybind("wardrobe", Keys.UNBOUND).onPress(this::openWardrobe);

    private final LoadoutSync sync = new LoadoutSync(this);
    private Loadout loadout = new Loadout();
    private boolean localDirty;
    private boolean loaded;
    private final DevItemsImpl devItems = new DevItemsImpl(this);
    private final PreviewService preview = new PreviewService();
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
        ItemLoader.loadBuiltin();
        Services.register(DevItems.class, devItems);
        Services.register(AvatarPreview.class, preview);
        on(VibeyEvents.ServerHello.class, e -> dev.henny.vibeyclient.api.net.VibeyNet.sendLoadout(sharedSlots(), sync.ownToken()));
        on(VibeyEvents.RemoteLoadout.class, e -> sync.serverLoadout(e.player(), e.slots(), e.token()));
        on(VibeyEvents.WorldLeave.class, e -> sync.worldLeft());
        on(VibeyEvents.OnlineFeaturesChanged.class, e -> {
            if (e.enabled()) ItemLoader.refreshCatalog(true);
            else ItemLoader.dropCatalog();
        });
        on(VibeyEvents.BackendConnected.class, e -> ItemLoader.refreshCatalog(false));
        // another backend has another catalog
        GlobalSettings.backendUrl.onChange(v -> {
            ItemLoader.dropCatalog();
            ItemLoader.refreshCatalog(true);
        });
        for (BoolSetting s : new BoolSetting[]{showOwn, showOthers, hideWithArmor, showCapes, showWings, showAuras, showPets, showShields, showHats, showBacks}) {
            s.onChange(v -> applyConfig());
        }
        AvatarItems.addListener(this::applySelf);
        ItemLoader.onNewItems = this::newItems;
        dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.register();
    }

    @Override
    public void onEnable() {
        Services.register(Wardrobe.class, this);
        Services.register(dev.henny.vibeyclient.api.service.MenuPreview.class, titlePreview);
        applyConfig();
        ItemLoader.refreshCatalog(false);
    }

    @Override
    public void onDisable() {
        Services.unregister(Wardrobe.class, this);
        Services.unregister(dev.henny.vibeyclient.api.service.MenuPreview.class, titlePreview);
        AvatarConfig.cosmetics = false;
    }

    @Override
    public void tick() {
        ItemLoader.tick();
        AvatarConfig.self = EngineCompat.selfId();
        applySelf();
        sync.tick();
        AvatarTicker.tick();
        ItemLoader.refreshCatalog(false);
        dev.henny.vibeyclient.avatar.SelfTest.tick();
    }

    @Override
    public void onWorldJoin() {
        AvatarStates.clear();
        applySelf();
        ItemLoader.requestCatalog(3_000);
    }

    // ============================================================================================ OTA: new items

    private final java.util.Set<String> pendingNew = new java.util.LinkedHashSet<>();

    /** New catalog items or new grants (client thread): one toast per batch, a few seconds after the last change. */
    void newItems(java.util.Set<String> ids) {
        for (String id : ids) {
            AvatarItem i = AvatarItems.get(id);
            if (i != null && canUse(i)) pendingNew.add(id);
        }
        if (pendingNew.isEmpty() || !notifyNew.get()) {
            pendingNew.clear();
            return;
        }
        dev.henny.vibeyclient.api.Tasks.debounce("vibeyclient:cosmetics:new", 3_000, () -> dev.henny.vibeyclient.api.Tasks.onMain(() -> {
            int n = pendingNew.size();
            if (n == 0) return;
            AvatarItem first = AvatarItems.get(pendingNew.iterator().next());
            pendingNew.clear();
            if (!isEnabled()) return;
            String body = n == 1 && first != null ? tr("new.one", first.name()) : tr("new.many", n);
            dev.henny.vibeyclient.api.service.Notify.show(new dev.henny.vibeyclient.api.service.Notify.Toast(tr("new.title"), body,
                    dev.henny.vibeyclient.api.service.Notify.Kind.SUCCESS, null, this::openWardrobe));
        }));
    }

    private void applyConfig() {
        AvatarConfig.cosmetics = isEnabled();
        AvatarConfig.showOwn = showOwn.get();
        AvatarConfig.showOthers = showOthers.get();
        AvatarDraw.hideWithArmor = hideWithArmor.get();
        BoolSetting[] per = {showCapes, showWings, showAuras, showPets, showShields, showHats, showBacks};
        for (Slot s : Slot.all()) AvatarConfig.slotShown[s.ordinal()] = per[s.ordinal()].get();
    }

    // ============================================================================================ loadout

    UUID selfId() {
        return EngineCompat.selfId();
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
            AvatarItem item = AvatarItems.get(local);
            if (l.get(s) == null && item != null && item.source() != ItemSource.CATALOG) l.slots[s.ordinal()] = local;
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
            AvatarItem item = AvatarItems.get(loadout.get(s));
            if (item != null && !item.isDev() && sync.canUse(item)) m.put(s.id(), item.id());
        }
        return m;
    }

    /** Our own avatar state follows the local loadout (usable items only). */
    private void applySelf() {
        UUID self = EngineCompat.selfId();
        if (self == null) return;
        AvatarState st = AvatarStates.get(self);
        for (Slot s : Slot.all()) {
            String id = loadout.get(s);
            AvatarItem item = AvatarItems.get(id);
            st.slots[s.ordinal()] = item != null && sync.canUse(item) ? id : null;
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
    public void equip(AvatarItem item) {
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
    public boolean canUse(AvatarItem item) {
        return sync.canUse(item);
    }

    @Override
    public String lockedReason(AvatarItem item) {
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
        dev.henny.vibeyclient.avatar.EmoteBridge.play.accept(emoteId);
    }

    @Override
    public void stopEmote() {
        dev.henny.vibeyclient.avatar.EmoteBridge.stop.run();
    }

    @Override
    public String playingEmote() {
        AvatarState st = AvatarStates.peek(EngineCompat.selfId());
        return st == null || st.emote == null || st.emote.stopping() ? null : st.emote.id;
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
