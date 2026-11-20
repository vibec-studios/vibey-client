package dev.henny.companio.def;

import dev.henny.companio.def.action.Action;
import dev.henny.companio.expr.Expr;

public record Step(Action action, Expr condition, float chance, float at) {}
