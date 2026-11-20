package dev.henny.vibeyclient.module.chat.filters;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vibeyclient.module.chat.core.ChatFilters;
import dev.henny.vibeyclient.module.chat.hook.ChatRefresh;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class FilterController {
    public final ListState<FilterRow> rows = new ListState<>();
    public final State<String> draft = new State<>("");
    public final State<Boolean> invalid = new State<>(false);
    public final State<Boolean> collapse = new State<>(true);
    public final State<String> hidden = new State<>("");

    private final ChatModule module;

    FilterController(ChatModule module) {
        this.module = module;
        collapse.set(module.collapse.on());
        refresh();
    }

    public String t(String key) {
        return Lang.tr(key);
    }

    public void add() {
        String text = draft.peek();
        if (module.filterStore().add(text)) {
            draft.set("");
            invalid.set(false);
            refresh();
            ChatRefresh.later();
        } else {
            invalid.set(!text.isBlank());
        }
    }

    public void typed() {
        String text = draft.peek();
        invalid.set(!text.isBlank() && !ChatFilters.valid(text));
    }

    public void remove(FilterRow row) {
        if (row == null) return;
        module.filterStore().remove(row.text);
        refresh();
    }

    public void toggleCollapse() {
        module.collapse.set(!module.collapse.on());
        collapse.set(module.collapse.on());
    }

    public void close() {
        Minecraft.getInstance().screen.onClose();
    }

    void refresh() {
        List<FilterRow> next = new ArrayList<>();
        for (String p : module.filterStore().patterns()) next.add(new FilterRow(p));
        rows.replaceAll(next);
        int n = module.filterStore().hidden();
        hidden.set(n == 0 ? "" : Lang.tr("chat.filters.hidden", n));
    }
}
