package dev.henny.companio.mc.client;

import dev.henny.companio.config.CompanioConfig;
import dev.henny.companio.config.Saved;
import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.pack.PackRegistry;
import dev.henny.companio.sense.Senses;
import dev.henny.companio.world.Command;
import dev.henny.companio.world.Companion;
import dev.henny.companio.world.Output;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

final class OwnCompanion {
    private final PackRegistry registry;
    private final Senses senses;
    private final Output output;
    private Companion companion;
    private CompanionDef def;

    OwnCompanion(PackRegistry registry, Senses senses, Output output) {
        this.registry = registry;
        this.senses = senses;
        this.output = output;
    }

    Companion get() {
        return companion;
    }

    boolean sync(CompanioConfig c, Player p, Supplier<String> language) {
        CompanionDef want = c.enabled && c.shown() ? registry.companion(c.selected, c.active()) : null;
        if (want == def && companion != null) {
            apply(c);
            return false;
        }
        save(c);
        def = want;
        companion = null;
        if (want == null || p == null) return true;
        Companion n = new Companion(want, senses, System.nanoTime(), language);
        n.output(output);
        Saved saved = c.saved.get(want.key());
        if (saved != null) {
            n.brain().needs().restore(saved.needs);
            n.brain().memory().restoreVars(saved.vars);
        }
        double side = Math.toRadians(p.getYRot() + 90);
        n.spawn(p.getX() + Math.cos(side) * 1.2, p.getY(), p.getZ() + Math.sin(side) * 1.2, p.getYRot());
        n.name(c.name);
        n.command(Command.of(c.command), false);
        companion = n;
        apply(c);
        return true;
    }

    private void apply(CompanioConfig c) {
        String variant = c.variant.isEmpty() ? companion.def.defaultVariant() : c.variant;
        if (!variant.equals(companion.variant())) companion.variant(variant);
        float size = c.size <= 0 ? companion.def.size().def() : c.size;
        if (size != companion.size()) companion.size(size);
        if (!c.name.equals(companion.name())) companion.name(c.name);
    }

    void save(CompanioConfig c) {
        if (companion == null) return;
        Saved s = c.saved.computeIfAbsent(companion.def.key(), k -> new Saved());
        s.needs = new java.util.HashMap<>(companion.brain().needs().snapshot());
        s.vars = new java.util.HashMap<>(companion.brain().memory().vars());
    }

    void clear(CompanioConfig c) {
        save(c);
        companion = null;
        def = null;
    }

    static String language() {
        return Minecraft.getInstance().options.languageCode;
    }
}
