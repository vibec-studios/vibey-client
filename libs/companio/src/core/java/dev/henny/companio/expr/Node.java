package dev.henny.companio.expr;

sealed interface Node permits Node.Lit, Node.Var, Node.Not, Node.Neg, Node.Bin, Node.Cond, Node.Call {
    Object eval(Scope s);

    record Lit(Object value) implements Node {
        public Object eval(Scope s) {
            return value;
        }
    }

    record Var(String name) implements Node {
        public Object eval(Scope s) {
            Object v = s.variable(name);
            return v == null ? Boolean.FALSE : v;
        }
    }

    record Not(Node a) implements Node {
        public Object eval(Scope s) {
            return !Values.truthy(a.eval(s));
        }
    }

    record Neg(Node a) implements Node {
        public Object eval(Scope s) {
            return -Values.number(a.eval(s));
        }
    }

    record Cond(Node c, Node a, Node b) implements Node {
        public Object eval(Scope s) {
            return Values.truthy(c.eval(s)) ? a.eval(s) : b.eval(s);
        }
    }

    record Call(String name, Node[] args) implements Node {
        public Object eval(Scope s) {
            Object[] v = new Object[args.length];
            for (int i = 0; i < args.length; i++) v[i] = args[i].eval(s);
            Object pure = Functions.pure(name, v);
            if (pure != null) return pure;
            Object r = s.call(name, v);
            return r == null ? Boolean.FALSE : r;
        }
    }

    record Bin(String op, Node a, Node b) implements Node {
        public Object eval(Scope s) {
            if (op.equals("&&")) return Values.truthy(a.eval(s)) && Values.truthy(b.eval(s));
            if (op.equals("||")) return Values.truthy(a.eval(s)) || Values.truthy(b.eval(s));
            Object x = a.eval(s), y = b.eval(s);
            return switch (op) {
                case "==" -> Values.same(x, y);
                case "!=" -> !Values.same(x, y);
                case "+" -> plus(x, y);
                case "-" -> Values.number(x) - Values.number(y);
                case "*" -> Values.number(x) * Values.number(y);
                case "/" -> Values.number(y) == 0 ? 0.0 : Values.number(x) / Values.number(y);
                case "%" -> Values.number(y) == 0 ? 0.0 : Values.number(x) % Values.number(y);
                case "<" -> Values.number(x) < Values.number(y);
                case "<=" -> Values.number(x) <= Values.number(y);
                case ">" -> Values.number(x) > Values.number(y);
                case ">=" -> Values.number(x) >= Values.number(y);
                default -> Boolean.FALSE;
            };
        }

        private static Object plus(Object x, Object y) {
            if (x instanceof String || y instanceof String) return Values.text(x) + Values.text(y);
            return Values.number(x) + Values.number(y);
        }
    }
}
