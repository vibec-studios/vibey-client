package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.mesh.BundleMesh;
import dev.henny.cosmetico.mesh.MeshPart;
import dev.henny.cosmetico.model.Bundle;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

import static dev.henny.cosmetico.golden.Out.f;

public final class MeshDump {
    private MeshDump() {}

    public static String dump(Bundle b) {
        Out o = new Out();
        BundleMesh mesh = BundleMesh.compile(b);
        o.line("bones", mesh.parts.length, "stride", BundleMesh.STRIDE);
        for (int i = 0; i < mesh.parts.length; i++) {
            o.line("bone", i, f(mesh.pivotM[i]), f(mesh.restRotM[i]), "parts", mesh.parts[i].length);
            for (MeshPart p : mesh.parts[i]) {
                float[] first = Arrays.copyOf(p.data, Math.min(p.data.length, BundleMesh.STRIDE * 4));
                o.line("part", p.texture, p.mode, p.quads, p.data.length, sha(p.data), f(first));
            }
        }
        return o.toString();
    }

    static String sha(float[] data) {
        try {
            ByteBuffer buf = ByteBuffer.allocate(data.length * 4);
            for (float v : data) buf.putInt(Float.floatToRawIntBits(v));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(buf.array()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
