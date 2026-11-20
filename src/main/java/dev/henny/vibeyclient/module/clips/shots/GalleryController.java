package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.clips.shots.core.ArmedConfirm;
import dev.henny.vibeyclient.module.clips.shots.core.Shot;
import dev.henny.vibeyclient.module.clips.shots.core.ShotList;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GalleryController {
    static final int STRIP = 5;
    static final long DWELL_MS = 450, RESCAN_MS = 3000;

    public final ListState<ShotRow> rows = new ListState<>();
    public final ListState<ShotRow> strip = new ListState<>();
    public final State<ShotRow> sel = new State<>(null);
    public final State<String> preview = new State<>("");
    public final State<String> sort = new State<>("newest");
    public final State<Boolean> favOnly = new State<>(false);
    public final State<Boolean> armed = new State<>(false);
    public final State<Boolean> loading = new State<>(true);
    public final State<String> position = new State<>("");

    private final ShotLibrary library = new ShotLibrary();
    private final ShotImages images = new ShotImages();
    private final ArmedConfirm confirm = new ArmedConfirm();
    private final Map<String, ShotRow> byName = new HashMap<>();
    private ShotRow looking;
    private long lookingSince;
    private long nextScan;

    GalleryController() {
        library.listen(this::rebuild);
        library.rescan();
    }

    public String t(String key) {
        return Lang.tr(key);
    }

    public void select(ShotRow r) {
        if (r == null) return;
        ShotRow old = sel.peek();
        if (old != null) old.current.set(false);
        r.current.set(true);
        sel.set(r);
        confirm.clear();
        armed.set(false);
        preview.set("");
        images.request(r.shot, ShotImages.Kind.PREVIEW, id -> {
            if (sel.peek() == r) preview.set(id);
        });
        updateStrip();
    }

    public void next() {
        step(1);
    }

    public void prev() {
        step(-1);
    }

    public void fav() {
        ShotRow r = sel.peek();
        if (r != null) r.fav.set(library.toggleFavourite(r.id));
    }

    public void copy() {
        ShotRow r = sel.peek();
        if (r != null) GalleryActions.copy(r.shot);
    }

    public void remove() {
        ShotRow r = sel.peek();
        if (r == null) return;
        if (!confirm.press(r.id, System.currentTimeMillis())) {
            armed.set(true);
            return;
        }
        armed.set(false);
        library.delete(r.shot);
        GalleryActions.deleted();
    }

    public void cycleSort() {
        library.sort(library.sort().next());
    }

    public void toggleFavs() {
        library.favouritesOnly(!library.favouritesOnly());
    }

    public void folder() {
        ShotFiles.openFolder();
    }

    public void place() {
        Shots.placePanel();
    }

    public void close() {
        Shots.closeScreen();
    }

    public void unplace() {
        Shots.removePanel();
    }

    public void look(ShotRow r) {
        looking = r;
        lookingSince = System.currentTimeMillis();
    }

    public void unlook(ShotRow r) {
        if (looking == r) looking = null;
    }

    void poll() {
        long now = System.currentTimeMillis();
        if (now >= nextScan) {
            nextScan = now + RESCAN_MS;
            library.rescan();
        }
        ShotRow r = looking;
        if (r != null && r != sel.peek() && now - lookingSince >= DWELL_MS) select(r);
        if (armed.peek() && !confirm.armed(idOf(sel.peek()), now)) armed.set(false);
    }

    void dispose() {
        images.close();
        rows.clear();
        strip.clear();
        byName.clear();
        sel.set(null);
        preview.set("");
    }

    public List<Shot> shown() {
        List<Shot> out = new ArrayList<>();
        for (ShotRow r : rows.snapshot()) out.add(r.shot);
        return out;
    }

    public int loadedTextures() {
        return images.loaded();
    }

    private void rebuild() {
        List<Shot> view = library.view();
        int oldIndex = Math.max(0, ShotList.indexOf(shown(), idOf(sel.peek())));
        Map<String, ShotRow> keep = new HashMap<>(byName);
        byName.clear();
        List<ShotRow> next = new ArrayList<>(view.size());
        for (Shot s : view) {
            ShotRow r = keep.get(s.name());
            if (r == null || !r.shot.equals(s)) r = new ShotRow(s);
            r.fav.set(library.favourite(s.name()));
            byName.put(s.name(), r);
            next.add(r);
            if (r.thumb.peek().isEmpty()) images.request(s, ShotImages.Kind.THUMB, r.thumb::set);
        }
        rows.replaceAll(next);
        sort.set(library.sort().name().toLowerCase(Locale.ROOT));
        favOnly.set(library.favouritesOnly());
        loading.set(!library.loaded());
        Shot keepSel = ShotList.keepOrNear(view, idOf(sel.peek()), oldIndex);
        ShotRow target = keepSel == null ? null : byName.get(keepSel.name());
        if (target == null) clearSelection();
        else if (target != sel.peek()) select(target);
        else updateStrip();
    }

    private void step(int delta) {
        Shot s = ShotList.step(shown(), idOf(sel.peek()), delta);
        if (s != null) select(byName.get(s.name()));
    }

    private void updateStrip() {
        List<ShotRow> out = new ArrayList<>(STRIP);
        List<Shot> all = shown();
        for (Shot s : ShotList.window(all, idOf(sel.peek()), STRIP)) out.add(byName.get(s.name()));
        strip.replaceAll(out);
        int i = ShotList.indexOf(all, idOf(sel.peek()));
        position.set(i < 0 ? "" : (i + 1) + " / " + all.size());
    }

    private void clearSelection() {
        ShotRow old = sel.peek();
        if (old != null) old.current.set(false);
        sel.set(null);
        preview.set("");
        strip.clear();
        position.set("");
    }

    private static String idOf(ShotRow r) {
        return r == null ? null : r.id;
    }
}
