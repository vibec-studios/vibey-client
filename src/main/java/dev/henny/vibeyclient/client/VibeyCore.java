package dev.henny.vibeyclient.client;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.net.VibeyNet;
import dev.henny.vibeyclient.module.BuiltinModules;
import dev.henny.vibeyclient.net.NetDispatch;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.VibeyScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.function.Consumer;

/**
 * Client entry of the core. The loader entry points call {@link #init} once and forward their events to the
 * {@code on*} hooks; everything else (modules, UI) only talks to the API in {@code dev.henny.vibeyclient.api}.
 */
public final class VibeyCore {
    private VibeyCore() {}

    private static ClientPlatform platform;
    private static boolean inWorld;

    public static void init(ClientPlatform p) {
        platform = p;
        GlobalSettings.load();
        GlobalSettings.openMenu.onPress(VibeyCore::openMenu);
        BuiltinModules.registerAll(ModuleManager.get());
        ModuleManager.get().load();
        NetDispatch.setClientHandler(VibeyNet::handle);
        Vibey.LOG.info("Vibey Client core ready, {} modules", ModuleManager.get().all().size());
    }

    public static ClientPlatform clientPlatform() {
        return platform;
    }

    public static void openMenu() {
        if (ClientCompat.screen() instanceof VibeyScreen) return;
        ClientCompat.openScreen(new VibeyScreen(ClientCompat.screen()));
    }

    // ============================================================================================ hooks

    /** End of every client tick. */
    public static void onTick() {
        ModuleManager mm = ModuleManager.get();
        if (!mm.isStarted()) mm.start();
        Lang.tick();
        Keybinds.tick();
        mm.tick();
    }

    public static void onHud(Gfx g) {
        if (ClientCompat.hudHidden()) return;
        ModuleManager.get().renderHud(g);
    }

    public static void onWorldJoin() {
        inWorld = true;
        VibeyNet.onJoin();
        ModuleManager.get().forEachEnabled(m -> m.onWorldJoin());
        Events.post(new VibeyEvents.WorldJoin());
    }

    public static void onWorldLeave() {
        if (!inWorld) return;
        inWorld = false;
        VibeyNet.onLeave();
        ModuleManager.get().forEachEnabled(m -> m.onWorldLeave());
        Events.post(new VibeyEvents.WorldLeave());
    }

    public static void onScreenInit(Screen screen, List<? extends GuiEventListener> widgets,
                                    Consumer<AbstractWidget> add, Consumer<AbstractWidget> remove) {
        PauseMenu.decorate(screen, widgets, add);
        Events.post(new VibeyEvents.ScreenInit(screen, widgets, add, remove));
    }
}
