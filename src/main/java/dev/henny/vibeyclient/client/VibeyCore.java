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
    private static boolean smokeDone;

    public static void init(ClientPlatform p) {
        platform = p;
        GlobalSettings.load();
        GlobalSettings.openMenu.onPress(VibeyCore::openMenu);
        dev.henny.vibeyclient.ui.toast.ToastHooks.init();
        dev.henny.vibeyclient.ui.title.CoreMenus.register();
        BuiltinModules.registerAll(ModuleManager.get());
        ModuleManager.get().load();
        ServerRulesClient.init();
        NetDispatch.setClientHandler(VibeyNet::handle);
        dev.henny.vibeyclient.dev.DevDriver.startIfRequested();
        dev.henny.vibeyclient.launcher.LauncherStatus.init(Vibey.platform().gameDir());
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
        dev.henny.vibeyclient.dev.DevTourB.tick();
        ModuleManager mm = ModuleManager.get();
        if (!mm.isStarted()) mm.start();
        Lang.tick();
        Keybinds.tick();
        ServerRulesClient.tick();
        mm.tick();
        dev.henny.vibeyclient.launcher.LauncherStatus.tick();
        dev.henny.vibeyclient.ui.title.UiShot.tick();
        dev.henny.vibeyclient.dev.DevTour.tick();
        dev.henny.vibeyclient.dev.ClickTest.tick();
    }

    public static void onHud(Gfx g) {
        if (ClientCompat.hudHidden()) return;
        ModuleManager.get().renderHud(g);
        dev.henny.vibeyclient.ui.hud.HudLayout.renderWidgets(g);
        dev.henny.vibeyclient.ui.toast.ToastHooks.renderHud(g);
        // vanilla draws its toasts right after the HUD in the same layer, where text goes over shapes: without a new
        // layer our widgets' text (e.g. the clock top right) would show through an advancement toast
        g.layer();
    }

    public static void onWorldJoin() {
        inWorld = true;
        ServerRulesClient.onJoin();
        VibeyNet.onJoin();
        ModuleManager.get().forEachEnabled(m -> m.onWorldJoin());
        Events.post(new VibeyEvents.WorldJoin());
    }

    public static void onWorldLeave() {
        if (!inWorld) return;
        inWorld = false;
        ServerRulesClient.onLeave();
        VibeyNet.onLeave();
        ModuleManager.get().forEachEnabled(m -> m.onWorldLeave());
        Events.post(new VibeyEvents.WorldLeave());
    }

    public static void onScreenInit(Screen screen, List<? extends GuiEventListener> widgets,
                                    Consumer<AbstractWidget> add, Consumer<AbstractWidget> remove) {
        PauseMenu.decorate(screen, widgets, add);
        // A fresh game directory opens the accessibility onboarding first; both mean "started".
        boolean start = screen instanceof net.minecraft.client.gui.screens.TitleScreen
                || screen.getClass().getSimpleName().contains("Onboarding");
        if (start && dev.henny.vibeyclient.Smoke.enabled() && !smokeDone) {
            smokeDone = true;
            ModuleManager.get().start();
            StringBuilder sb = new StringBuilder();
            for (dev.henny.vibeyclient.api.VibeyModule m : ModuleManager.get().all()) sb.append(m.id()).append('=').append(m.status()).append(' ');
            Vibey.LOG.info("[vibeyclient-smoke] client reached the title screen on {}, modules: {}", Vibey.platform().loader(), sb.toString().trim());
            net.minecraft.client.Minecraft.getInstance().execute(() -> net.minecraft.client.Minecraft.getInstance().stop());
        }
        Events.post(new VibeyEvents.ScreenInit(screen, widgets, add, remove));
    }
}
