package dev.henny.companio.def;

import dev.henny.companio.expr.Expr;

import java.util.List;

public record PropDef(String id, List<String> bones, int[] boneIndices, boolean shown, Expr when) {}
