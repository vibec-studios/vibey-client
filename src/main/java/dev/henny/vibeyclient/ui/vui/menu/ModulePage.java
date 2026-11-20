package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.ui.ServerBadge;
import dev.henny.vibeyclient.ui.vui.settings.Labels;
import dev.henny.vibeyclient.ui.vui.settings.PageSections;
import dev.henny.vibeyclient.ui.vui.settings.Rows;
import dev.henny.vibeyclient.ui.vui.settings.Section;
import dev.henny.vibeyclient.ui.vui.settings.SettingRow;

import java.util.ArrayList;
import java.util.List;

public final class ModulePage {
    private final VibeyModule module;
    private final ModuleSwitch on;
    private final SettingRow primary;
    private final List<Section> sections;
    private final List<Section> more;
    private final int moreCount;
    private final List<SettingRow> rows;
    private final String visibility;

    public ModulePage(VibeyModule module) {
        this.module = module;
        this.on = new ModuleSwitch(module.id());
        PageSections.Result r = PageSections.of(module.settings(), module.displayName());
        this.primary = r.primary() == null ? null : primaryRow(r);
        this.sections = r.main();
        this.more = r.more();
        this.moreCount = r.moreCount();
        this.rows = r.allRows();
        if (primary != null) rows.add(primary);
        this.visibility = PageSections.visibility(module.settings());
    }

    private static SettingRow primaryRow(PageSections.Result r) {
        SettingRow row = Rows.of(r.primary());
        return row == null ? null : row.withButton(Labels.action(r.primary()));
    }

    public String getId() { return module.id(); }
    public String getName() { return module.displayName(); }
    public String getIcon() { return ModuleLook.icon(module); }
    public String getAccent() { return ModuleLook.accent(module); }
    public String getTip() { return Lang.tr(module.descriptionKey()); }
    public ModuleSwitch getOn() { return on; }
    public boolean isAvailable() { return ModuleLook.available(module); }
    public SettingRow getPrimary() { return primary; }
    public List<Section> getSections() { return sections; }
    public List<Section> getMore() { return more; }
    public int getMoreCount() { return moreCount; }

    public List<Notice> getNotices() {
        List<Notice> out = new ArrayList<>();
        String reason = ModuleLook.reason(module);
        if (!reason.isEmpty()) out.add(new Notice(reason, module.statusReason() == null ? reason : module.statusReason()));
        if (ModuleLook.blocked(module)) out.add(new Notice(ServerBadge.label(), ServerBadge.hint()));
        for (String n : module.notices()) out.add(new Notice(n, n));
        return out;
    }

    public VibeyModule module() {
        return module;
    }

    public boolean stale() {
        return !visibility.equals(PageSections.visibility(module.settings()));
    }

    public void sync() {
        on.sync();
        for (SettingRow r : rows) r.sync();
    }
}
