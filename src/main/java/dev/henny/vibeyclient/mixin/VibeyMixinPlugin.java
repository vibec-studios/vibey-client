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
 * none of the module's mixins apply.
 */
public final class VibeyMixinPlugin implements IMixinConfigPlugin {
    private static final String MODULE_PREFIX = "dev.henny.vibeyclient.module.";
    private boolean skip;
    private String module = "core";

    @Override
    public void onLoad(String mixinPackage) {
        if (mixinPackage.startsWith(MODULE_PREFIX)) {
            String rest = mixinPackage.substring(MODULE_PREFIX.length());
            int dot = rest.indexOf('.');
            module = dot < 0 ? rest : rest.substring(0, dot);
        }
        for (String mod : ModConflicts.of(module)) {
            if (isModLoaded(mod)) {
                skip = true;
                System.out.println("[VibeyClient] mixins of module '" + module + "' skipped: '" + mod + "' is installed");
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
