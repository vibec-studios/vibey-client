package dev.henny.vui.host;

import dev.henny.vui.Vui;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.dom.Document;
import dev.henny.vui.render.Canvas;

public final class ToastStack {
    public static final int MAX_VISIBLE = 5;

    private static final String TEMPLATE = """
            <template>
              <column id="toasts">
                <toast v-for="t in toasts" :key="t.id" :class="t.kind" @click="dismiss(t)">
                  <icon v-if="t.icon" :src="t.icon"/>
                  <column class="toast-body">
                    <text class="toast-title">{{ t.title }}</text>
                    <text v-if="t.text" class="toast-text">{{ t.text }}</text>
                  </column>
                </toast>
              </column>
            </template>
            <style>
            #toasts { position: absolute; anchor: bottom-right; right: 6; bottom: 6; gap: 4; align-items: end; }
            .toast-body { gap: 1; }
            .toast-title { color: $text; }
            .toast-text { color: $muted; }
            toast:enter, toast:leave { opacity: 0; translate-y: 8; }
            toast.ok { border-color: $ok; }
            toast.warn { border-color: $warn; }
            toast.error { border-color: $danger; }
            </style>
            """;

    private final ListState<Toast> toasts = new ListState<>();
    private final Document document;

    public ToastStack(Vui vui) {
        this(vui, vui.compile("vui:toasts", "<vui:toasts>", TEMPLATE));
    }

    public ToastStack(Vui vui, dev.henny.vui.template.CompiledTemplate template) {
        this.document = vui.create(template, new Controller());
    }

    public Document document() {
        return document;
    }

    public ListState<Toast> toasts() {
        return toasts;
    }

    public Toast push(String title, String text, String icon, String kind, float seconds) {
        Toast t = new Toast(title, text, icon, kind, seconds);
        toasts.add(t);
        while (toasts.size() > MAX_VISIBLE) toasts.remove(0);
        return t;
    }

    public Toast push(String title, String text) {
        return push(title, text, null, "info", 4f);
    }

    public void dismiss(Toast t) {
        toasts.remove(t);
    }

    public void resize(float w, float h) {
        document.resize(w, h);
    }

    public void frame(float dt) {
        for (int i = toasts.size() - 1; i >= 0; i--) if (toasts.get(i).tick(dt)) toasts.remove(i);
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
    }
}
