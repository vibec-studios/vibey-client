package dev.henny.vui.mc.resource;

import dev.henny.vui.mc.compat.Mc;

public final class ReloadWatch {
    private boolean loading;

    public void tick(Runnable onReloaded) {
        boolean now = Mc.loadingOverlay();
        if (loading && !now) onReloaded.run();
        loading = now;
    }
}
