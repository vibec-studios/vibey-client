package dev.henny.vui.host;

import dev.henny.vui.dom.Element;
import dev.henny.vui.headless.Headless;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToastActionTest {
    private static ToastStack stack() {
        dev.henny.vui.Vui vui = Headless.create().vui();
        vui.icons().put(new dev.henny.vui.icon.IconSet("vibeyclient").put("host", "#"));
        ToastStack s = new ToastStack(vui);
        s.resize(300, 200);
        return s;
    }

    private static void settle(ToastStack s) {
        for (int i = 0; i < 4; i++) s.frame(0.05f);
    }

    @Test
    void actionButtonRunsTheActionAndDismisses() {
        ToastStack s = stack();
        List<String> ran = new ArrayList<>();
        s.onAction((t, id) -> ran.add("listener:" + id));
        s.push(new Toast("PolarPia", "Invites you", "vibeyclient:host", "info", 5f).action("Join", "join", () -> ran.add("run")));
        settle(s);
        Element button = s.document().query(".toast-action").get(0);
        float x = button.box().x + 2;
        float y = button.box().y + 2;
        s.document().pointerMove(x, y);
        s.document().pointerDown(x, y, 0, 0);
        s.document().pointerUp(x, y, 0, 0);
        assertEquals(List.of("run", "listener:join"), ran);
        assertEquals(0, s.toasts().size());
        assertTrue(s.document().diagnostics().isEmpty(), s.document().diagnostics().format());
    }

    @Test
    void hoverPausesAutoDismiss() {
        ToastStack s = stack();
        Toast t = s.push("Saved", "", null, "ok", 1f);
        settle(s);
        Element toast = s.document().query("toast").get(0);
        s.document().pointerMove(toast.box().x + 3, toast.box().y + 3);
        assertTrue(s.paused());
        for (int i = 0; i < 30; i++) s.frame(0.1f);
        assertEquals(1, s.toasts().size());
        s.document().pointerMove(1, 1);
        assertFalse(s.paused());
        for (int i = 0; i < 12; i++) s.frame(0.1f);
        assertEquals(0, s.toasts().size());
        assertEquals("", t.getIconTexture());
    }

    @Test
    void iconNamesAndTexturesAreTold() {
        Toast named = new Toast("a", "", "vibeyclient:host", "info", 1f);
        Toast texture = new Toast("a", "", "mymod:textures/gui/ok", "info", 1f);
        assertEquals("vibeyclient:host", named.getIconName());
        assertEquals("", named.getIconTexture());
        assertEquals("mymod:textures/gui/ok", texture.getIconTexture());
        assertEquals(null, texture.getIconName());
    }
}
