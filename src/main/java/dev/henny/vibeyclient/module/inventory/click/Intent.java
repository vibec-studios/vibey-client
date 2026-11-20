package dev.henny.vibeyclient.module.inventory.click;

import dev.henny.vibeyclient.module.inventory.core.Click;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

public interface Intent {
    int containerId();

    List<Click> next(AbstractContainerMenu menu);

    default boolean valid(AbstractContainerMenu menu) {
        return menu.containerId == containerId() && menu.getCarried().isEmpty();
    }
}
