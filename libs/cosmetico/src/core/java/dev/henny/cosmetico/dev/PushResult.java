package dev.henny.cosmetico.dev;

import java.util.List;

public record PushResult(String id, boolean ok, List<String> warnings, String error) {}
