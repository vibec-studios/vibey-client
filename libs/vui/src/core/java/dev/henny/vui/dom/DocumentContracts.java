package dev.henny.vui.dom;

import dev.henny.vui.contract.ContractCheck;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.template.CompiledTemplate;

import java.util.Collection;

final class DocumentContracts {
    private DocumentContracts() {
    }

    static Diagnostics check(CompiledTemplate template, Collection<CompiledTemplate> components, Object controller, boolean actions) {
        Diagnostics out = new Diagnostics();
        ContractCheck.check(template.contract(), controller, actions, out);
        for (CompiledTemplate c : components) ContractCheck.check(c.contract(), controller, actions, out);
        return out;
    }
}
