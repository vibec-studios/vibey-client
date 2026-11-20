package dev.henny.vui.host;

import dev.henny.vui.Vui;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.template.CompiledTemplate;

import java.util.function.BiConsumer;

public final class ToastStack {
    public static final int MAX_VISIBLE = 5;

    private final ListState<Toast> toasts = new ListState<>();
    private final Document document;
    private BiConsumer<Toast, String> actionListener = (t, id) -> {
    };

    public ToastStack(Vui vui) {
        this(vui, vui.compile("vui:toasts", "<vui:toasts>", ToastTemplate.SOURCE));
    }

    public ToastStack(Vui vui, CompiledTemplate template) {
        this.document = vui.create(template, new Controller());
    }

    public Document document() {
        return document;
    }

    public ListState<Toast> toasts() {
        return toasts;
    }

    public ToastStack onAction(BiConsumer<Toast, String> listener) {
        actionListener = listener == null ? (t, id) -> {
        } : listener;
        return this;
    }

    public Toast push(Toast t) {
        toasts.add(t);
        while (toasts.size() > MAX_VISIBLE) toasts.remove(0);
        return t;
    }

    public Toast push(String title, String text, String icon, String kind, float seconds) {
        return push(new Toast(title, text, icon, kind, seconds));
    }

    public Toast push(String title, String text) {
        return push(title, text, null, "info", 4f);
    }

    public void dismiss(Toast t) {
        toasts.remove(t);
    }

    public void act(Toast t) {
        t.run();
        actionListener.accept(t, t.getActionId());
        dismiss(t);
    }

    public boolean paused() {
        Element t = toastOf(document.events().hovered());
        return t != null && hasButton(t);
    }

    public boolean takesClick(float x, float y) {
        if (toasts.size() == 0) return false;
        document.layoutIfNeeded();
        for (Element e = document.events().hit(x, y); e != null; e = e.parent()) {
            if (e.tag().equals("button")) return true;
            if (e.tag().equals("toast")) return false;
        }
        return false;
    }

    private static Element toastOf(Element e) {
        for (; e != null; e = e.parent()) if (e.tag().equals("toast")) return e;
        return null;
    }

    private static boolean hasButton(Element e) {
        if (e.tag().equals("button")) return true;
        for (int i = 0; i < e.childCount(); i++) if (hasButton(e.child(i))) return true;
        return false;
    }

    public void resize(float w, float h) {
        document.resize(w, h);
    }

    public void frame(float dt) {
        if (!paused()) for (int i = toasts.size() - 1; i >= 0; i--) if (toasts.get(i).tick(dt)) toasts.remove(i);
        document.frame(dt);
    }

    public void render(Canvas canvas) {
        document.render(canvas);
    }

    public final class Controller {
        public ListState<Toast> getToasts() {
            return toasts;
        }

        public void dismiss(Toast t) {
            ToastStack.this.dismiss(t);
        }

        public void act(Toast t) {
            ToastStack.this.act(t);
        }
    }
}
