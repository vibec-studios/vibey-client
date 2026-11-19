package dev.henny.vibeyclient;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which of our mixin hooks really landed. Optional features use {@code require = 0} so that another mod changing
 * the same vanilla code can never crash the game; this class finds the hooks that found nothing to hook into, so the
 * feature can say "not available" instead of silently doing nothing. Plain Java on purpose (like {@link
 * ModConflicts}): {@code VibeyMixinPlugin.postApply} calls {@link #check} while the target class is transformed,
 * before any game class of ours is loaded.
 *
 * <p>How: an injector handler (an {@code @Inject}, {@code @WrapOperation}, … method) is merged into the target class
 * under its own name or a prefixed one ({@code wrapOperation$zza000$vibeyclient$vibey$crosshair}). If nothing in the
 * target class calls it once the class is done, the injection found no target. "Done" matters: MixinExtras applies
 * {@code @WrapOperation}, {@code @ModifyExpressionValue}, {@code @WrapMethod} … late, in a transformer extension
 * that runs after {@code postApply}. So {@link #check} only remembers the class node, and the scan runs on the next
 * query ({@link #failed}, {@link #failures}, {@link #scanNow}) after the class has finished loading.
 */
public final class MixinHealth {
    private MixinHealth() {}

    private static final Logger LOG = LoggerFactory.getLogger("VibeyClient");

    private static final Set<String> INJECTORS = Set.of(
            "Lorg/spongepowered/asm/mixin/injection/Inject;",
            "Lorg/spongepowered/asm/mixin/injection/Redirect;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArg;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;",
            "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;",
            "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",
            "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",
            "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",
            "Lcom/llamalad7/mixinextras/injector/ModifyReceiver;",
            "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;",
            "Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;");

    /** "mixin class name#handler", e.g. "dev.henny.vibeyclient.module.pvp.mixin.GuiMixin#vibey$crosshair". */
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private record Pending(String targetClassName, ClassNode target, String mixinClassName, ClassNode mixin) {}

    /** Classes transformed but not scanned yet (dropped once scanned, they are big). */
    private static final java.util.Queue<Pending> PENDING = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** Called once per (target, mixin) after the mixin was applied: only remembers it, see the class comment. */
    public static void check(String targetClassName, ClassNode target, String mixinClassName, ClassNode mixin) {
        if (target == null || mixin == null || mixin.methods == null) return;
        if (!hasInjector(mixin)) return;
        PENDING.add(new Pending(targetClassName, target, mixinClassName, mixin));
    }

    /**
     * Scans every class that has finished loading since the last call and logs one warning per hook that did not
     * land. Called by the client on every screen init (so the warnings appear at the title screen) and by every query.
     * Never throws; must not be called from inside a class transformation.
     */
    public static void scanNow() {
        Pending p;
        while ((p = PENDING.poll()) != null) {
            try {
                // waits if another thread is still loading (and transforming) it; for our queries it is loaded already
                Class.forName(p.targetClassName(), false, MixinHealth.class.getClassLoader());
            } catch (Throwable t) {
                continue; // cannot tell: never claim a hook failed
            }
            scan(p.targetClassName(), p.target(), p.mixinClassName(), p.mixin());
        }
    }

    private static boolean hasInjector(ClassNode mixin) {
        for (MethodNode m : mixin.methods) if (isInjector(m)) return true;
        return false;
    }

    private static void scan(String targetClassName, ClassNode target, String mixinClassName, ClassNode mixin) {
        try {
            Set<String> called = new HashSet<>();
            for (MethodNode m : target.methods) {
                if (m.instructions == null) continue;
                for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof MethodInsnNode mi) called.add(mi.name);
                    else if (insn instanceof InvokeDynamicInsnNode indy) {
                        if (indy.bsmArgs == null) continue;
                        for (Object a : indy.bsmArgs) if (a instanceof Handle h) called.add(h.getName());
                    }
                }
            }
            for (MethodNode handler : mixin.methods) {
                if (!isInjector(handler)) continue;
                boolean merged = false, alive = false;
                for (MethodNode m : target.methods) {
                    if (!m.name.equals(handler.name) && !m.name.endsWith("$" + handler.name)) continue;
                    merged = true;
                    if (called.contains(m.name)) alive = true;
                }
                // not merged at all: nothing we can tell (some other kind of mixin), leave it alone
                if (!merged || alive) continue;
                if (FAILED.add(mixinClassName + "#" + handler.name)) {
                    LOG.warn("[VibeyClient] {}.{} could not hook into {}: another mod already changes that code. "
                                    + "This feature is turned off, the game keeps running.",
                            simple(mixinClassName), handler.name, simple(targetClassName));
                }
            }
        } catch (Throwable t) {
            LOG.debug("[VibeyClient] mixin health check of {} failed", mixinClassName, t);
        }
    }

    private static boolean isInjector(MethodNode m) {
        return hasAny(m.visibleAnnotations) || hasAny(m.invisibleAnnotations);
    }

    private static boolean hasAny(List<AnnotationNode> list) {
        if (list == null) return false;
        for (AnnotationNode a : list) if (INJECTORS.contains(a.desc)) return true;
        return false;
    }

    private static String simple(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    /** True if that handler of that mixin found nothing to hook into. */
    public static boolean failed(String mixinClassName, String handler) {
        if (!PENDING.isEmpty()) scanNow();
        return FAILED.contains(mixinClassName + "#" + handler);
    }

    /** Tests only: pretend that hook failed (or undo it), e.g. to screenshot the "not available" notice. */
    public static void simulateForTest(String mixinClassName, String handler, boolean failed) {
        if (failed) FAILED.add(mixinClassName + "#" + handler);
        else FAILED.remove(mixinClassName + "#" + handler);
    }

    /** All failed hooks ("class#handler"), for logs and tests. */
    public static Set<String> failures() {
        if (!PENDING.isEmpty()) scanNow();
        return Set.copyOf(FAILED);
    }
}
