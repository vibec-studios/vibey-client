package dev.henny.companio.mc.screen;

import dev.henny.companio.def.TrickDef;
import dev.henny.companio.mc.client.CompanioClient;
import dev.henny.companio.world.Command;
import dev.henny.companio.world.Companion;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.host.VuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;

public final class RadialController {
    public record Item(String id, String label, String icon, float x, float y, boolean on) {}

    public final ListState<Item> items = new ListState<>();
    public final State<Boolean> naming = new State<>(false);
    public final State<String> name = new State<>("");
    public float cx, cy;
    public String title = "", subtitle = "";
    private final CompanioClient client;
    private VuiScreen screen;
    private boolean tricks;

    RadialController(CompanioClient client) {
        this.client = client;
        Minecraft mc = Minecraft.getInstance();
        cx = mc.getWindow().getGuiScaledWidth() / 2f;
        cy = mc.getWindow().getGuiScaledHeight() / 2f;
        Companion c = client.own();
        if (c != null) {
            title = c.name().isEmpty() ? c.def.name().resolve(c.def.lang(), mc.options.languageCode) : c.name();
            subtitle = I18n.get("companio.menu.mode." + c.command().id());
            name.set(c.name());
        }
        build();
    }

    private void build() {
        Companion c = client.own();
        List<Item> list = new ArrayList<>();
        if (c == null) {
            items.replaceAll(list);
            return;
        }
        if (tricks) {
            for (TrickDef t : c.def.tricks()) list.add(new Item("trick:" + t.id(), t.name().resolve(c.def.lang(), Minecraft.getInstance().options.languageCode), t.icon(), 0, 0, false));
            list.add(new Item("back", I18n.get("companio.menu.back"), "◀", 0, 0, false));
        } else {
            list.add(new Item("follow", I18n.get("companio.menu.follow"), "➜", 0, 0, c.command() == Command.FOLLOW));
            list.add(new Item("sit", I18n.get("companio.menu.sit"), "▼", 0, 0, c.command() == Command.SIT));
            list.add(new Item("stay", I18n.get("companio.menu.stay"), "■", 0, 0, c.command() == Command.STAY));
            if (!c.def.tricks().isEmpty()) list.add(new Item("tricks", I18n.get("companio.menu.tricks"), "★", 0, 0, false));
            list.add(new Item("name", I18n.get("companio.menu.name"), "✎", 0, 0, false));
        }
        List<Item> placed = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            double a = -Math.PI / 2 + Math.PI * 2 * i / list.size();
            Item it = list.get(i);
            placed.add(new Item(it.id(), it.label(), it.icon(), cx + (float) Math.cos(a) * 62, cy + (float) Math.sin(a) * 62, it.on()));
        }
        items.replaceAll(placed);
    }

    public void choose(Item it) {
        Companion c = client.own();
        if (c == null) {
            close();
            return;
        }
        switch (it.id()) {
            case "follow" -> done(() -> client.command(Command.FOLLOW));
            case "sit" -> done(() -> client.command(Command.SIT));
            case "stay" -> done(() -> client.command(Command.STAY));
            case "tricks" -> {
                tricks = true;
                build();
            }
            case "back" -> {
                tricks = false;
                build();
            }
            case "name" -> naming.set(true);
            default -> {
                if (it.id().startsWith("trick:")) done(() -> c.trick(it.id().substring(6)));
            }
        }
    }

    private void done(Runnable r) {
        r.run();
        close();
    }

    public void saveName() {
        client.rename(name.peek());
        close();
    }

    public void close() {
        if (screen != null) screen.onClose();
    }

    public static void open() {
        CompanioClient client = CompanioClient.get();
        if (client == null || client.own() == null) return;
        RadialController c = new RadialController(client);
        c.screen = VuiMc.screen("companio:radial", c).pause(false).background(false);
        c.screen.open();
    }
}
