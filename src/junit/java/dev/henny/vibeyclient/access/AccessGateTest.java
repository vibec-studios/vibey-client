package dev.henny.vibeyclient.access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessGateTest {
    private static final long DAY = 24L * 60 * 60 * 1000;

    @TempDir
    Path dir;

    private final AtomicLong clock = new AtomicLong(1_800_000_000_000L);
    private FakeAccessSource source;
    private AccessCache cache;
    private AccessGate gate;
    private final List<AccessView> seen = new ArrayList<>();

    @BeforeEach
    void setUp() {
        source = new FakeAccessSource();
        cache = new AccessCache(dir.resolve("access.json"));
        gate = new AccessGate(source, cache, clock::get);
        gate.onChange(seen::add);
    }

    private static AccessInfo waitlist(int position) {
        return new AccessInfo(AccessStatus.WAITLIST, position, 40, null);
    }

    private static AccessInfo approved() {
        return AccessInfo.of(AccessStatus.APPROVED);
    }

    @Test
    void approvedOpensAndIsCached() {
        gate.check(false);
        assertEquals(AccessPhase.CHECKING, gate.view().phase());
        assertFalse(gate.open());
        source.answer(approved());
        assertTrue(gate.open());
        assertFalse(gate.view().checking());
        assertTrue(cache.fresh(source.key, clock.get()));
    }

    @Test
    void waitlistStaysClosedAndIsNeverCached() {
        gate.check(false);
        source.answer(waitlist(12));
        AccessView v = gate.view();
        assertEquals(AccessPhase.DENIED, v.phase());
        assertEquals(12, v.info().position());
        assertFalse(cache.fresh(source.key, clock.get()));
    }

    @Test
    void cachedApprovalOpensAtOnceAndSurvivesAnOutage() {
        cache.remember(source.key, clock.get() - 3 * DAY);
        gate.check(false);
        assertTrue(gate.open());
        assertTrue(gate.view().checking());
        source.fail(AccessFailure.Kind.UNREACHABLE);
        assertTrue(gate.open());
        assertFalse(gate.view().checking());
        gate.check(false);
        source.fail(AccessFailure.Kind.NO_LOGIN);
        assertTrue(gate.open());
    }

    @Test
    void successfulCheckRenewsTheCache() {
        cache.remember(source.key, clock.get() - 6 * DAY);
        gate.check(false);
        source.answer(approved());
        clock.addAndGet(5 * DAY);
        assertTrue(cache.fresh(source.key, clock.get()));
    }

    @Test
    void cacheExpiresAfterSevenDays() {
        cache.remember(source.key, clock.get() - 7 * DAY - 1);
        gate.check(false);
        assertFalse(gate.open());
        source.fail(AccessFailure.Kind.UNREACHABLE);
        assertEquals(AccessPhase.UNREACHABLE, gate.view().phase());
    }

    @Test
    void backgroundCheckCanStillClose() {
        cache.remember(source.key, clock.get());
        gate.check(false);
        assertTrue(gate.open());
        source.answer(new AccessInfo(AccessStatus.REJECTED, null, null, "Spam"));
        assertEquals(AccessPhase.DENIED, gate.view().phase());
        assertEquals("Spam", gate.view().info().reason());
        assertFalse(cache.fresh(source.key, clock.get()));
    }

    @Test
    void noLoginAndOutageWithoutCacheStayClosed() {
        gate.check(false);
        source.fail(AccessFailure.Kind.NO_LOGIN);
        assertEquals(AccessPhase.NO_LOGIN, gate.view().phase());
        gate.check(true);
        assertTrue(source.userActions.get(1));
        source.fail(AccessFailure.Kind.UNREACHABLE);
        assertEquals(AccessPhase.UNREACHABLE, gate.view().phase());
    }

    @Test
    void deniedOnAnyRequestClosesAndForgets() {
        gate.check(false);
        source.answer(approved());
        assertTrue(gate.open());
        gate.denied("ACCESS_PENDING");
        assertEquals(AccessPhase.DENIED, gate.view().phase());
        assertEquals(AccessStatus.WAITLIST, gate.view().info().status());
        assertFalse(cache.fresh(source.key, clock.get()));
        assertEquals(2, source.calls.size());
        source.answer(waitlist(7));
        assertEquals(7, gate.view().info().position());
        assertFalse(gate.open());
    }

    @Test
    void followUpAfterDenialNeverReopensByItself() {
        gate.check(false);
        source.answer(approved());
        gate.denied("ACCESS_NONE");
        source.answer(approved());
        assertFalse(gate.open());
        assertEquals(AccessStatus.NONE, gate.view().info().status());
        gate.check(true);
        source.answer(approved());
        assertTrue(gate.open());
    }

    @Test
    void denialWhileClosedAsksNothingMore() {
        gate.check(false);
        source.answer(waitlist(4));
        gate.check(true);
        gate.denied("ACCESS_REJECTED");
        assertEquals(2, source.calls.size());
        assertEquals(AccessStatus.REJECTED, gate.view().info().status());
        assertFalse(gate.view().checking());
        source.answer(approved());
        assertFalse(gate.open());
    }

    @Test
    void retryFromWaitlistSwitchesWithoutRestart() {
        gate.check(false);
        source.answer(waitlist(3));
        gate.check(true);
        assertEquals(AccessPhase.DENIED, gate.view().phase());
        assertTrue(gate.view().checking());
        source.answer(approved());
        assertTrue(gate.open());
    }

    @Test
    void onlineFeaturesOffNeverTouchesTheNetwork() {
        source.online = false;
        gate.check(false);
        assertEquals(AccessPhase.ONLINE_OFF, gate.view().phase());
        cache.remember(source.key, clock.get());
        gate.check(false);
        assertTrue(gate.open());
        assertEquals(0, source.calls.size());
    }

    @Test
    void lateAnswersOfAnOlderCheckAreIgnored() {
        gate.check(false);
        gate.check(true);
        source.calls.get(0).complete(approved());
        assertFalse(gate.open());
        source.answer(waitlist(5));
        assertEquals(5, gate.view().info().position());
    }

    @Test
    void listenersSeeEachChange() {
        gate.check(false);
        source.answer(waitlist(2));
        gate.check(true);
        source.answer(approved());
        List<AccessView> expected = List.of(
                new AccessView(AccessPhase.DENIED, waitlist(2), false),
                new AccessView(AccessPhase.DENIED, waitlist(2), true),
                new AccessView(AccessPhase.OPEN, approved(), false));
        assertEquals(expected, seen);
    }
}
