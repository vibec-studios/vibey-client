package dev.henny.vibeyclient.mixin.client;

import net.minecraft.client.gui.components.toasts.Toast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** One visible vanilla toast (package-private class in vanilla): its toast, slot and slide state. */
//? if >=1.21.2 {
@Mixin(targets = "net.minecraft.client.gui.components.toasts.ToastManager$ToastInstance")
//?} else
/*@Mixin(targets = "net.minecraft.client.gui.components.toasts.ToastComponent$ToastInstance")*/
public interface ToastInstanceAccessor {
    @Accessor("toast")
    Toast vibeyclient$toast();

    //? if >=1.21.2 {
    @Accessor("firstSlotIndex")
    //?} else
    /*@Accessor("index")*/
    int vibeyclient$slot();

    @Accessor("visibility")
    Toast.Visibility vibeyclient$visibility();

    //? if >=1.21.2 {
    @Accessor("visiblePortion")
    float vibeyclient$visiblePortion();
    //?}
}
