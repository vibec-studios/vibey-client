package dev.henny.vibeafk;

import dev.henny.vibeafk.control.SoundControl;
import dev.henny.vibeafk.control.Throttle;
import dev.henny.vibeafk.control.WindowControl;
import dev.henny.vibeafk.hook.Hooks;

import java.util.function.BiFunction;

public final class Core implements BiFunction<String, Object, Object> {
    public static volatile Object HOOK;

    final WindowControl window = new WindowControl();
    final SoundControl sound = new SoundControl();
    final Throttle throttle = new Throttle();
    private final MainThread main;
    private volatile boolean legacy;
    private volatile Class<?> lastPollOwner;

    Core(MainThread main) {
        this.main = main;
    }

    public boolean legacy() {
        return legacy;
    }

    Class<?> lastPollOwner() {
        return lastPollOwner;
    }

    @Override
    public Object apply(String event, Object raw) {
        try {
            Object[] a = raw instanceof Object[] ? (Object[]) raw : new Object[2];
            Class<?> owner = a[0] instanceof Class ? (Class<?>) a[0] : null;
            Object arg = a.length > 1 ? a[1] : null;
            if (Hooks.SWAP.equals(event)) {
                window.onSwap(owner, arg instanceof Long ? (Long) arg : 0L);
                throttle.frame();
                return null;
            }
            if (Hooks.POLL.equals(event)) {
                lastPollOwner = owner;
                main.tick();
                return null;
            }
            if (Hooks.SWAP_LEGACY.equals(event)) {
                legacy = true;
                main.tick();
                throttle.frame();
                return null;
            }
            if (Hooks.GAIN.equals(event)) {
                return arg instanceof Float ? Float.valueOf(sound.filter(owner, (Float) arg)) : null;
            }
            if (Hooks.GLFW_SHOW.equals(event) || Hooks.SDL_SHOW.equals(event)) {
                return window.skipShow() ? Boolean.TRUE : Boolean.FALSE;
            }
            if (Hooks.GLFW_CREATE.equals(event)) {
                window.onGlfwCreate(owner, main.startHidden());
                return null;
            }
            if (Hooks.GLFW_WINDOW.equals(event)) {
                window.onGlfwWindow(owner, arg instanceof Long ? (Long) arg : 0L);
                return null;
            }
            if (Hooks.SDL_FLAGS.equals(event)) {
                long flags = arg instanceof Long ? (Long) arg : 0L;
                return Long.valueOf(window.sdlFlags(owner, flags, main.startHidden()));
            }
            if (Hooks.SDL_WINDOW.equals(event)) {
                window.onSdlWindow(owner, arg instanceof Long ? (Long) arg : 0L);
                return null;
            }
            return null;
        } catch (Throwable t) {
            main.hookError(event, t);
            return null;
        }
    }
}
