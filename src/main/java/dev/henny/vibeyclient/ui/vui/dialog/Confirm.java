package dev.henny.vibeyclient.ui.vui.dialog;

import dev.henny.vibeyclient.ui.vui.VibeyVui;
import dev.henny.vui.dom.Document;
import dev.henny.vui.mc.VuiMc;

public final class Confirm {
    public static final String DOC = "vibeyclient:dialog/confirm";

    private final ConfirmState confirm;
    private final Runnable onYes;
    private Document doc;

    private Confirm(ConfirmState confirm, Runnable onYes) {
        this.confirm = confirm;
        this.onYes = onYes;
    }

    public static Confirm ask(String icon, String title, String text, String yes, boolean danger, Runnable onYes) {
        VibeyVui.init();
        Confirm c = new Confirm(new ConfirmState(icon, title, text, yes, danger), onYes);
        c.doc = VuiMc.overlays().show(DOC, c);
        c.doc.onEscape(c::cancel);
        return c;
    }

    public ConfirmState getConfirm() {
        return confirm;
    }

    public void accept() {
        close();
        onYes.run();
    }

    public void cancel() {
        close();
    }

    public boolean isOpen() {
        return doc != null;
    }

    private void close() {
        if (doc == null) return;
        VuiMc.overlays().close(doc);
        doc = null;
    }
}
