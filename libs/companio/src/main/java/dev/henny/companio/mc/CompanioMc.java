package dev.henny.companio.mc;

import dev.henny.companio.config.CompanioConfig;
import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.mc.client.CompanioClient;
import dev.henny.companio.mc.screen.PacksController;
import dev.henny.companio.mc.screen.RadialController;
import dev.henny.companio.pack.PackRegistry;
import dev.henny.companio.sync.SyncPort;
import dev.henny.companio.world.Command;
import dev.henny.companio.world.Companion;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.UUID;

public final class CompanioMc {
    private CompanioMc() {}

    public static boolean ready() {
        return CompanioClient.get() != null;
    }

    public static CompanioConfig config() {
        return CompanioClient.get().config();
    }

    public static PackRegistry packs() {
        return CompanioClient.get().registry();
    }

    public static List<CompanionDef> companions() {
        CompanioClient c = CompanioClient.get();
        return c == null ? List.of() : c.registry().companions(c.config().active());
    }

    public static Companion own() {
        CompanioClient c = CompanioClient.get();
        return c == null ? null : c.own();
    }

    public static void changed() {
        CompanioClient c = CompanioClient.get();
        if (c != null) c.changed();
    }

    public static boolean select(String key) {
        CompanioClient c = CompanioClient.get();
        if (c == null) return false;
        if (key.isEmpty()) {
            c.config().enabled = false;
            c.changed();
            return true;
        }
        if (c.registry().companion(key, c.config().active()) == null) return false;
        if (!key.equals(c.config().selected)) {
            c.config().selected = key;
            c.config().variant = "";
            c.config().size = 0;
        }
        c.config().enabled = true;
        c.changed();
        return true;
    }

    public static void command(Command cmd) {
        if (ready()) CompanioClient.get().command(cmd);
    }

    public static void rename(String name) {
        if (ready()) CompanioClient.get().rename(name);
    }

    public static void size(float size) {
        if (ready()) CompanioClient.get().setSize(size);
    }

    public static boolean trick(String id) {
        Companion c = own();
        return c != null && c.trick(id);
    }

    public static void recall() {
        Companion c = own();
        CompanioClient client = CompanioClient.get();
        if (c != null && Minecraft.getInstance().level != null) c.teleportToOwner(new dev.henny.companio.mc.world.McBlockView(Minecraft.getInstance().level));
    }

    public static void openPacks() {
        PacksController.open();
    }

    public static void openMenu() {
        RadialController.open();
    }

    public static void reload() {
        if (ready()) CompanioClient.get().repository().reload();
    }

    public static void toggleDebug() {
        if (ready()) CompanioClient.get().overlay().toggleDebug();
    }

    public static void hidden(boolean hidden) {
        if (ready()) CompanioClient.get().config().hostHidden = hidden;
    }

    public static void serverHidden(boolean hidden) {
        if (ready()) CompanioClient.get().config().serverHidden = hidden;
    }

    public static void sync(SyncPort port) {
        if (ready()) CompanioClient.get().port(port);
    }

    public static void receive(UUID player, String payload) {
        if (ready()) CompanioClient.get().receive(player, payload);
    }

    public static void forget(UUID player) {
        if (ready()) CompanioClient.get().forget(player);
    }
}
