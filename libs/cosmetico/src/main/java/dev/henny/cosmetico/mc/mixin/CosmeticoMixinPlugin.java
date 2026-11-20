package dev.henny.cosmetico.mc.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class CosmeticoMixinPlugin implements IMixinConfigPlugin {
    static final List<String> CONFLICTS = List.of();
    private boolean skip;

    @Override
    public void onLoad(String mixinPackage) {
        if (Boolean.getBoolean("cosmetico.disableMixins")) skip = true;
        for (String mod : CONFLICTS) {
            if (!isModLoaded(mod)) continue;
            skip = true;
            System.out.println("[cosmetico] render hooks skipped: '" + mod + "' is installed");
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
        return !skip || mixinClassName.endsWith("Invoker") || mixinClassName.endsWith("Accessor");
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
