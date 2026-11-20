package dev.henny.vui.dom.build;

import dev.henny.vui.bind.Effect;
import dev.henny.vui.dom.ChildSource;
import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.expr.VarScope;
import dev.henny.vui.template.CBinding;
import dev.henny.vui.template.CElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SlotOutlet {
    private SlotOutlet() {
    }

    public static void build(TreeBuilder builder, Element parent, CElement outlet, Scope scope, BuildContext ctx,
                             List<ChildSource> into) {
        SlotContent content = ctx.slot(outlet.slotOutletName());
        if (content == null || content.nodes().isEmpty()) {
            builder.buildChildren(parent, outlet.children(), scope, ctx, into);
            return;
        }
        Scope slotScope = slotScope(builder, parent, outlet, scope, content);
        builder.buildChildren(parent, content.nodes(), slotScope, content.context(), into);
    }

    public static Scope slotScope(TreeBuilder builder, Element owner, CElement outlet, Scope componentScope, SlotContent content) {
        List<CBinding> props = outlet.bindings();
        if (props.isEmpty() || content.spec() == null) return content.scope();
        String[] names = content.spec().variable() != null
                ? new String[]{content.spec().variable()} : content.spec().destructure();
        if (names.length == 0) return content.scope();
        VarScope vars = new VarScope(content.scope(), names, new Object[names.length]);
        Effect effect = builder.host().effect(() -> {
            Map<String, Object> values = new LinkedHashMap<>();
            for (CBinding b : props) values.put(b.name(), b.expr().evaluate(componentScope, builder.host().errors()));
            if (content.spec().variable() != null) vars.set(0, values);
            else for (int i = 0; i < names.length; i++) vars.set(i, values.get(names[i]));
        });
        owner.effects().add(effect);
        effect.run();
        return vars;
    }
}
