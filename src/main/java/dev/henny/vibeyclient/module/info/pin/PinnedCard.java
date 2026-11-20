package dev.henny.vibeyclient.module.info.pin;

import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.card.BlockCards;
import dev.henny.vibeyclient.module.info.card.CardData;
import dev.henny.vibeyclient.module.info.card.EntityCards;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

public final class PinnedCard {
    public static final String DOCUMENT = "vibeyclient:info/card";

    private final CardController controller = new CardController();
    private final WorldObject panel;
    private final BlockPos pos;
    private final Entity entity;
    private final BlockState pinnedState;

    PinnedCard(CardData data, BlockState state, Direction face, Minecraft mc) {
        this.pos = data.pos();
        this.entity = data.entity();
        this.pinnedState = state;
        controller.apply(data);
        panel = R3Mc.panel(DOCUMENT, controller);
        panel.transform().size(PinPlacement.WIDTH, PinPlacement.HEIGHT);
        panel.pixelsPerBlock(PinPlacement.PIXELS_PER_BLOCK);
        if (entity != null) PinPlacement.onEntity(panel, entity);
        else PinPlacement.atBlock(panel, pos, face, mc.player.getEyePosition());
        R3Mc.add(panel);
    }

    boolean refresh(InfoModule m, Minecraft mc) {
        if (R3Mc.scene().get(panel.id()) == null) return false;
        if (entity != null) {
            if (entity.isRemoved()) return false;
            controller.apply(EntityCards.read(entity, mc.player, m));
            return true;
        }
        BlockState now = mc.level.getBlockState(pos);
        if (now.getBlock() != pinnedState.getBlock()) return false;
        controller.apply(BlockCards.read(mc.level, pos, now, mc.player, m));
        return true;
    }

    public WorldObject panel() {
        return panel;
    }

    public CardController controller() {
        return controller;
    }

    public BlockPos pos() {
        return pos;
    }

    public Entity entity() {
        return entity;
    }

    void remove() {
        R3Mc.remove(panel);
    }
}
