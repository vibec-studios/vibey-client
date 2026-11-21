package dev.henny.vibeyclient.backend;

public final class SharedBackend {
    private SharedBackend() {}

    private static BackendClient instance;

    public static synchronized BackendClient get() {
        if (instance == null) instance = new BackendClient(AuthStore.defaultStore());
        return instance;
    }
}
