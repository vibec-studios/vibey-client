package dev.henny.companio.def.action;

import java.util.Map;

public record NeedAction(Map<String, Float> deltas) implements Action {}
