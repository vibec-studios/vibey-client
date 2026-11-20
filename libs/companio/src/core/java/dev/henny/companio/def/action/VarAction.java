package dev.henny.companio.def.action;

import dev.henny.companio.expr.Expr;

public record VarAction(String var, Expr value) implements Action {}
