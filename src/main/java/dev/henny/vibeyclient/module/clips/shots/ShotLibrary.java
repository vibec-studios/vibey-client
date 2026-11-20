package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.module.clips.shots.core.Favourites;
import dev.henny.vibeyclient.module.clips.shots.core.Shot;
import dev.henny.vibeyclient.module.clips.shots.core.ShotList;
import dev.henny.vibeyclient.module.clips.shots.core.ShotSort;

import java.util.List;

final class ShotLibrary {
    private List<Shot> all = List.of();
    private Favourites favourites = new Favourites();
    private ShotSort sort = ShotSort.NEWEST;
    private boolean favouritesOnly;
    private boolean loaded;
    private boolean scanning;
    private Runnable listener = () -> {};

    void listen(Runnable r) {
        listener = r;
    }

    void rescan() {
        if (scanning) return;
        scanning = true;
        Tasks.run(() -> {
            List<Shot> found = ShotFiles.scan();
            Favourites favs = loaded ? null : ShotFiles.loadFavourites();
            Tasks.onMain(() -> apply(found, favs));
        });
    }

    List<Shot> view() {
        return ShotList.view(all, favourites.names(), sort, favouritesOnly);
    }

    boolean loaded() {
        return loaded;
    }

    boolean favourite(String name) {
        return favourites.contains(name);
    }

    boolean toggleFavourite(String name) {
        boolean on = favourites.toggle(name);
        String json = favourites.toJson();
        Tasks.run(() -> ShotFiles.saveFavourites(json));
        listener.run();
        return on;
    }

    void delete(Shot s) {
        favourites.forget(s.name());
        String json = favourites.toJson();
        Tasks.run(() -> {
            ShotFiles.delete(s);
            ShotFiles.saveFavourites(json);
            Tasks.onMain(this::rescan);
        });
    }

    ShotSort sort() {
        return sort;
    }

    void sort(ShotSort s) {
        sort = s;
        listener.run();
    }

    boolean favouritesOnly() {
        return favouritesOnly;
    }

    void favouritesOnly(boolean on) {
        favouritesOnly = on;
        listener.run();
    }

    private void apply(List<Shot> found, Favourites favs) {
        scanning = false;
        boolean changed = !loaded || !found.equals(all);
        all = List.copyOf(found);
        if (favs != null) favourites = favs;
        loaded = true;
        if (changed) listener.run();
    }
}
