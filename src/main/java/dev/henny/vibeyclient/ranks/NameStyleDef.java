package dev.henny.vibeyclient.ranks;

import dev.henny.vibeyclient.ranks.style.NameStyle;

import java.util.List;

public record NameStyleDef(String id, Label label, List<String> ranks, int sort, String effect, NameStyle style) {}
