package dev.henny.vibeyclient.module.clips.ring;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

final class RamBody implements ChunkBody {
    private final BlockPool pool;
    private final List<byte[]> blocks = new ArrayList<>();
    private long size;

    RamBody(BlockPool pool) {
        this.pool = pool;
    }

    @Override
    public void append(byte[] src, int off, int len) {
        while (len > 0) {
            int inBlock = (int) (size % BlockPool.BLOCK);
            if (inBlock == 0) blocks.add(pool.take());
            byte[] b = blocks.get(blocks.size() - 1);
            int n = Math.min(len, BlockPool.BLOCK - inBlock);
            System.arraycopy(src, off, b, inBlock, n);
            off += n;
            len -= n;
            size += n;
        }
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public void seal() {
    }

    @Override
    public boolean writeTo(OutputStream out, long end) throws IOException {
        long left = end;
        for (byte[] b : blocks) {
            if (left <= 0) break;
            int n = (int) Math.min(left, BlockPool.BLOCK);
            out.write(b, 0, n);
            left -= n;
        }
        return true;
    }

    @Override
    public void release() {
        for (byte[] b : blocks) pool.give(b);
        blocks.clear();
    }
}
