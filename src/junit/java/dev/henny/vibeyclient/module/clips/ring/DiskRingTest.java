package dev.henny.vibeyclient.module.clips.ring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiskRingTest {
    @TempDir
    Path tmp;

    private static byte[] written(List<Chunk.View> views) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Chunk.View v : views) assertTrue(v.writeTo(out));
        return out.toByteArray();
    }

    private static long files(Path dir) throws Exception {
        if (!Files.isDirectory(dir)) return 0;
        try (Stream<Path> s = Files.list(dir)) {
            return s.count();
        }
    }

    @Test
    void diskAndRamHoldTheSameBytes() throws Exception {
        byte[] s = TsChunkerTest.stream(8, 60, 0);
        ClipRing ram = new ClipRing(600_000, 1L << 30);
        ClipRing disk = new ClipRing(600_000, 1L << 30, ChunkStorage.disk(tmp.resolve("b")));
        TsChunker a = new TsChunker(ram, 1);
        TsChunker b = new TsChunker(disk, 1);
        TsChunkerTest.feedRandomly(a, s, 7);
        TsChunkerTest.feedRandomly(b, s, 7);
        assertTrue(disk.onDisk());
        assertEquals(ram.bytes(), disk.bytes());
        assertEquals(ram.durationMs(), disk.durationMs());
        assertArrayEquals(written(ram.all()), written(disk.all()), "open chunk included");
        a.end();
        b.end();
        assertArrayEquals(written(ram.all()), written(disk.all()));
        assertEquals(8, files(tmp.resolve("b")));
        assertNull(disk.storage().error());
    }

    @Test
    void droppedChunksDeleteTheirFiles() throws Exception {
        Path dir = tmp.resolve("b");
        ClipRing ring = new ClipRing(4000, 1L << 30, ChunkStorage.disk(dir));
        TsChunker c = new TsChunker(ring, 1);
        byte[] s = TsChunkerTest.stream(10, 60, 0);
        c.feed(s, 0, s.length);
        c.end();
        assertTrue(ring.droppedChunks() > 0);
        assertEquals(ring.chunks(), files(dir));
    }

    @Test
    void clearRemovesTheFolderAndLateChunksLeaveNothing() throws Exception {
        Path dir = tmp.resolve("b");
        ClipRing ring = new ClipRing(600_000, 1L << 30, ChunkStorage.disk(dir));
        TsChunker c = new TsChunker(ring, 1);
        byte[] s = TsChunkerTest.stream(4, 60, 0);
        c.feed(s, 0, s.length);
        List<Chunk.View> v = ring.all();
        ring.clear();
        assertFalse(Files.exists(dir));
        assertFalse(v.get(0).writeTo(new ByteArrayOutputStream()));
        byte[] more = TsChunkerTest.stream(3, 60, 4 * 60 * 3000L);
        c.feed(more, 0, more.length);
        c.end();
        assertEquals(0, ring.chunks());
        assertEquals(0, files(dir));
    }

    @Test
    void longBufferKeepsMinutes() throws Exception {
        ClipRing ring = new ClipRing(5 * 60_000 + 2000, 1L << 30, ChunkStorage.disk(tmp.resolve("b")));
        TsChunker c = new TsChunker(ring, 1);
        for (int i = 0; i < 200; i++) {
            byte[] s = TsChunkerTest.stream(1, 60, i * 60 * 3000L);
            c.feed(s, 0, s.length);
        }
        c.end();
        long ms = ring.durationMs();
        assertTrue(ms >= 5 * 60_000 && ms <= 5 * 60_000 + 4000, "held " + ms);
        byte[] all = written(ring.all());
        assertEquals(0, all.length % 188);
        assertEquals(0x47, all[0] & 0xFF);
    }
}
