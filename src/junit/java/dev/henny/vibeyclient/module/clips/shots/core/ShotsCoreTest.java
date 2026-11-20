package dev.henny.vibeyclient.module.clips.shots.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShotsCoreTest {
    private static final Shot A = new Shot("a.png", 300, 10), B = new Shot("B.png", 100, 10), C = new Shot("c.png", 200, 10);
    private static final List<Shot> ALL = List.of(A, B, C);

    private static List<String> names(List<Shot> l) {
        return l.stream().map(Shot::name).toList();
    }

    @Test
    void sortsAndFilters() {
        assertEquals(List.of("a.png", "c.png", "B.png"), names(ShotList.view(ALL, Set.of(), ShotSort.NEWEST, false)));
        assertEquals(List.of("B.png", "c.png", "a.png"), names(ShotList.view(ALL, Set.of(), ShotSort.OLDEST, false)));
        assertEquals(List.of("a.png", "B.png", "c.png"), names(ShotList.view(ALL, Set.of(), ShotSort.NAME, false)));
        assertEquals(List.of("c.png"), names(ShotList.view(ALL, Set.of("c.png"), ShotSort.NAME, true)));
        assertEquals(ShotSort.OLDEST, ShotSort.NEWEST.next());
        assertEquals(ShotSort.NEWEST, ShotSort.NAME.next());
        assertEquals(ShotSort.NAME, ShotSort.parse("name"));
        assertEquals(ShotSort.NEWEST, ShotSort.parse("?"));
    }

    @Test
    void stepsWrapAround() {
        List<Shot> l = ShotList.view(ALL, Set.of(), ShotSort.NEWEST, false);
        assertEquals(C, ShotList.step(l, "a.png", 1));
        assertEquals(A, ShotList.step(l, "B.png", 1));
        assertEquals(B, ShotList.step(l, "a.png", -1));
        assertEquals(A, ShotList.step(l, "missing", 1));
        assertNull(ShotList.step(List.of(), "a.png", 1));
    }

    @Test
    void keepsSelectionNearAfterDelete() {
        List<Shot> l = List.of(A, B);
        assertEquals(B, ShotList.keepOrNear(l, "B.png", 0));
        assertEquals(B, ShotList.keepOrNear(l, "c.png", 5));
        assertNull(ShotList.keepOrNear(List.of(), "c.png", 0));
    }

    @Test
    void stripWindowFollowsSelection() {
        List<Shot> many = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) many.add(new Shot(i + ".png", i, 1));
        assertEquals(List.of("0.png", "1.png", "2.png", "3.png", "4.png"), names(ShotList.window(many, "0.png", 5)));
        assertEquals(List.of("3.png", "4.png", "5.png", "6.png", "7.png"), names(ShotList.window(many, "5.png", 5)));
        assertEquals(List.of("5.png", "6.png", "7.png", "8.png", "9.png"), names(ShotList.window(many, "9.png", 5)));
        assertEquals(3, ShotList.window(ALL, "a.png", 5).size());
    }

    @Test
    void favouritesRoundTrip() {
        Favourites f = new Favourites();
        assertTrue(f.toggle("a.png"));
        assertTrue(f.toggle("b.png"));
        assertFalse(f.toggle("b.png"));
        Favourites back = Favourites.fromJson(f.toJson());
        assertEquals(Set.of("a.png"), back.names());
        assertTrue(f.toJson().contains("\"schema\":1"));
        assertTrue(Favourites.fromJson("{broken").names().isEmpty());
        assertTrue(Favourites.fromJson(null).names().isEmpty());
        back.forget("a.png");
        assertFalse(back.contains("a.png"));
    }

    @Test
    void imageFit() {
        assertArrayEquals(new int[] {240, 0, 1440, 1080}, ImageFit.coverSource(1920, 1080, 4, 3));
        assertArrayEquals(new int[] {0, 0, 1920, 1080}, ImageFit.coverSource(1920, 1080, 16, 9));
        assertArrayEquals(new int[] {0, 149, 1000, 563}, ImageFit.coverSource(1000, 862, 16, 9));
        assertArrayEquals(new int[] {120, 0, 720, 540}, ImageFit.containTarget(800, 600, 960, 540));
    }

    @Test
    void downscaleAveragesAndLetterboxes() {
        int[] src = {0xFF000000, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF};
        int[] one = Downscale.box(src, 2, 0, 0, 2, 2, 1, 1);
        assertEquals(0xFF7F7F7F, one[0]);
        int[] boxed = Downscale.contain(new int[] {0xFFFF0000}, 1, 1, 3, 1);
        assertEquals(Downscale.BACKGROUND, boxed[0]);
        assertEquals(0xFFFF0000, boxed[1]);
        assertEquals(9, Downscale.cover(new int[16 * 9], 16, 9, 3, 3).length);
    }

    @Test
    void deleteNeedsASecondClick() {
        ArmedConfirm c = new ArmedConfirm();
        assertFalse(c.press("a", 0));
        assertTrue(c.armed("a", 100));
        assertTrue(c.press("a", 100));
        assertFalse(c.armed("a", 100));
        assertFalse(c.press("a", 0));
        assertFalse(c.press("a", ArmedConfirm.WINDOW_MS + 1));
        assertFalse(c.press("b", ArmedConfirm.WINDOW_MS + 2));
        assertFalse(c.armed("a", ArmedConfirm.WINDOW_MS + 2));
        assertEquals("a.png|300|10", A.key());
    }
}
