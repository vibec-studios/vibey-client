package dev.henny.vui.contract;

import dev.henny.vui.access.Members;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.expr.EvalException;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.Values;

import java.util.Map;

public final class ContractCheck {
    private ContractCheck() {
    }

    public static Diagnostics check(Contract contract, Object controller, boolean actions, Diagnostics out) {
        for (Contract.Entry f : contract.fields()) field(f, controller, out);
        if (actions) for (Contract.Entry a : contract.actions()) action(a, controller, out);
        return out;
    }

    private static void field(Contract.Entry f, Object controller, Diagnostics out) {
        Object value;
        try {
            value = controller == null ? Scope.MISSING : Members.get(controller, f.name());
        } catch (EvalException e) {
            value = Scope.MISSING;
        }
        if (value == Scope.MISSING) {
            if (!f.optional()) out.warn(f.pos(), "controller has no field '" + f.name() + "' (contract)", hint(controller));
            return;
        }
        if (value != null && !matches(f.type(), Values.unwrap(value))) {
            out.warn(f.pos(), "field '" + f.name() + "' should be " + f.type() + " but is " + Values.typeName(value), null);
        }
    }

    private static void action(Contract.Entry a, Object controller, Diagnostics out) {
        if (a.optional() || Members.callable(controller, a.name())) return;
        out.warn(a.pos(), "controller has no action '" + a.name() + "' (contract)", "add a method " + a.name() + "(…) or model.action(\"" + a.name() + "\", …)");
    }

    static boolean matches(String type, Object v) {
        return switch (type) {
            case "string" -> v instanceof CharSequence;
            case "number" -> v instanceof Number;
            case "bool" -> v instanceof Boolean;
            case "list" -> v instanceof Iterable<?> || v instanceof Object[];
            case "object" -> !(v instanceof CharSequence || v instanceof Number || v instanceof Boolean || v instanceof Iterable<?>) || v instanceof Map<?, ?>;
            default -> true;
        };
    }

    private static String hint(Object controller) {
        return controller == null ? "the document has no controller" : "add it to " + controller.getClass().getSimpleName() + " or the state file";
    }
}
