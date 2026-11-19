package dev.henny.vibeyclient.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * The vanilla toasts currently on screen, so the Vibey toast stack can sit below them
 * ({@code ClientCompat.vanillaToastBottom}). Elements are {@link ToastInstanceAccessor}s.
 */
//? if >=1.21.2 {
@Mixin(net.minecraft.client.gui.components.toasts.ToastManager.class)
//?} else
/*@Mixin(net.minecraft.client.gui.components.toasts.ToastComponent.class)*/
public interface ToastManagerAccessor {
    //? if >=1.21.2 {
    @Accessor("visibleToasts")
    //?} else
    /*@Accessor("visible")*/
    List<?> vibeyclient$visible();
}
