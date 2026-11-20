package dev.henny.companio.def.action;

import dev.henny.companio.def.Step;
import java.util.List;

public record RandomAction(List<List<Step>> options) implements Action {}
