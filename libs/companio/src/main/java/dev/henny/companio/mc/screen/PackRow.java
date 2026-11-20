package dev.henny.companio.mc.screen;

import java.util.List;

public record PackRow(String key, String id, String name, String desc, String meta, String icon, int errors, boolean loaded,
                      boolean isActive, List<CompanionChip> companions, List<ProblemLine> problems) {
    public record CompanionChip(String key, String name) {}

    public record ProblemLine(String key, String text, boolean error) {}
}
