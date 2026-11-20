package dev.henny.vibeyclient.module.multiplayer;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.multiplayer.host.HostSession;
import dev.henny.vibeyclient.module.multiplayer.screen.AdminScreen;
import dev.henny.vibeyclient.module.multiplayer.screen.HostScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Dev-only visual check for loaders without client game tests (NeoForge): {@code -Dvibeyclient.mpcheck=true} in a dev
 * workspace walks through the host screen, the Vibey menu, a fresh world, the pause menu and (with
 * {@code -Dvibeyclient.mpcheck.relay=host:port}, a LOCAL relay) hosting, saves a screenshot of each step into
 * {@code run/screenshots} and quits. Version-specific calls go through reflection; nothing here runs without both
 * the dev environment and the property.
 */
final class MpDevCheck {
    private MpDevCheck() {}

    private static int step = -1;
    private static int wait;
    private static int tries;

    static boolean enabled() {
        return Vibey.platform().isDevelopment() && Boolean.getBoolean("vibeyclient.mpcheck");
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (step < 0) {
            Screen current = ClientCompat.screen();
            // A fresh game directory opens the accessibility onboarding first.
            if (current != null && current.getClass().getSimpleName().contains("Onboarding")) {
                ClientCompat.openScreen(new TitleScreen());
                return;
            }
            if (current instanceof TitleScreen) {
                step = 0;
                wait = 60;
                GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            }
            return;
        }
        if (--wait > 0) return;
        wait = 20;
        String relay = System.getProperty("vibeyclient.mpcheck.relay", "");
        try {
            switch (step++) {
                case 0 -> ClientCompat.openScreen(new HostScreen(ClientCompat.screen()));
                case 1 -> {
                    shot("mpcheck_1_host_title");
                    dev.henny.vibeyclient.ui.vui.Menus.openModule(null, "multiplayer");
                }
                case 2 -> {
                    shot("mpcheck_2_menu_multiplayer");
                    ClientCompat.openScreen(null);
                    createWorld(mc);
                }
                case 3 -> pressCreate();
                case 4 -> {
                    if (mc.level == null || mc.player == null) {
                        if (++tries > 60) throw new IllegalStateException("world did not open");
                        step--;
                        return;
                    }
                    wait = 80;
                }
                case 5 -> ClientCompat.openScreen(new PauseScreen(true));
                case 6 -> {
                    shot("mpcheck_3_pause_menu");
                    ClientCompat.openScreen(new HostScreen(null));
                }
                case 7 -> {
                    shot("mpcheck_4_host_world");
                    if (!relay.isEmpty()) {
                        MultiplayerModule.instance().relay.set(relay);
                        HostSession.start();
                        tries = 0;
                    }
                }
                case 8 -> {
                    if (!relay.isEmpty() && HostSession.status() != HostSession.Status.ONLINE && HostSession.status() != HostSession.Status.ERROR
                            && ++tries < 30) {
                        step--;
                        return;
                    }
                    Vibey.LOG.info("[vibeyclient-mpcheck] hosting status {} address {} error '{}'", HostSession.status(), HostSession.address(), HostSession.error());
                    shot("mpcheck_5_host_online");
                    ClientCompat.openScreen(new AdminScreen(new HostScreen(null)));
                }
                case 9 -> {
                    shot("mpcheck_6_admin");
                    HostSession.stop();
                    ClientCompat.openScreen(null);
                }
                default -> {
                    Vibey.LOG.info("[vibeyclient-mpcheck] done");
                    mc.stop();
                }
            }
        } catch (Throwable t) {
            Vibey.LOG.error("[vibeyclient-mpcheck] step {} failed", step - 1, t);
            mc.stop();
        }
    }

    private static void shot(String name) throws ReflectiveOperationException {
        Minecraft mc = Minecraft.getInstance();
        Class<?> c = Class.forName("net.minecraft.client.Screenshot");
        for (Method m : c.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (!m.getName().equals("grab") || p.length < 4 || p[0] != File.class || p[1] != String.class) continue;
            Object[] args = new Object[p.length];
            args[0] = mc.gameDirectory;
            args[1] = name + ".png";
            args[2] = renderTarget(mc, p[2]);
            for (int i = 3; i < p.length; i++) {
                if (p[i] == int.class) args[i] = 1;
                else args[i] = (java.util.function.Consumer<Object>) msg -> Vibey.LOG.info("[vibeyclient-mpcheck] screenshot {}", name);
            }
            m.invoke(null, args);
            return;
        }
        throw new NoSuchMethodException("Screenshot.grab");
    }

    /** The main render target (the getter's name differs between versions). */
    private static Object renderTarget(Minecraft mc, Class<?> type) throws ReflectiveOperationException {
        for (Method m : Minecraft.class.getMethods()) {
            if (m.getParameterCount() == 0 && type.isAssignableFrom(m.getReturnType()) && !java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                return m.invoke(mc);
            }
        }
        throw new NoSuchMethodException("Minecraft -> " + type.getName());
    }

    private static void createWorld(Minecraft mc) throws ReflectiveOperationException {
        Class<?> c = Class.forName("net.minecraft.client.gui.screens.worldselection.CreateWorldScreen");
        for (Method m : c.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (m.getName().equals("openFresh") && p.length >= 1 && p[0] == Minecraft.class) {
                Object[] args = new Object[p.length];
                args[0] = mc;
                for (int i = 1; i < p.length; i++) {
                    if (p[i] == Screen.class) args[i] = new TitleScreen();
                    else if (p[i] == Runnable.class) args[i] = (Runnable) () -> ClientCompat.openScreen(new TitleScreen());
                }
                m.invoke(null, args);
                return;
            }
        }
        throw new NoSuchMethodException("CreateWorldScreen.openFresh");
    }

    private static void pressCreate() throws ReflectiveOperationException {
        Screen s = ClientCompat.screen();
        if (s == null) throw new IllegalStateException("no create world screen");
        for (var l : s.children()) {
            if (l instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc && tc.getKey().equals("selectWorld.create")) {
                for (Class<?> k = b.getClass(); k != null; k = k.getSuperclass()) {
                    for (Method m : k.getDeclaredMethods()) {
                        if (!m.getName().equals("onPress")) continue;
                        m.setAccessible(true);
                        if (m.getParameterCount() == 0) m.invoke(b);
                        else {
                            Object key = Class.forName("net.minecraft.client.input.KeyEvent")
                                    .getConstructor(int.class, int.class, int.class).newInstance(257, 0, 0);
                            m.invoke(b, key);
                        }
                        return;
                    }
                }
            }
        }
        throw new IllegalStateException("no Create button on " + s.getClass().getName());
    }
}
