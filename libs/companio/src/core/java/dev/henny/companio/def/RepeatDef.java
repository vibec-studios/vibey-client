package dev.henny.companio.def;

import java.util.List;

public record RepeatDef(Range every, List<Step> actions) {}
