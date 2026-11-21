package dev.henny.vibeafk.hook;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

final class WrapperWriter implements Opcodes {
    private static final String OBJ = "java/lang/Object";
    private static final String BIFN = "java/util/function/BiFunction";

    private final MethodVisitor mv;
    private final String owner;
    private final String original;
    private final String desc;
    private final Target target;
    private final Type[] args;
    private final Type ret;
    private final List<Object> locals = new ArrayList<Object>();
    private int nextSlot;

    WrapperWriter(MethodVisitor mv, String owner, String original, String desc, Target target) {
        this.mv = mv;
        this.owner = owner;
        this.original = original;
        this.desc = desc;
        this.target = target;
        this.args = Type.getArgumentTypes(desc);
        this.ret = Type.getReturnType(desc);
        for (Type t : args) {
            locals.add(frameType(t));
            nextSlot += t.getSize();
        }
    }

    void write() {
        mv.visitCode();
        switch (target.kind) {
            case NOTIFY:
                hookCall(target.event, null, 0);
                mv.visitInsn(POP);
                callOriginalAndReturn();
                break;
            case NOTIFY_ARG0:
                hookCall(target.event, args[0], 0);
                mv.visitInsn(POP);
                callOriginalAndReturn();
                break;
            case SKIP_IF_TRUE:
                hookCall(target.event, args[0], 0);
                mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;");
                mv.visitInsn(SWAP);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "equals", "(Ljava/lang/Object;)Z", false);
                Label cont = new Label();
                mv.visitJumpInsn(IFEQ, cont);
                returnDefault();
                mv.visitLabel(cont);
                frame();
                callOriginalAndReturn();
                break;
            case CREATE_GLFW:
                hookCall(Hooks.GLFW_CREATE, null, 0);
                mv.visitInsn(POP);
                callOriginalAndCapture();
                break;
            case CREATE_SDL:
                filterLong(Hooks.SDL_FLAGS, args.length - 1);
                callOriginalAndCapture();
                break;
            case GAIN:
                gain();
                callOriginalAndReturn();
                break;
            default:
                throw new IllegalStateException(target.kind.name());
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void gain() {
        Label skip = new Label();
        Label pop = new Label();
        mv.visitVarInsn(ILOAD, 0);
        mv.visitLdcInsn(0x100A);
        mv.visitJumpInsn(IF_ICMPNE, skip);
        hookCall(target.event, Type.FLOAT_TYPE, 1);
        mv.visitInsn(DUP);
        mv.visitTypeInsn(INSTANCEOF, "java/lang/Float");
        mv.visitJumpInsn(IFEQ, pop);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
        mv.visitVarInsn(FSTORE, 1);
        mv.visitJumpInsn(GOTO, skip);
        mv.visitLabel(pop);
        frame(OBJ);
        mv.visitInsn(POP);
        mv.visitLabel(skip);
        frame();
    }

    private void filterLong(String event, int argIndex) {
        int slot = slotOf(argIndex);
        Label done = new Label();
        Label pop = new Label();
        hookCall(event, Type.LONG_TYPE, slot);
        mv.visitInsn(DUP);
        mv.visitTypeInsn(INSTANCEOF, "java/lang/Long");
        mv.visitJumpInsn(IFEQ, pop);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
        mv.visitVarInsn(LSTORE, slot);
        mv.visitJumpInsn(GOTO, done);
        mv.visitLabel(pop);
        frame(OBJ);
        mv.visitInsn(POP);
        mv.visitLabel(done);
        frame();
    }

    private void hookCall(String event, Type argType, int slot) {
        Label call = new Label();
        Label done = new Label();
        Label have = new Label();
        mv.visitFieldInsn(GETSTATIC, owner, Hooks.FIELD, "Ljava/lang/Object;");
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, have);
        mv.visitInsn(POP);
        mv.visitMethodInsn(INVOKESTATIC, owner, Hooks.LOOKUP, "()Ljava/lang/Object;", false);
        mv.visitLabel(have);
        frame(OBJ);
        mv.visitInsn(DUP);
        mv.visitTypeInsn(INSTANCEOF, BIFN);
        mv.visitJumpInsn(IFNE, call);
        mv.visitInsn(POP);
        mv.visitInsn(ACONST_NULL);
        mv.visitJumpInsn(GOTO, done);
        mv.visitLabel(call);
        frame(OBJ);
        mv.visitTypeInsn(CHECKCAST, BIFN);
        mv.visitLdcInsn(event);
        mv.visitInsn(ICONST_2);
        mv.visitTypeInsn(ANEWARRAY, OBJ);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitLdcInsn(Type.getObjectType(owner));
        mv.visitInsn(AASTORE);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_1);
        if (argType == null) mv.visitInsn(ACONST_NULL);
        else loadBoxed(argType, slot);
        mv.visitInsn(AASTORE);
        mv.visitMethodInsn(INVOKEINTERFACE, BIFN, "apply", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitLabel(done);
        frame(OBJ);
    }

