package dev.henny.companio.def;

import java.util.List;

public record PropDef(String id, List<String> bones, int[] boneIndices, boolean shown) {}
