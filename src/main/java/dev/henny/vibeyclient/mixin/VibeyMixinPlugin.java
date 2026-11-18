package dev.henny.vibeyclient.mixin;

import dev.henny.vibeyclient.ModConflicts;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Shared by every {@code vibeyclient.<module>.mixins.json}: the module id is taken from the mixin package
 * ({@code dev.henny.vibeyclient.module.<id>.mixin}); if the standalone mod that replaces the module is installed,
 * none of the module's mixins apply. A part of a module may have its own config and conflicts:
 * {@code dev.henny.vibeyclient.module.media.lyrics.mixin} is checked against {@code ModConflicts.of("media")} and
 * {@code ModConflicts.of("media.lyrics")}.
 */
public final class VibeyMixinPlugin implements IMixinConfigPlugin {
    private static final String MODULE_PREFIX = "dev.henny.vibeyclient.module.";
    private boolean skip;
    private String module = "core";

    @Override
    public void onLoad(String mixinPackage) {
        if (mixinPackage.startsWith(MODULE_PREFIX)) {
            String rest = mixinPackage.substring(MODULE_PREFIX.length());
            int end = rest.endsWith(".mixin") ? rest.length() - ".mixin".length() : rest.indexOf('.');
            module = end < 0 ? rest : rest.substring(0, end);
        }
        // "media.lyrics" (a part of a module) is skipped when either the module or that part has a conflict.
        String top = module.contains(".") ? module.substring(0, module.indexOf('.')) : module;
        java.util.List<String> mods = new java.util.ArrayList<>(ModConflicts.of(top));
        if (!top.equals(module)) mods.addAll(ModConflicts.of(module));
        for (String mod : mods) {
            if (isModLoaded(mod)) {
                skip = true;
                System.out.println("[VibeyClient] mixins of '" + module + "' skipped: '" + mod + "' is installed");
            }
        }
    }

    private static boolean isModLoaded(String id) {
        //? if fabric {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id);
        //?} else {
        /*var list = net.neoforged.fml.loading.LoadingModList.get();
        return list != null && list.getModFileById(id) != null;
        *///?}
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !skip;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