    private void loadBoxed(Type t, int slot) {
        switch (t.getSort()) {
            case Type.LONG:
                mv.visitVarInsn(LLOAD, slot);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case Type.FLOAT:
                mv.visitVarInsn(FLOAD, slot);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case Type.INT:
                mv.visitVarInsn(ILOAD, slot);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case Type.BOOLEAN:
                mv.visitVarInsn(ILOAD, slot);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case Type.OBJECT:
            case Type.ARRAY:
                mv.visitVarInsn(ALOAD, slot);
                break;
            default:
                mv.visitInsn(ACONST_NULL);
        }
    }

    private void loadArgs() {
        int slot = 0;
        for (Type t : args) {
            mv.visitVarInsn(t.getOpcode(ILOAD), slot);
            slot += t.getSize();
        }
    }

    private void callOriginalAndReturn() {
        loadArgs();
        mv.visitMethodInsn(INVOKESTATIC, owner, original, desc, false);
        mv.visitInsn(ret.getOpcode(IRETURN));
    }

    private void callOriginalAndCapture() {
        loadArgs();
        mv.visitMethodInsn(INVOKESTATIC, owner, original, desc, false);
        int slot = nextSlot;
        mv.visitVarInsn(LSTORE, slot);
        locals.add(LONG);
        hookCall(target.event, Type.LONG_TYPE, slot);
        mv.visitInsn(POP);
        mv.visitVarInsn(LLOAD, slot);
        mv.visitInsn(LRETURN);
    }

    private int slotOf(int argIndex) {
        int slot = 0;
        for (int k = 0; k < argIndex; k++) slot += args[k].getSize();
        return slot;
    }

    private void returnDefault() {
        switch (ret.getSort()) {
            case Type.VOID:
                mv.visitInsn(RETURN);
                break;
            case Type.BOOLEAN:
                mv.visitInsn(ICONST_1);
                mv.visitInsn(IRETURN);
                break;
            case Type.INT:
                mv.visitInsn(ICONST_0);
                mv.visitInsn(IRETURN);
                break;
            case Type.LONG:
                mv.visitInsn(LCONST_0);
                mv.visitInsn(LRETURN);
                break;
            default:
                mv.visitInsn(ACONST_NULL);
                mv.visitInsn(ARETURN);
        }
    }

    static void writeLookup(MethodVisitor mv, String owner) {
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        Label ok = new Label();
        mv.visitCode();
        mv.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
        mv.visitLabel(start);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitLdcInsn(Hooks.CORE_CLASS);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitLdcInsn(Hooks.CORE_FIELD);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
        mv.visitInsn(ACONST_NULL);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, ok);
        mv.visitInsn(POP);
        mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;");
        mv.visitLabel(ok);
        mv.visitFrame(F_NEW, 0, new Object[0], 1, new Object[]{OBJ});
        mv.visitInsn(DUP);
        mv.visitFieldInsn(PUTSTATIC, owner, Hooks.FIELD, "Ljava/lang/Object;");
        mv.visitLabel(end);
        mv.visitInsn(ARETURN);
        mv.visitLabel(handler);
        mv.visitFrame(F_NEW, 0, new Object[0], 1, new Object[]{"java/lang/Throwable"});
        mv.visitInsn(POP);
        mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;");
        mv.visitInsn(DUP);
        mv.visitFieldInsn(PUTSTATIC, owner, Hooks.FIELD, "Ljava/lang/Object;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void frame(Object... stack) {
        mv.visitFrame(F_NEW, locals.size(), locals.toArray(), stack.length, stack);
    }

    private static Object frameType(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return INTEGER;
            case Type.FLOAT:
                return FLOAT;
            case Type.LONG:
                return LONG;
            case Type.DOUBLE:
                return DOUBLE;
            case Type.ARRAY:
                return t.getDescriptor();
            default:
                return t.getInternalName();
        }
    }
}
