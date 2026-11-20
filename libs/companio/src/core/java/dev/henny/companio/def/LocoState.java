package dev.henny.companio.def;

import java.util.Locale;

public enum LocoState {
    IDLE(null), WALK(IDLE), RUN(WALK), SWIM(WALK), SWIM_IDLE(SWIM), FALL(IDLE), FLY(FALL), SIT(IDLE), SLEEP(SIT), HELD(FALL), RIDE(SIT);

    public final LocoState fallback;

    LocoState(LocoState fallback) {
        this.fallback = fallback;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LocoState of(String id) {
        for (LocoState s : values()) if (s.id().equals(id)) return s;
        return null;
    }
}
