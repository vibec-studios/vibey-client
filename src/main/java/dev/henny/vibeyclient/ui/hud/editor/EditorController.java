package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.ui.hud.HudShare;
import dev.henny.vibeyclient.ui.hud.HudTemplates;
import dev.henny.vibeyclient.ui.hud.edit.EditActions;
import dev.henny.vibeyclient.ui.hud.edit.EditSession;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.Model;
import dev.henny.vui.bind.State;

import java.util.List;

public final class EditorController {
    public final State<Boolean> firstRun = new State<>(false);
    public final State<Boolean> busy = new State<>(false);
    public final State<Integer> widgetCount = new State<>(0);
    public final State<String> panel = new State<>("");
    public final State<Boolean> snap = new State<>(true);
    public final State<Boolean> canUndo = new State<>(false);
    public final State<Boolean> canRedo = new State<>(false);
    public final State<Boolean> dockTop = new State<>(false);
    public final State<Items.Bar> selected = new State<>(null);
    public final ListState<Items.Library> widgets = new ListState<>();
    public final ListState<Items.Preset> presets = new ListState<>();
    public final ListState<Items.Badge> badges = new ListState<>();
    public final State<Items.Menu> menu = new State<>(new Items.Menu(0, 0, List.of()));
    public final State<String> importCode = new State<>("");
    public final State<String> importError = new State<>("");
    public final ListState<Items.FileEntry> files = new ListState<>();
    public final Model st = new Model();
    public final ListState<Items.Option> opts = new ListState<>();
    public final ListState<Items.AnchorCell> anchors = new ListState<>();
    public final int maxCode = HudShare.MAX_CODE;

    final EditSession session;
    final EditActions actions;
    final EditorHost host;
    final StylePanel style;
    final SharePanel share;

    EditorController(EditSession session, EditorHost host) {
        this.session = session;
        this.actions = session.actions();
        this.host = host;
        this.style = new StylePanel(this);
        this.share = new SharePanel(this);
    }

    public void undo() {
        actions.undo();
    }

    public void redo() {
        actions.redo();
    }

    public void cancel() {
        host.close(false);
    }

    public void done() {
        host.close(true);
    }

    public void confirmReset() {
        panel.set("");
        actions.resetAll();
        String def = HudTemplates.defaultId();
        if (def != null) HudTemplates.apply(def);
        host.refresh();
    }

    public void styleSelected() {
        panel.set("style".equals(panel.peek()) ? "" : "style");
    }

    public void hideSelected() {
        actions.hideSelection();
    }

    public void resetSelected() {
        actions.resetSelection();
    }

    public void placed(Items.Library w, boolean on) {
        actions.show(w.element(), on);
    }

    public void addCopy(Items.Library w) {
        actions.addCopy(w.element());
    }

    public void applyPreset(Items.Preset p) {
        HudTemplates.Template t = HudTemplates.get(p.id());
        if (t == null) return;
        panel.set("");
        actions.apply(t.state().get(), "ui.hud.applied");
    }

    public void menuPick(Items.MenuEntry m) {
        panel.set("");
        ContextMenu.pick(this, m.id());
    }

    public void copyCode() {
        share.copyCode();
    }

    public void openImport() {
        share.openImport();
    }

    public void pasteCode() {
        share.paste();
    }

    public void importNow() {
        share.importNow();
    }

    public void importFile(Items.FileEntry f) {
        share.importFile(f.path());
    }

    public void saveFile() {
        share.saveFile();
    }

    public void openFolder() {
        share.openFolder();
    }

    public void styleScope(String scope) {
        style.scope(scope);
    }

    public void styleTab(String tab) {
        st.value("tab", tab);
    }

    public void set(String key, Object value) {
        style.set(key, value);
    }

    public void inherit(String key) {
        style.inherit(key);
    }

    public void setScale(float scale) {
        actions.scaleTo(scale);
    }

    public void setAnchor(String id) {
        style.anchor(id);
    }

    public void optionSet(Items.Option o, boolean on) {
        style.option(o, on ? "1" : "0");
    }

    public void optionStep(Items.Option o, int dir) {
        style.step(o, dir);
    }

    public void optionCapture(Items.Option o) {
        style.capture(o);
    }
}
