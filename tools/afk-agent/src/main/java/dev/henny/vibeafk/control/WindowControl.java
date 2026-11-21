package dev.henny.vibeafk.control;

import java.lang.reflect.Method;

public final class WindowControl {
    public static final int GLFW_VISIBLE = 0x00020004;
    public static final long SDL_WINDOW_HIDDEN = 0x8L;
    private static final String SDL_VIDEO = "org.lwjgl.sdl.SDLVideo";

    private volatile Class<?> glfw;
    private volatile long glfwWindow;
    private volatile Class<?> sdl;
    private volatile long sdlWindow;
    private volatile boolean hidden;
    private boolean self;
    private String lastError;

    public boolean available() {
        return (glfw != null && glfwWindow != 0) || (sdl != null && sdlWindow != 0);
    }

    public boolean hidden() {
        return hidden;
    }

    public String lastError() {
        return lastError;
    }

    public void onGlfwCreate(Class<?> glfwClass, boolean startHidden) {
        glfw = glfwClass;
        if (!startHidden) return;
        try {
            Method hint = glfwClass.getMethod("glfwWindowHint", int.class, int.class);
            hint.invoke(null, GLFW_VISIBLE, 0);
            hidden = true;
        } catch (Throwable t) {
            lastError = t.toString();
        }
    }

    public void onGlfwWindow(Class<?> glfwClass, long window) {
        glfw = glfwClass;
        if (window != 0) glfwWindow = window;
    }

    public long sdlFlags(Class<?> sdlClass, long flags, boolean startHidden) {
        sdl = sdlClass;
        if (!startHidden) return flags;
        hidden = true;
        return flags | SDL_WINDOW_HIDDEN;
    }

    public void onSdlWindow(Class<?> sdlClass, long window) {
        sdl = sdlClass;
        if (window != 0) sdlWindow = window;
    }

    public void onSwap(Class<?> owner, long window) {
        if (owner == null || window == 0) return;
        String n = owner.getName();
        if (n.equals("org.lwjgl.glfw.GLFW")) {
            glfw = owner;
            glfwWindow = window;
        } else if (n.equals(SDL_VIDEO)) {
            sdl = owner;
            sdlWindow = window;
        }
    }

    public boolean skipShow() {
        return hidden && !self;
    }

    public boolean hide() {
        if (!available()) return false;
        self = true;
        try {
            if (sdl != null && sdlWindow != 0) invoke(sdl, "SDL_HideWindow", sdlWindow);
            else invoke(glfw, "glfwHideWindow", glfwWindow);
            hidden = true;
            return true;
        } catch (Throwable t) {
            lastError = t.toString();
            return false;
        } finally {
            self = false;
        }
    }

    public boolean show() {
        if (!available()) return false;
        self = true;
        try {
            if (sdl != null && sdlWindow != 0) {
                invoke(sdl, "SDL_ShowWindow", sdlWindow);
                tryInvoke(sdl, "SDL_RaiseWindow", sdlWindow);
            } else {
                invoke(glfw, "glfwShowWindow", glfwWindow);
                tryInvoke(glfw, "glfwFocusWindow", glfwWindow);
            }
            hidden = false;
            return true;
        } catch (Throwable t) {
            lastError = t.toString();
            return false;
        } finally {
            self = false;
        }
    }

    private static void invoke(Class<?> c, String name, long window) throws Exception {
        c.getMethod(name, long.class).invoke(null, window);
    }

    private static void tryInvoke(Class<?> c, String name, long window) {
        try {
            invoke(c, name, window);
        } catch (Throwable ignored) {
        }
    }
}
