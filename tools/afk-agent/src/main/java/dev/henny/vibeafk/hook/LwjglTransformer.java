package dev.henny.vibeafk.hook;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LwjglTransformer implements ClassFileTransformer {
    private final Set<String> transformed = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final Listener listener;

    public interface Listener {
        void transformed(String owner, List<String> methods, ClassLoader loader);

        void failed(String owner, Throwable t);
    }

    public LwjglTransformer(Listener listener) {
        this.listener = listener;
    }

    public Set<String> transformedOwners() {
        return transformed;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> redefined, ProtectionDomain pd, byte[] bytes) {
        if (className == null || redefined != null || !Target.owners().contains(className)) return null;
        try {
            List<String> done = new ArrayList<String>();
            byte[] out = transform(className, bytes, done);
            if (out == null) return null;
            transformed.add(className);
            if (listener != null) listener.transformed(className, done, loader);
            return out;
        } catch (Throwable t) {
            if (listener != null) listener.failed(className, t);
            return null;
        }
    }

    public static byte[] transform(final String owner, byte[] bytes, final List<String> done) {
        final List<Target> targets = Target.forOwner(owner);
        if (targets.isEmpty()) return null;
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        final List<String[]> wrappers = new ArrayList<String[]>();
        final List<Target> wrapperTargets = new ArrayList<Target>();
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            private boolean itf;

            @Override
            public void visit(int version, int access, String name, String sig, String sup, String[] itfs) {
                itf = (access & Opcodes.ACC_INTERFACE) != 0;
                super.visit(version, access, name, sig, sup, itfs);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exc) {
                if ((access & Opcodes.ACC_STATIC) != 0 && (access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) == 0 && !itf) {
                    for (Target t : targets) {
                        if (!t.matches(name, desc)) continue;
                        String renamed = Hooks.PREFIX + name;
                        wrappers.add(new String[]{name, desc, sig, Integer.toString(access)});
                        wrapperTargets.add(t);
                        int hidden = (access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
                        return super.visitMethod(hidden, renamed + "$" + wrappers.size(), desc, sig, exc);
                    }
                }
                return super.visitMethod(access, name, desc, sig, exc);
            }

            @Override
            public void visitEnd() {
                for (int k = 0; k < wrappers.size(); k++) {
                    String[] w = wrappers.get(k);
                    String renamed = Hooks.PREFIX + w[0] + "$" + (k + 1);
                    int access = Integer.parseInt(w[3]) & ~Opcodes.ACC_SYNCHRONIZED;
                    MethodVisitor mv = super.visitMethod(access, w[0], w[1], w[2], null);
                    new WrapperWriter(mv, owner, renamed, w[1], wrapperTargets.get(k)).write();
                    done.add(w[0] + w[1]);
                }
                if (!wrappers.isEmpty()) {
                    super.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, Hooks.FIELD,
                            "Ljava/lang/Object;", null, null).visitEnd();
                    WrapperWriter.writeLookup(super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                            Hooks.LOOKUP, "()Ljava/lang/Object;", null, null), owner);
                }
                super.visitEnd();
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        if (wrappers.isEmpty()) return null;
        return cw.toByteArray();
    }
}
