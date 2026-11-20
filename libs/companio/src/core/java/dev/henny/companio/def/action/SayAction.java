package dev.henny.companio.def.action;

import dev.henny.companio.text.TextRef;

public record SayAction(TextRef text, float seconds) implements Action {}
