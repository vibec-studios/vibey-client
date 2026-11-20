package dev.henny.vui.mc.dev;

import dev.henny.vui.dev.DevTarget;
import dev.henny.vui.dom.Document;
import dev.henny.vui.mc.VuiRuntime;
import dev.henny.vui.mc.compat.Mc;
import dev.henny.vui.mc.host.VuiScreen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

final class McDevTarget implements DevTarget {
    private final DevTools tools;
    private final VuiRuntime runtime;

    McDevTarget(DevTools tools, VuiRuntime runtime) {
        this.tools = tools;
        this.runtime = runtime;
    }

    @Override
    public List<Document> documents() {
        return runtime.vui().reloader().documents();
    }

    @Override
    public Document active() {
        if (Mc.screen() instanceof VuiScreen screen) return screen.document();
        Document top = runtime.overlays().top();
        return top != null ? top : DevTarget.super.active();
    }

    @Override
    public Document open(String id) {
        return tools.open(id).document();
    }

    @Override
    public CompletableFuture<byte[]> screenshotAsync() {
        return tools.capture().request(runtime.layers().frames());
    }
}
