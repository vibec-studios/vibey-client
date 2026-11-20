package dev.henny.companio.def;

import dev.henny.companio.expr.Expr;
import java.util.List;
import java.util.Map;

public record BehaviorDef(String id, int priority, Expr when, Expr whileExpr, String on, Range duration, float minDuration,
                          float cooldown, float weight, boolean interrupt, boolean obeys, MoveMode move, LookMode look,
                          AnimRef anim, String baseAnim, Map<String, Boolean> props, Map<String, Float> needs,
                          List<Step> start, List<RepeatDef> repeat, List<Step> end) {
    public boolean triggered() {
        return on != null;
    }
}
