package dev.henny.vibeyclient.api.privacy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NameRedactorTest {
    private static NameAliases aliases() {
        NameAliases a = new NameAliases();
        a.own("VibeyTester", "Me");
        a.otherFormat("Player %d");
        return a;
    }

    @Test
    void replacesWholeNamesWithStableAliases() {
        NameRedactor r = NameRedactor.of(List.of("VibeyTester", "Alex", "Alexander"), aliases());
        assertEquals("<Player 1> hi Me, ask Player 2", r.redact("<Alexander> hi vibeytester, ask Alex"));
        assertEquals("Player 2 Player 1", r.redact("Alex Alexander"));
        assertEquals("Alexa stays", r.redact("Alexa stays"));
    }

    @Test
    void hitsAreSortedAndDoNotOverlap() {
        NameRedactor r = NameRedactor.of(List.of("Al", "Alex"), aliases());
        List<NameRedactor.Hit> h = r.hits("Alex Al");
        assertEquals(2, h.size());
        assertEquals("Alex", h.get(0).name());
        assertEquals(5, h.get(1).start());
    }

    @Test
    void emptyRedactorChangesNothing() {
        assertFalse(NameRedactor.NONE.active());
        assertEquals("Alex", NameRedactor.NONE.redact("Alex"));
    }

    @Test
    void privacyFacadeOnlyRedactsWhileNamesAreHidden() {
        Privacy.set(Privacy.Flags.OFF);
        Privacy.names(List.of("Alex"));
        assertFalse(Privacy.redactor().active());
        Privacy.set(new Privacy.Flags(true, false, true));
        assertEquals(true, Privacy.redactor().active());
        assertEquals(true, Privacy.hideServer());
        Privacy.set(Privacy.Flags.OFF);
        assertFalse(Privacy.hideNames());
    }
}
