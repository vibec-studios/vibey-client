package dev.henny.vibeyclient.module.waypoints.core.map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class TileCodec {
    public static final int MAGIC = 0x56575431;
    public static final int MAX_BYTES = 512 * 1024;

    private TileCodec() {}

    public static byte[] encode(TileData tile) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(16 * 1024);
        DataOutputStream head = new DataOutputStream(bytes);
        head.writeInt(MAGIC);
        head.writeShort(TileKey.SIZE);
        try (DataOutputStream out = new DataOutputStream(new DeflaterOutputStream(bytes))) {
            synchronized (tile) {
                for (int v : tile.base) out.writeInt(v);
                for (short h : tile.heights) out.writeShort(h);
            }
        }
        return bytes.toByteArray();
    }

    public static TileData decode(byte[] data) throws IOException {
        if (data.length > MAX_BYTES) throw new IOException("tile too big");
        DataInputStream head = new DataInputStream(new ByteArrayInputStream(data));
        if (head.readInt() != MAGIC || head.readShort() != TileKey.SIZE) throw new IOException("not a tile");
        int[] base = new int[TileData.PIXELS];
        short[] heights = new short[TileData.PIXELS];
        try (DataInputStream in = new DataInputStream(new InflaterInputStream(head))) {
            for (int i = 0; i < base.length; i++) base[i] = in.readInt();
            for (int i = 0; i < heights.length; i++) heights[i] = in.readShort();
        }
        return new TileData(base, heights);
    }
}
