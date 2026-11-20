package dev.henny.vibeyclient.module.info.pin;

import dev.henny.r3.interact.Interactable;
import dev.henny.r3.interact.InteractionEvent;
import dev.henny.r3.interact.InteractionEventKind;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.mc.client.R3Client;
import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.card.CardData;
import dev.henny.vibeyclient.module.info.look.LookTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class PinnedCards {
    public static final int MAX = 6;
    private static final int REFRESH_TICKS = 5;
    private static final List<PinnedCard> CARDS = new ArrayList<>();
    private static boolean listening;
    private static int age;

    private PinnedCards() {}

    public static PinnedCard pin(CardData data, Minecraft mc) {
        listen();
        PinnedCard same = find(data);
        if (same != null) unpin(same);
        if (CARDS.size() >= MAX) unpin(CARDS.get(0));
        BlockState state = data.pos() == null ? null : mc.level.getBlockState(data.pos());
        PinnedCard card = new PinnedCard(data, state, LookTarget.face(), mc);
        CARDS.add(card);
        return card;
    }

    public static void unpin(PinnedCard card) {
        card.remove();
        CARDS.remove(card);
    }

    public static void tick(InfoModule m, Minecraft mc) {
        if (CARDS.isEmpty() || ++age % REFRESH_TICKS != 0) return;
        for (PinnedCard c : new ArrayList<>(CARDS)) if (!c.refresh(m, mc)) unpin(c);
    }

    public static PinnedCard hovered() {
        if (CARDS.isEmpty() || !R3Client.started()) return null;
        Interactable target = R3Mc.scene().interaction().hover().target();
        for (PinnedCard c : CARDS) if (c.panel() == target) return c;
        return null;
    }

    public static List<PinnedCard> all() {
        return List.copyOf(CARDS);
    }

    public static void clear() {
        if (R3Client.started()) for (PinnedCard c : CARDS) c.remove();
        CARDS.clear();
    }

    private static PinnedCard find(CardData d) {
        for (PinnedCard c : CARDS) {
            if (d.entity() != null && c.entity() == d.entity()) return c;
            if (d.pos() != null && d.pos().equals(c.pos())) return c;
        }
        return null;
    }

    private static void listen() {
        if (listening) return;
        listening = true;
        R3Mc.scene().interaction().listen(PinnedCards::event);
    }

    private static void event(InteractionEvent e) {
        if (e.kind() != InteractionEventKind.CONTEXT_MENU) return;
        for (PinnedCard c : new ArrayList<>(CARDS)) if (c.panel() == e.target()) unpin(c);
    }
}
