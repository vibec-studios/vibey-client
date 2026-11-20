package dev.henny.vibeyclient.module.chat.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MentionMatcherTest {
    private final MentionMatcher m = new MentionMatcher("VibeyTester", Words.split("vibey, Henny ,x"));

    @Test
    void ownNameAnywhereCaseInsensitive() {
        assertTrue(m.mentions("<Alex> hey vibeytester, come here"));
        assertTrue(m.mentions("[Mod] Bob: @VibeyTester look"));
    }

    @Test
    void wholeWordsOnly() {
        assertFalse(m.mentions("<Alex> VibeyTesters are great"));
        assertFalse(m.mentions("<Alex> xvibey"));
        assertTrue(m.mentions("<Alex> vibey!"));
    }

    @Test
    void shortExtraWordsAreIgnored() {
        assertEquals(List.of("VibeyTester", "vibey", "Henny"), m.words());
    }

    @Test
    void ownMessagesAreNoMention() {
        assertFalse(m.mentions("<VibeyTester> hi vibey"));
        assertFalse(m.mentions("[Owner] VibeyTester: henny is here"));
        assertFalse(m.mentions("VibeyTester » Henny"));
    }

    @Test
    void rangesAreMerged() {
        List<Ranges.Range> r = m.ranges("Henny and vibeytester");
        assertEquals(List.of(new Ranges.Range(0, 5), new Ranges.Range(10, 21)), r);
    }

    @Test
    void senderPrefix() {
        assertTrue(SenderPrefix.sentBy("<Steve> hello", "Steve"));
        assertTrue(SenderPrefix.sentBy("[VIP] Steve: hello", "Steve"));
        assertFalse(SenderPrefix.sentBy("<Alex> Steve: hello", "Steve"));
        assertFalse(SenderPrefix.sentBy("Alex said Steve is cool", "Steve"));
    }
}
