package dev.henny.vibeyclient.api.event;

import dev.henny.vibeyclient.Vibey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Small typed event bus. Events are plain objects (records in {@link VibeyEvents}, or your own); listeners get
 * exactly the class they subscribed to. {@link #post} calls listeners synchronously on the posting thread; core
 * events are posted on the client thread. A failing listener is logged and does not stop the others.
 */
public final class Events {
    private Events() {}

    private static final Map<Class<?>, List<Consumer<Object>>> LISTENERS = new ConcurrentHashMap<>();

    /** Handle to stop listening. */
    public interface Subscription {
        void cancel();
    }

    @SuppressWarnings("unchecked")
    public static <E> Subscription subscribe(Class<E> type, Consumer<? super E> listener) {
        Consumer<Object> c = e -> ((Consumer<Object>) listener).accept(e);
        List<Consumer<Object>> list = LISTENERS.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
        list.add(c);
        return () -> list.remove(c);
    }

    public static <E> E post(E event) {
        List<Consumer<Object>> list = LISTENERS.get(event.getClass());
        if (list == null) return event;
        for (Consumer<Object> l : list) {
            try {
                l.accept(event);
            } catch (Throwable t) {
                Vibey.LOG.error("Listener for {} failed", event.getClass().getSimpleName(), t);
            }
        }
        return event;
    }
}
