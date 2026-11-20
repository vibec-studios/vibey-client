package dev.henny.vui.expr;

import dev.henny.vui.bind.Model;
import dev.henny.vui.bind.State;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.ErrorSink;
import dev.henny.vui.diag.SourcePos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionTest {
    public static final class Ctrl {
        public int count = 2;
        public String name = "vibe";
        public final State<Double> volume = new State<>(0.5);
        public final List<String> items = List.of("a", "b", "c");
        public final Map<String, Object> map = Map.of("k", 7);
        public boolean enabled;
        private int calls;
        public Ctrl child;

        public String getLabel() {
            return "L" + count;
        }

        public boolean isReady() {
            return true;
        }

        public int add(int a, int b) {
            return a + b;
        }

        public void bump() {
            calls++;
        }

        public int calls() {
            return calls;
        }
    }

    private static Object eval(String src, Object controller) {
        Diagnostics d = new Diagnostics();
        CompiledExpr e = ExprCompiler.expression(src, SourcePos.UNKNOWN, d);
        assertTrue(d.isEmpty(), d.format());
        List<String> errors = new ArrayList<>();
        Object v = e.evaluate(new ControllerScope(controller, new Functions()), (p, m, h) -> errors.add(m));
        assertTrue(errors.isEmpty(), String.join("\n", errors));
        return v;
    }

    @Test
    void arithmeticAndPrecedence() {
        assertEquals(7.0, eval("1 + 2 * 3", null));
        assertEquals(9.0, eval("(1 + 2) * 3", null));
        assertEquals(1.0, eval("7 % 3", null));
        assertEquals(-2.0, eval("-2", null));
        assertEquals(2.5, eval("5 / 2", null));
    }

    @Test
    void comparisonsAndLogic() {
        assertEquals(true, eval("1 < 2 && 2 <= 2 && 3 > 2 && 3 >= 3", null));
        assertEquals(true, eval("1 == 1.0 && 'a' != 'b' && !false", null));
        assertEquals("x", eval("null || 'x'", null));
        assertEquals(0.0, eval("0 && 'x'", null));
        assertEquals("yes", eval("2 > 1 ? 'yes' : 'no'", null));
        assertEquals(true, eval("'abc' === 'abc'", null));
    }

    @Test
    void stringConcatFormatsIntegers() {
        assertEquals("n=3", eval("'n=' + 3", null));
        assertEquals("3", Values.display(3.0));
    }

    @Test
    void controllerFieldsGettersAndMethods() {
        Ctrl c = new Ctrl();
        assertEquals(2, eval("count", c));
        assertEquals("L2", eval("label", c));
        assertEquals(true, eval("ready", c));
        assertEquals(0.5, eval("volume", c));
        assertEquals(5, eval("add(2, 3)", c));
        assertEquals(3.0, eval("items.length", c));
        assertEquals("b", eval("items[1]", c));
        assertEquals(7, eval("map.k", c));
        assertEquals(4.0, eval("len(name)", c));
    }

    @Test
    void optionalChainingOnNull() {
        assertNull(eval("child?.name", new Ctrl()));
    }

    @Test
    void readingFromNullReportsHelpfulError() {
        CompiledExpr e = ExprCompiler.expression("child.name", new SourcePos("f.vui", 3, 9), new Diagnostics());
        List<String> errors = new ArrayList<>();
        e.evaluate(new ControllerScope(new Ctrl(), new Functions()), (p, m, h) -> errors.add(p + " " + m + " | " + h));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("f.vui:3:9 cannot read 'name' of null"), errors.get(0));
    }

    @Test
    void assignmentWritesFieldsStatesAndModels() {
        Ctrl c = new Ctrl();
        Scope s = new ControllerScope(c, new Functions());
        ExprCompiler.handler("count = count + 1; enabled = true", SourcePos.UNKNOWN, new Diagnostics()).evaluate(s, ErrorSink.IGNORE);
        assertEquals(3, c.count);
        assertTrue(c.enabled);
        ExprCompiler.expression("volume", SourcePos.UNKNOWN, new Diagnostics()).assign(s, 0.9, ErrorSink.IGNORE);
        assertEquals(0.9, c.volume.peek());
        Model m = new Model().value("x", 1);
        ExprCompiler.handler("x = x + 1", SourcePos.UNKNOWN, new Diagnostics()).evaluate(new ControllerScope(m, null), ErrorSink.IGNORE);
        assertEquals(2.0, m.get("x"));
    }

    @Test
    void handlerMethodReferenceCallsWithOrWithoutEvent() {
        Ctrl c = new Ctrl();
        CompiledExpr h = ExprCompiler.handler("bump", SourcePos.UNKNOWN, new Diagnostics());
        h.evaluate(VarScope.of(new ControllerScope(c, null), "$event", "evt"), ErrorSink.IGNORE);
        assertEquals(1, c.calls());
    }

    @Test
    void modelActionsAndBuiltins() {
        Model m = new Model().action("twice", args -> Values.toNumber(args[0]) * 2);
        assertEquals(8.0, eval("twice(4)", m));
        assertEquals(3.0, eval("max(1, 3)", null));
        assertEquals("50%", eval("percent(0.5)", null));
        assertEquals("1.50", eval("fixed(1.5, 2)", null));
        assertEquals("ABC", eval("upper('abc')", null));
    }

    @Test
    void translatorHook() {
        Functions f = new Functions().translator((key, args) -> key.equals("hello") ? "Hallo " + args[0] : key);
        CompiledExpr e = ExprCompiler.expression("t('hello', 'Welt')", SourcePos.UNKNOWN, new Diagnostics());
        assertEquals("Hallo Welt", e.evaluate(new ControllerScope(null, f), ErrorSink.IGNORE));
    }

    @Test
    void objectAndArrayLiterals() {
        Object o = eval("{ on: 1 > 0, 'off-state': false }", null);
        assertEquals(Map.of("on", true, "off-state", false), o);
        assertEquals(List.of(1.0, "a"), eval("[1, 'a']", null));
    }

    @Test
    void syntaxErrorsHavePositionAndHint() {
        Diagnostics d = new Diagnostics();
        CompiledExpr e = ExprCompiler.expression("a + * b", new SourcePos("f.vui", 2, 10), d);
        assertFalse(e.isValid());
        assertEquals(1, d.size());
        assertEquals(14, d.all().get(0).pos().col(), d.format());
        Diagnostics d2 = new Diagnostics();
        ExprCompiler.expression("cond ? a", SourcePos.UNKNOWN, d2);
        assertTrue(d2.format().contains("hint:"), d2.format());
    }

    @Test
    void enumsCompareWithStrings() {
        assertEquals(true, Values.looseEquals(java.util.concurrent.TimeUnit.SECONDS, "seconds"));
    }
}
