package dev.henny.vibeyclient.avatar.golden;

import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.render.BundleMesh;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;

import static dev.henny.vibeyclient.avatar.golden.Out.f;

final class MeshDump {
    private MeshDump() {}

    static String dump(Bundle b) {
        Out o = new Out();
        BundleMesh mesh = BundleMesh.compile(b);
        o.line("bones", mesh.parts.length, "stride", BundleMesh.STRIDE);
        for (int i = 0; i < mesh.parts.length; i++) {
            o.line("bone", i, f(mesh.pivotM[i]), f(mesh.restRotM[i]), "parts", mesh.parts[i].length);
            for (BundleMesh.Part p : mesh.parts[i]) {
                float[] first = java.util.Arrays.copyOf(p.data, Math.min(p.data.length, BundleMesh.STRIDE * 4));
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
