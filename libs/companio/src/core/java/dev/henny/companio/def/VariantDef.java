package dev.henny.companio.def;

import dev.henny.companio.text.TextRef;
import java.util.Map;

public record VariantDef(String id, TextRef name, Map<String, String> textures) {}
