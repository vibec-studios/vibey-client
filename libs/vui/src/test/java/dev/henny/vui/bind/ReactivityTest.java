package dev.henny.vui.bind;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactivityTest {
    @Test
    void effectRerunsOnlyWhenItsDependenciesChange() {
        Scheduler s = new Scheduler();
        State<Integer> a = State.of(1);
        State<Integer> b = State.of(10);
        AtomicInteger runs = new AtomicInteger();
        int[] seen = new int[1];
        Effect e = new Effect(() -> {
            runs.incrementAndGet();
            seen[0] = a.get();
        }, s);
        e.run();
        assertEquals(1, runs.get());
        b.set(11);
        s.flush();
        assertEquals(1, runs.get());
        a.set(2);
        a.set(3);
        s.flush();
        assertEquals(2, runs.get());
        assertEquals(3, seen[0]);
    }

    @Test
    void settingTheSameValueDoesNotNotify() {
        Scheduler s = new Scheduler();
        State<String> a = State.of("x");
        AtomicInteger runs = new AtomicInteger();
        new Effect(() -> {
            a.get();
            runs.incrementAndGet();
        }, s).run();
        a.set("x");
        s.flush();
        assertEquals(1, runs.get());
    }

    @Test
    void dependenciesAreRecomputedEachRun() {
        Scheduler s = new Scheduler();
        State<Boolean> flag = State.of(true);
        State<Integer> a = State.of(1);
        State<Integer> b = State.of(2);
        AtomicInteger runs = new AtomicInteger();
        new Effect(() -> {
            runs.incrementAndGet();
            if (flag.get()) a.get();
            else b.get();
        }, s).run();
        flag.set(false);
        s.flush();
        a.set(5);
        s.flush();
        assertEquals(2, runs.get());
        b.set(9);
        s.flush();
        assertEquals(3, runs.get());
    }

    @Test
    void computedIsLazyAndCached() {
        State<Integer> a = State.of(2);
        AtomicInteger evaluations = new AtomicInteger();
        Computed<Integer> square = Computed.of(() -> {
            evaluations.incrementAndGet();
            return a.get() * a.get();
        });
        assertEquals(0, evaluations.get());
        assertEquals(4, square.get());
        assertEquals(4, square.get());
        assertEquals(1, evaluations.get());
        a.set(3);
        assertTrue(square.isStale());
        assertEquals(9, square.get());
        assertEquals(2, evaluations.get());
    }

    @Test
    void effectsSeeComputedChanges() {
        Scheduler s = new Scheduler();
        State<Integer> a = State.of(1);
        Computed<Integer> doubled = Computed.of(() -> a.get() * 2);
        int[] seen = new int[1];
        new Effect(() -> seen[0] = doubled.get(), s).run();
        a.set(4);
        s.flush();
        assertEquals(8, seen[0]);
    }

    @Test
    void listStateNotifiesOnMutation() {
        Scheduler s = new Scheduler();
        ListState<String> list = ListState.of("a");
        AtomicInteger runs = new AtomicInteger();
        new Effect(() -> {
            list.size();
            runs.incrementAndGet();
        }, s).run();
        list.add("b");
        list.move(0, 1);
        s.flush();
        assertEquals(2, runs.get());
        assertEquals(List.of("b", "a"), list.snapshot());
    }

    @Test
    void disposedEffectsStopListening() {
        Scheduler s = new Scheduler();
        State<Integer> a = State.of(1);
        AtomicInteger runs = new AtomicInteger();
        Effect e = new Effect(() -> {
            a.get();
            runs.incrementAndGet();
        }, s);
        e.run();
        e.dispose();
        a.set(2);
        s.flush();
        assertEquals(1, runs.get());
        assertEquals(0, a.observerCount());
    }

    @Test
    void untrackedReadsAreFlagged() {
        Effect e = new Effect(Tracker::untrackedRead, null);
        e.run();
        assertTrue(e.readsUntrackedValues());
        Effect clean = new Effect(() -> State.of(1).get(), null);
        clean.run();
        assertFalse(clean.readsUntrackedValues());
    }

    @Test
    void schedulerStopsRunawayLoops() {
        Scheduler s = new Scheduler();
        State<Integer> a = State.of(0);
        new Effect(() -> a.set(a.get() + 1), s).run();
        a.set(100);
        assertFalse(s.flush());
    }

    @Test
    void modelValuesAreStates() {
        Model m = new Model().value("title", "a");
        State<String> title = m.state("title");
        m.value("title", "b");
        assertEquals("b", title.peek());
        assertEquals("b", m.get("title"));
    }
}
