package dev.henny.cosmetico.state;

import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.model.Slot;

import java.util.UUID;

public final class Avatar {
    public final UUID uuid;
    public final boolean preview;
    public final LoadoutLayers loadout = new LoadoutLayers();
    public final Motion motion = new Motion();
    public final PetMotion pet = new PetMotion();
    public final FrameSnapshot frame = new FrameSnapshot();
    public final EventClocks events = new EventClocks();
    public final OffsetWeights offsets = new OffsetWeights();
    public final Locators locators = new Locators();
    public EmotePlay emote;
    public long lastSeenTick;

    public Avatar(UUID uuid, boolean preview) {
        this.uuid = uuid;
        this.preview = preview;
    }

    public String shown(Slot slot) {
        return loadout.shown(slot);
    }
}
