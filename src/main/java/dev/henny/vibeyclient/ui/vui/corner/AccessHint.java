package dev.henny.vibeyclient.ui.vui.corner;

import dev.henny.vibeyclient.access.AccessView;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.access.Access;
import dev.henny.vibeyclient.client.access.AccessText;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vui.bind.State;

import java.util.List;

public final class AccessHint extends CornerController {
    public static final String DOC = "vibeyclient:title/access";

    public final State<String> text = new State<>("");
    public final State<String> tip = new State<>("");
    public final State<Boolean> busy = new State<>(false);
    public final State<Boolean> retry = new State<>(false);
    public final CornerEntry button = new CornerEntry(new MenuEntries.Entry("retry", "core.access.retry", "redo", 0,
            () -> true, s -> Access.recheck()));
    private final List<CornerEntry> entries = List.of(button);

    AccessHint() {
        update();
    }

    @Override
    public List<CornerEntry> all() {
        return entries;
    }

    @Override
    public void update() {
        AccessView v = Access.view();
        text.set(AccessText.line(v));
        tip.set(AccessText.detail(v));
        busy.set(v.checking());
        retry.set(AccessText.canRetry(v));
    }

    public String getRetryLabel() {
        return Lang.tr(busy.get() ? "core.access.busy" : "core.access.retry");
    }
}
