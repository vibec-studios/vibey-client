package dev.henny.vibeyclient.module.screens.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.henny.vibeyclient.module.screens.apply.StateHolder;
import dev.henny.vibeyclient.module.screens.apply.WidgetPaint;
import dev.henny.vibeyclient.module.screens.apply.WidgetState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractWidget.class)
abstract class WidgetStateMixin implements StateHolder {
    @Shadow protected int width;
    @Shadow protected int height;
    @Unique private WidgetState vibey$state;

    @Override
    public WidgetState vibey$screens() {
        return vibey$state;
    }

    @Override
    public void vibey$screens(WidgetState state) {
        vibey$state = state;
    }

    @Override
    public void vibey$size(int w, int h) {
        width = w;
        height = h;
    }

    @WrapMethod(method = "render", require = 0)
    private void vibey$render(GuiGraphics g, int mx, int my, float dt, Operation<Void> original) {
        WidgetState s = vibey$state;
        if (s == null || s.plain()) original.call(g, mx, my, dt);
        else WidgetPaint.render((AbstractWidget) (Object) this, s, g, mx, my, dt, original);
    }
}
