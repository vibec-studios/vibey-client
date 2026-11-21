package dev.henny.vibeafk.hook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Target {
    public enum Kind { NOTIFY, NOTIFY_ARG0, SKIP_IF_TRUE, CREATE_GLFW, CREATE_SDL, GAIN, AFTER }

    public final String owner;
    public final String name;
    public final String descPrefix;
    public final String descSuffix;
    public final Kind kind;
    public final String event;

    private Target(String owner, String name, String descPrefix, String descSuffix, Kind kind, String event) {
        this.owner = owner;
        this.name = name;
        this.descPrefix = descPrefix;
        this.descSuffix = descSuffix;
        this.kind = kind;
        this.event = event;
    }

    public boolean matches(String n, String desc) {
        return name.equals(n) && desc.startsWith(descPrefix) && desc.endsWith(descSuffix);
    }

    public static final String GLFW = "org/lwjgl/glfw/GLFW";
    public static final String SDL_VIDEO = "org/lwjgl/sdl/SDLVideo";
    public static final String SDL_EVENTS = "org/lwjgl/sdl/SDLEvents";
    public static final String VK_SWAPCHAIN = "org/lwjgl/vulkan/KHRSwapchain";
    public static final String AL10 = "org/lwjgl/openal/AL10";
    public static final String AL = "org/lwjgl/openal/AL";
    public static final String LWJGL2_DISPLAY = "org/lwjgl/opengl/Display";

    private static final Map<String, List<Target>> BY_OWNER;

    static {
        List<Target> all = new ArrayList<Target>();
        all.add(new Target(GLFW, "glfwSwapBuffers", "(J)", "V", Kind.NOTIFY_ARG0, Hooks.SWAP));
        all.add(new Target(GLFW, "glfwPollEvents", "()", "V", Kind.NOTIFY, Hooks.POLL));
        all.add(new Target(GLFW, "glfwCreateWindow", "(", ")J", Kind.CREATE_GLFW, Hooks.GLFW_WINDOW));
        all.add(new Target(GLFW, "glfwShowWindow", "(J)", "V", Kind.SKIP_IF_TRUE, Hooks.GLFW_SHOW));
        all.add(new Target(SDL_VIDEO, "SDL_GL_SwapWindow", "(J)", "Z", Kind.NOTIFY_ARG0, Hooks.SWAP));
        all.add(new Target(SDL_VIDEO, "SDL_CreateWindow", "(", "J)J", Kind.CREATE_SDL, Hooks.SDL_WINDOW));
        all.add(new Target(SDL_VIDEO, "SDL_ShowWindow", "(J)", "Z", Kind.SKIP_IF_TRUE, Hooks.SDL_SHOW));
        all.add(new Target(SDL_EVENTS, "SDL_PumpEvents", "()", "V", Kind.NOTIFY, Hooks.POLL));
        all.add(new Target(SDL_EVENTS, "SDL_PollEvent", "(", ")Z", Kind.NOTIFY, Hooks.POLL));
        all.add(new Target(VK_SWAPCHAIN, "vkQueuePresentKHR", "(", ")I", Kind.NOTIFY, Hooks.SWAP));
        all.add(new Target(AL10, "alListenerf", "(IF)", "V", Kind.GAIN, Hooks.GAIN));
        all.add(new Target(AL, "createCapabilities", "(Lorg/lwjgl/openal/ALCCapabilities;", ")Lorg/lwjgl/openal/ALCapabilities;", Kind.AFTER, Hooks.AL_READY));
        all.add(new Target(AL, "create", "(Ljava/lang/String;IIZ", ")V", Kind.AFTER, Hooks.AL_READY));
        all.add(new Target(LWJGL2_DISPLAY, "update", "(Z)", "V", Kind.NOTIFY, Hooks.SWAP_LEGACY));
        Map<String, List<Target>> m = new HashMap<String, List<Target>>();
        for (Target t : all) {
            List<Target> l = m.get(t.owner);
            if (l == null) m.put(t.owner, l = new ArrayList<Target>());
            l.add(t);
        }
        BY_OWNER = Collections.unmodifiableMap(m);
    }

    public static Set<String> owners() {
        return BY_OWNER.keySet();
    }

    public static List<Target> forOwner(String owner) {
        List<Target> l = BY_OWNER.get(owner);
        return l == null ? Collections.<Target>emptyList() : l;
    }
}
