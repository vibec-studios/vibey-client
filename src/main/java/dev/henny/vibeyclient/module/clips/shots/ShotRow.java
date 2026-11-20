package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.vibeyclient.module.clips.shots.core.Shot;
import dev.henny.vui.bind.State;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ShotRow {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    public final String id;
    public final String date;
    public final State<Boolean> fav = new State<>(false);
    public final State<String> thumb = new State<>("");
    public final State<Boolean> current = new State<>(false);
    final Shot shot;

    ShotRow(Shot shot) {
        this.shot = shot;
        this.id = shot.name();
        this.date = DATE.format(Instant.ofEpochMilli(shot.modifiedMs()));
    }

    public Shot shot() {
        return shot;
    }
}
