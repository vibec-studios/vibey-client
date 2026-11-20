package dev.henny.companio.expr;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExprTest {
    private static final Names NAMES = new Names() {
        public boolean known(String v) {
            return List.of("weather.rain", "need.energy", "player.held", "time.hour").contains(v);
        }

        public java.util.Collection<String> all() {
            return List.of("weather.rain", "need.energy", "player.held", "time.hour");
        }
    };

    private static Scope scope(Map<String, Object> vars) {
        return new Scope() {
            public Object variable(String name) {
                return vars.get(name);
            }

            public Object call(String function, Object[] args) {
                return function.equals("cooldown") ? Boolean.TRUE : Boolean.FALSE;
            }
        };
    }

    private static Object eval(String src, Map<String, Object> vars) {
        return Expr.compile(src, NAMES).eval(scope(vars));
    }

    @Test
    void precedenceAndLogic() {
        assertEquals(7.0, eval("1 + 2 * 3", Map.of()));
        assertEquals(9.0, eval("(1 + 2) * 3", Map.of()));
        assertEquals(true, eval("weather.rain && need.energy < 0.5", Map.of("weather.rain", true, "need.energy", 0.2)));
        assertEquals(false, eval("weather.rain and not (need.energy < 0.5)", Map.of("weather.rain", true, "need.energy", 0.2)));
        assertEquals(true, eval("!weather.rain || need.energy >= 1", Map.of("weather.rain", false, "need.energy", 0.0)));
        assertEquals("b", eval("need.energy > 0.5 ? 'a' : 'b'", Map.of("need.energy", 0.1)));
        assertEquals(-2.0, eval("-(1 + 1)", Map.of()));
        assertEquals(1.0, eval("7 % 3", Map.of()));
        assertEquals(0.0, eval("1 / 0", Map.of()));
    }

    @Test
    void stringsAndFunctions() {
        assertEquals(true, eval("player.held == \"minecraft:cod\"", Map.of("player.held", "minecraft:cod")));
        assertEquals("x1", eval("'x' + 1", Map.of()));
        assertEquals(2.0, eval("max(1, min(2, 3))", Map.of()));
        assertEquals(0.5, eval("clamp(3, 0, 0.5)", Map.of()));
        assertEquals(true, eval("is(time.hour, 1, 2, 3)", Map.of("time.hour", 2.0)));
        assertEquals(true, eval("cooldown('bark')", Map.of()));
    }

    @Test
    void unknownNamesSuggestFixes() {
        ExprException e = assertThrows(ExprException.class, () -> Expr.compile("wether.rain", NAMES));
        assertTrue(e.getMessage().contains("did you mean \"weather.rain\""), e.getMessage());
        ExprException f = assertThrows(ExprException.class, () -> Expr.compile("chanse(0.5)", NAMES));
        assertTrue(f.getMessage().contains("chance"), f.getMessage());
    }

    @Test
    void syntaxErrorsAreReported() {
        assertThrows(ExprException.class, () -> Expr.compile("1 +", NAMES));
        assertThrows(ExprException.class, () -> Expr.compile("(1 + 2", NAMES));
        assertThrows(ExprException.class, () -> Expr.compile("'open", NAMES));
        assertThrows(ExprException.class, () -> Expr.compile("chance()", NAMES));
        assertThrows(ExprException.class, () -> Expr.compile("1 ? 2", NAMES));
        assertThrows(ExprException.class, () -> Expr.compile("a $ b", NAMES));
        assertThrows(ExprException.class, () -> Expr.compile("", NAMES));
        assertThrows(ExprException.class, () -> Expr.compile("1".repeat(401), NAMES));
        String deep = "(".repeat(30) + "1" + ")".repeat(30);
        assertTrue(assertThrows(ExprException.class, () -> Expr.compile(deep, NAMES)).getMessage().contains("deep"));
    }
}
