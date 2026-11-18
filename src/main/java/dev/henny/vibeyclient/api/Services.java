package dev.henny.vibeyclient.api;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service locator so modules can talk without hard dependencies, e.g. the multiplayer module registers a
 * {@code HostSessionService} and social looks it up. Register in {@code onLoad}/{@code onEnable}, look up lazily
 * (never cache across enable/disable), unregister in {@code onDisable} if the service only works while enabled.
 */
public final class Services {
    private Services() {}

    private static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

    public static <T> void register(Class<T> type, T implementation) {
        SERVICES.put(type, type.cast(implementation));
    }

    public static <T> void unregister(Class<T> type, T implementation) {
        SERVICES.remove(type, implementation);
    }

    public static <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(type.cast(SERVICES.get(type)));
    }

    public static <T> T require(Class<T> type) {
        T t = type.cast(SERVICES.get(type));
        if (t == null) throw new IllegalStateException("No service registered for " + type.getName());
        return t;
    }
}
