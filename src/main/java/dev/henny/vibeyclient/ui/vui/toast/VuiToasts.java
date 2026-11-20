package dev.henny.vibeyclient.ui.vui.toast;

import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.ui.toast.ToastStack;
import dev.henny.vibeyclient.ui.vui.ClassicUi;
import dev.henny.vibeyclient.ui.vui.VibeyVui;
import dev.henny.vui.host.Toast;
import dev.henny.vui.mc.compat.Mc;
import dev.henny.vui.mc.host.DocumentView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class VuiToasts {
    private VuiToasts() {}

    public static final String DOC = "vibeyclient:toast/stack";

    private static final ConcurrentLinkedQueue<ToastRequest> INCOMING = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<String> DISMISS = new ConcurrentLinkedQueue<>();
    private static final Map<String, Toast> BY_KEY = new HashMap<>();
    private static dev.henny.vui.host.ToastStack stack;
    private static DocumentView view;

    public static void init() {
        Notify.setSink(t -> {
            if (ClassicUi.enabled()) ToastStack.show(t);
            else INCOMING.add(ToastRequest.of(t));
        });
    }

    public static void show(Notify.Toast t, List<ToastStack.Button> buttons, int ms, String key) {
        ToastStack.Button b = buttons.isEmpty() ? null : buttons.get(0);
        Runnable run = b != null ? b.action() : t.action();
        String label = b != null ? b.label() : run != null ? dev.henny.vibeyclient.api.Lang.tr("ui.toast.open") : "";
        int duration = ms > 0 ? ms : run != null ? ToastRequest.ACTION_MS : ToastRequest.DEFAULT_MS;
        INCOMING.add(new ToastRequest(t, label, run, duration, key));
    }

    public static void dismiss(String key) {
        if (key != null) DISMISS.add(key);
    }

    public static dev.henny.vui.host.ToastStack stack() {
        ensure();
        return stack;
    }

    public static void render(Object graphics, int mouseX, int mouseY) {
        if (stack == null && INCOMING.isEmpty()) return;
        ensure();
        pump();
        if (mouseX >= 0) view.move(mouseX, mouseY);
        view.draw(graphics, Mc.guiWidth(), Mc.guiHeight(), false);
    }

    public static boolean click(double x, double y, int button) {
        if (stack == null || stack.toasts().size() == 0 || !view.hits(x, y)) return false;
        view.down(x, y, button);
        view.up(x, y, button);
        return true;
    }

    private static void ensure() {
        if (stack != null) return;
        var vui = VibeyVui.vui();
        stack = new dev.henny.vui.host.ToastStack(vui, vui.registry().template(DOC));
        view = new DocumentView(stack.document(), stack::frame);
    }

    private static void pump() {
        for (String key; (key = DISMISS.poll()) != null; ) {
            Toast t = BY_KEY.remove(key);
            if (t != null) stack.dismiss(t);
        }
        for (ToastRequest r; (r = INCOMING.poll()) != null; ) {
            if (r.key() != null) {
                Toast old = BY_KEY.remove(r.key());
                if (old != null) stack.dismiss(old);
            }
            Toast t = stack.push(r.build());
            if (r.key() != null) BY_KEY.put(r.key(), t);
        }
    }
}
