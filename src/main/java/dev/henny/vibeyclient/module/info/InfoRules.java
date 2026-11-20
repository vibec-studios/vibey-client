package dev.henny.vibeyclient.module.info;

import java.util.List;

public final class InfoRules {
    public static final String LIGHT = "info.light", CHUNKS = "info.chunks", CARD = "info.card", FOOD = "info.food",
            SATURATION = "info.saturation", TOOL = "info.toolwarn", TNT = "info.tnt";
    public static final List<String> ALL = List.of(LIGHT, CHUNKS, CARD, FOOD, SATURATION, TOOL, TNT);

    private InfoRules() {}
}
