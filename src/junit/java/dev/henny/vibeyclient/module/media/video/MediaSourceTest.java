package dev.henny.vibeyclient.module.media.video;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaSourceTest {
    @Test
    void youTubeLinks() {
        for (String s : new String[] {"https://www.youtube.com/watch?v=aqz-KE-bpKQ", "https://youtu.be/aqz-KE-bpKQ?si=x",
                "youtube.com/shorts/aqz-KE-bpKQ", "https://www.youtube.com/embed/aqz-KE-bpKQ", "https://m.youtube.com/watch?feature=x&v=aqz-KE-bpKQ"}) {
            MediaSource m = MediaSource.parse(s);
            assertEquals(MediaSource.Kind.YOUTUBE, m.kind(), s);
            assertEquals("aqz-KE-bpKQ", m.youTubeId(), s);
        }
        assertEquals(90, MediaSource.parse("https://youtu.be/aqz-KE-bpKQ?t=1m30s").youTubeStartSeconds());
        assertEquals(42, MediaSource.parse("https://www.youtube.com/watch?v=aqz-KE-bpKQ&t=42").youTubeStartSeconds());
        // no id: just a page
        assertEquals(MediaSource.Kind.WEB, MediaSource.parse("https://www.youtube.com/").kind());
    }

    @Test
    void streamsPagesFiles() throws Exception {
        assertEquals(MediaSource.Kind.STREAM, MediaSource.parse("https://example.org/v/clip.MP4?x=1").kind());
        assertEquals(MediaSource.Kind.STREAM, MediaSource.parse("https://example.org/live/index.m3u8").kind());
        assertEquals(MediaSource.Kind.STREAM, MediaSource.parse("rtmp://example.org/live/key").kind());
        assertEquals(MediaSource.Kind.WEB, MediaSource.parse("https://example.org/").kind());
        assertNull(MediaSource.parse("javascript:alert(1)"));
        assertNull(MediaSource.parse("ftp://example.org/a.mp4"));
        assertNull(MediaSource.parse("not a thing"));
        Path tmp = Files.createTempFile("vibey", ".mp4");
        try {
            MediaSource f = MediaSource.parse("\"" + tmp + "\"");
            assertEquals(MediaSource.Kind.FILE, f.kind());
            assertFalse(f.shareable());
            assertEquals(MediaSource.Kind.FILE, MediaSource.parse(tmp.toUri().toString()).kind());
        } finally {
            Files.deleteIfExists(tmp);
        }
        assertTrue(MediaSource.parse("https://example.org/a.webm").shareable());
        assertFalse(MediaSource.parse("rtmp://example.org/live").shareable());
    }

    @Test
    void remotePolicy() {
        assertTrue(UrlPolicy.acceptRemote(MediaSource.parse("https://www.youtube.com/watch?v=aqz-KE-bpKQ")));
        assertTrue(UrlPolicy.acceptRemote(MediaSource.parse("https://8.8.8.8/a.mp4")));
        for (String bad : new String[] {"http://localhost/a.mp4", "http://127.0.0.1/", "http://192.168.1.10/cam.m3u8", "http://10.0.0.1/",
                "http://[::1]/", "http://169.254.169.254/latest", "http://printer.local/", "http://nas/", "https://user:pw@example.org/",
                "http://100.64.1.1/", "http://[fd00::1]/"}) {
            MediaSource m = MediaSource.parse(bad);
            assertFalse(m != null && UrlPolicy.acceptRemote(m), bad);
        }
        assertFalse(UrlPolicy.acceptRemote(new MediaSource(MediaSource.Kind.FILE, "C:/x.mp4")));
        assertFalse(UrlPolicy.acceptRemote(new MediaSource(MediaSource.Kind.STREAM, "file:///etc/passwd")));
        assertFalse(UrlPolicy.acceptRemote(new MediaSource(MediaSource.Kind.STREAM, "rtmp://example.org/x")));
    }
}
