package dev.henny.companio.parse;

import dev.henny.companio.def.ParticleDef;
import dev.henny.companio.def.PropDef;
import dev.henny.companio.def.SoundDef;
import dev.henny.companio.expr.Names;
import dev.henny.companio.pack.Problems;
import dev.henny.companio.text.Lang;
import dev.henny.cosmetico.model.Bundle;

import java.util.Map;
import java.util.Set;

record ReadContext(Problems problems, Lang lang, Names names, Bundle model, Map<String, PropDef> props,
                   Map<String, SoundDef> sounds, Map<String, ParticleDef> particles, Set<String> needs, Set<String> vars,
                   Set<String> behaviors) {
    boolean hasAnimation(String name) {
        return model == null || model.animation(name) != null;
    }

    String animations() {
        return model == null ? "" : String.join(", ", model.animations.keySet());
    }

    boolean hasLocator(String name) {
        return model == null || (model.model != null && model.model.locators.containsKey(name));
    }
}
