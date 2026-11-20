package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.TextWidget;
import dev.henny.vibeyclient.ui.hud.HudShare;
import dev.henny.vibeyclient.ui.hud.HudTemplates;

import java.util.List;

final class HudShareChecks {
    private HudShareChecks() {}

    static void run(HudTestKit t) {
        t.context.runOnClient(mc -> {
            HudElement e = HudLayout.register(HudElement.builder("test.visible").label("test").defaults(Anchor.CENTER, 0, 0)
                    .widget(SampleWidget::new).build());
            HudTemplates.apply("pvp");
            boolean hidden = HudLayout.hidden(e);
            HudLayout.unregister("test.visible");
            if (hidden) throw new AssertionError("a preset hid another module's visible widget");
        });
        t.context.runOnClient(mc -> {
            HudTemplates.apply("streamer");
            HudLayout.State before = HudLayout.capture();
            String code = HudShare.exportCode();
            HudLayout.resetAll();
            try {
                HudLayout.apply(HudShare.importCode(code), true);
            } catch (HudShare.BadLayout e) {
                throw new AssertionError("own code rejected: " + e.key);
            }
            if (!before.sameAs(HudLayout.capture())) throw new AssertionError("share code round trip changed the layout");
            for (String bad : List.of("", "hello", "VHUD1.", "VHUD1.!!!!", "VHUD9.abc", "{\"v\":1}", "{\"v\":99,\"e\":{}}",
                    "{\"e\":[1,2]}", "{".repeat(5000), "VHUD1." + "A".repeat(30000),
                    "{\"v\":1,\"e\":{\"hud.fps\":{\"a\":\"nowhere\",\"x\":1e999,\"s\":\"big\",\"st\":{\"tc\":\"zzzzzz\"}}}}")) {
                try {
                    HudLayout.apply(HudShare.importCode(bad), true);
                } catch (HudShare.BadLayout e) {
                    System.out.println(HudTestKit.LOG + "rejected " + e.key);
                } catch (RuntimeException e) {
                    throw new AssertionError("import crashed on bad input", e);
                }
            }
            HudTemplates.apply("minimal");
        });
    }

    static final class SampleWidget extends TextWidget {
        @Override
        protected void lines(HudPaint p) {
            line(null, "sample");
        }
    }
}
