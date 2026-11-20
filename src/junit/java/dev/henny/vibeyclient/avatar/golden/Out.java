package dev.henny.vibeyclient.avatar.golden;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

final class Out {
    private final StringBuilder sb = new StringBuilder();

    Out line(Object... parts) {
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(parts[i]);
        }
        sb.append('\n');
        return this;
    }

    static String f(float[] a) {
        if (a == null) return "null";
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) s.append(i > 0 ? " " : "").append(a[i]);
        return s.append(']').toString();
    }

    static String i(int[] a) {
        if (a == null) return "null";
        StringBuilder s = new StringBuilder("[");
        for (int k = 0; k < a.length; k++) s.append(k > 0 ? " " : "").append(a[k]);
        return s.append(']').toString();
    }

    static String m(Matrix4f m) {
        return f(new float[]{m.m00(), m.m01(), m.m02(), m.m10(), m.m11(), m.m12(), m.m20(), m.m21(), m.m22(), m.m30(), m.m31(), m.m32()});
    }

    static String n(Matrix3f m) {
        return f(new float[]{m.m00, m.m01, m.m02, m.m10, m.m11, m.m12, m.m20, m.m21, m.m22});
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
