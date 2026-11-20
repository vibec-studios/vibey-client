package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudShare;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class SharePanel {
    private final EditorController c;

    SharePanel(EditorController c) {
        this.c = c;
    }

    void copyCode() {
        c.panel.set("");
        try {
            String code = HudShare.exportCode();
            ClientCompat.setClipboard(code);
            c.host.toast(Lang.tr("ui.hud.copied"));
        } catch (RuntimeException e) {
            c.host.toast(Lang.tr("core.hud.share.error.write"));
        }
    }

    void openImport() {
        String clip = ClientCompat.clipboard().strip();
        c.importCode.set(clip.regionMatches(true, 0, "VHUD", 0, 4) && clip.length() < HudShare.MAX_CODE ? clip : "");
        c.importError.set("");
        List<Items.FileEntry> list = new ArrayList<>();
        for (Path f : HudShare.files()) if (list.size() < 5) list.add(new Items.FileEntry(f.getFileName().toString(), f));
        c.files.replaceAll(list);
        c.panel.set("import");
    }

    void paste() {
        c.importCode.set(ClientCompat.clipboard().strip());
        c.importError.set("");
    }

    void importNow() {
        try {
            apply(HudShare.importCode(c.importCode.peek()), "ui.hud.imported");
        } catch (HudShare.BadLayout e) {
            c.importError.set(Lang.tr(e.key));
        }
    }

    void importFile(Path f) {
        try {
            apply(HudShare.importFile(f), "ui.hud.imported");
        } catch (HudShare.BadLayout e) {
            if ("import".equals(c.panel.peek())) c.importError.set(Lang.tr(e.key));
            else c.host.toast(Lang.tr(e.key));
        }
    }

    private void apply(HudLayout.State s, String toastKey) {
        c.panel.set("");
        c.actions.apply(s, toastKey);
    }

    void saveFile() {
        c.panel.set("");
        try {
            Path f = HudShare.exportFile();
            c.host.toast(Lang.tr("ui.hud.savedFile", f.getFileName().toString()));
        } catch (Exception e) {
            c.host.toast(Lang.tr("core.hud.share.error.write"));
        }
    }

    void openFolder() {
        try {
            Files.createDirectories(HudShare.folder());
            ClientCompat.openFolder(HudShare.folder());
        } catch (Exception e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Could not open {}", HudShare.folder(), e);
        }
    }
}
