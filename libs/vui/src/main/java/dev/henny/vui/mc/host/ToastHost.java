package dev.henny.vui.mc.host;

import dev.henny.vui.Vui;
import dev.henny.vui.host.Toast;
import dev.henny.vui.host.ToastStack;
import dev.henny.vui.mc.compat.Mc;

public final class ToastHost {
    private final ToastStack stack;
    private final DocumentView view;

    public ToastHost(Vui vui) {
        stack = new ToastStack(vui);
        view = new DocumentView(stack.document(), stack::frame);
    }

    public Toast push(String title, String text, String icon, String kind, float seconds) {
        return stack.push(title, text, icon, kind, seconds);
    }

    public Toast push(String title, String text) {
        return stack.push(title, text);
    }

    public void dismiss(Toast toast) {
        stack.dismiss(toast);
    }

    public ToastStack stack() {
        return stack;
    }

    public DocumentView view() {
        return view;
    }

    void draw(Object graphics, int mouseX, int mouseY) {
        if (mouseX >= 0) view.move(mouseX, mouseY);
        view.draw(graphics, Mc.guiWidth(), Mc.guiHeight(), false);
    }

    boolean click(double x, double y, int button) {
        if (!stack.takesClick((float) x, (float) y)) return false;
        view.down(x, y, button);
        view.up(x, y, button);
        return true;
    }
}
