package dev.henny.vibeafk.game;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Opener {
    private final Instrumentation inst;
    private final Set<String> opened = new HashSet<String>();
    private Method getModule;
    private Method isNamed;
    private Method redefine;
    private Object self;
    private boolean ready;

    public Opener(Instrumentation inst) {
        this.inst = inst;
        try {
            getModule = Class.class.getMethod("getModule");
            Class<?> moduleCls = Class.forName("java.lang.Module");
            isNamed = moduleCls.getMethod("isNamed");
            redefine = Instrumentation.class.getMethod("redefineModule", moduleCls, Set.class, Map.class, Map.class, Set.class, Map.class);
            self = getModule.invoke(Opener.class);
            ready = inst != null;
        } catch (Throwable ignored) {
            ready = false;
        }
    }

    public synchronized void open(Class<?> c) {
        if (!ready || c == null || c.isPrimitive() || c.isArray()) return;
        try {
            Object module = getModule.invoke(c);
            if (!(Boolean) isNamed.invoke(module)) return;
            String name = c.getName();
            int dot = name.lastIndexOf('.');
            if (dot < 0) return;
            String pkg = name.substring(0, dot);
            if (!opened.add(module.toString() + "/" + pkg)) return;
            Map<String, Set<Object>> opens = Collections.singletonMap(pkg, Collections.singleton(self));
            redefine.invoke(inst, module, Collections.singleton(self), opens, opens, Collections.emptySet(), Collections.emptyMap());
        } catch (Throwable ignored) {
        }
    }
}
