package dev.henny.vibeyclient.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonInfo;
//?}

/**
 * Dev click test ({@code dev/ClickTest}): sets the cursor position and feeds a button press/release into the game's
 * own mouse handler, exactly like the window callback does (GUI scaling, global key handling, screen dispatch).
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    void vibeyclient$setXpos(double x);

    @Accessor("ypos")
    void vibeyclient$setYpos(double y);

    //? if >=1.21.9 {
    @Invoker("onButton")
    void vibeyclient$onButton(long handle, MouseButtonInfo button, int action);
    //?} else {
    /*@Invoker("onPress")
    void vibeyclient$onPress(long handle, int button, int action, int modifiers);
    *///?}
}
