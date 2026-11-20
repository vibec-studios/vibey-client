package dev.henny.vibeyclient.ui.vui.toast;

import dev.henny.vui.Vui;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.headless.Headless;
import dev.henny.vui.host.Toast;
import dev.henny.vui.host.ToastStack;
import dev.henny.vui.icon.IconSet;
import dev.henny.vui.icon.IconSetParser;
import dev.henny.vui.template.CompiledTemplate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToastStackTemplateTest {
    private static String resource(String path) throws IOException {
        try (InputStream in = ToastStackTemplateTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void rendersPlayerHeadTextureAndPixelIconWithoutVuiErrors() throws IOException {
        Vui vui = Headless.create().vui();
        Diagnostics iconDiag = new Diagnostics();
        IconSet icons = IconSetParser.parse("vibeyclient", "icons.json", resource("/assets/vibeyclient/vui/icons.json"), iconDiag);
        assertTrue(iconDiag.isEmpty(), iconDiag.format());
        vui.icons().put(icons);

        CompiledTemplate tpl = vui.compile("vibeyclient:toast/stack", "vibeyclient/vui/toast/stack.vui", resource("/assets/vibeyclient/vui/toast/stack.vui"));
        ToastStack stack = new ToastStack(vui, tpl);
        stack.resize(300, 200);

        stack.push(new Toast("Alex", "Joined your world", "vibeyclient:head/head_30e510d2", "invite", 5f));
        stack.push(new Toast("Saved", "Settings updated", "vibeyclient:check", "ok", 5f));
        for (int i = 0; i < 4; i++) stack.frame(0.05f);

        assertTrue(stack.document().diagnostics().isEmpty(), stack.document().diagnostics().format());
        assertEquals(1, stack.document().query("image.toast-head").size());
        assertEquals(1, stack.document().query(".toast-icon icon").size());
    }
}
