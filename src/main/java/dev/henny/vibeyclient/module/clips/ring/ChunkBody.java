package dev.henny.vibeyclient.module.clips.ring;

import java.io.IOException;
import java.io.OutputStream;

interface ChunkBody {
    void append(byte[] src, int off, int len);

    long size();

    void seal();

    boolean writeTo(OutputStream out, long end) throws IOException;

    void release();
}
