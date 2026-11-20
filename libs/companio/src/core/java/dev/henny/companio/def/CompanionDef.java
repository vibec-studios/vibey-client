package dev.henny.companio.def;

import dev.henny.companio.text.Lang;
import dev.henny.companio.text.TextRef;
import dev.henny.cosmetico.model.Bundle;

import java.util.List;
import java.util.Map;

public record CompanionDef(String packId, String id, TextRef name, TextRef description, Lang lang,
                           Map<String, Bundle> models, List<VariantDef> variants, float scale, SizeDef size, BoxDef box,
                           Map<LocoState, String> loco, LookDef look, int lookBone, MovementDef movement,
                           Map<String, PropDef> props, Map<String, SoundDef> sounds, Map<String, byte[]> soundFiles,
                           Map<String, ParticleDef> particles, Map<String, NeedDef> needs, Map<String, Float> vars,
                           List<TrickDef> tricks, List<BehaviorDef> behaviors) {
    public String key() {
        return packId + ":" + id;
    }

    public String defaultVariant() {
        return variants.isEmpty() ? "default" : variants.get(0).id();
    }

    public Bundle model(String variant) {
        Bundle b = variant == null ? null : models.get(variant);
        return b != null ? b : models.get(defaultVariant());
    }

    public BehaviorDef behavior(String id) {
        for (BehaviorDef b : behaviors) if (b.id().equals(id)) return b;
        return null;
    }

    public TrickDef trick(String id) {
        for (TrickDef t : tricks) if (t.id().equals(id)) return t;
        return null;
    }
}
