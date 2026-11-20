package dev.henny.companio.mc.interact;

import dev.henny.companio.mc.client.CompanioClient;
import dev.henny.companio.world.Companion;
import dev.henny.r3.interact.InteractionEvent;
import dev.henny.r3.interact.InteractionEventKind;
import dev.henny.r3.mc.R3Mc;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3d;

public final class Interactions {
    private final CompanioClient client;
    private boolean listening;

    public Interactions(CompanioClient client) {
        this.client = client;
    }

    public void ensureListening() {
        if (listening || !dev.henny.r3.mc.client.R3Client.started()) return;
        listening = true;
        R3Mc.scene().interaction().listen(this::event);
    }

    private void event(InteractionEvent e) {
        CompanionTarget t = client.target();
        if (t.object() == null || e.target() != t.object()) return;
        Companion c = client.own();
        if (c == null) return;
        switch (e.kind()) {
            case ACTION -> c.pet();
            case GRAB_STARTED -> c.pickUp();
            case PLACED, MOVE_CANCELLED -> place(c, t);
            case UNDONE -> t.reanchor();
            case RESIZED -> resize(c, t);
            case CONTEXT_MENU -> menu(c);
            default -> { }
        }
    }

    private void place(Companion c, CompanionTarget t) {
        Vector3d p = t.object().transform().position();
        c.place(p.x, p.y - c.body.height * 0.5, p.z);
        t.reanchor();
    }

    private void resize(Companion c, CompanionTarget t) {
        float factor = t.object().transform().scale();
        client.setSize(c.size() * factor);
        t.object().transform().scale(1f);
    }

    private void menu(Companion c) {
        ItemStack held = Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getMainHandItem();
        Component custom = held.is(Items.NAME_TAG) ? held.get(DataComponents.CUSTOM_NAME) : null;
        if (custom != null) {
            client.rename(custom.getString());
            return;
        }
        client.openMenu();
    }
}
