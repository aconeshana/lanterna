package com.googlecode.lanterna.input;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Verifies {@link OSCResponsePattern} correctly decodes unsolicited OSC
 * responses from the terminal (e.g., {@code OSC 11} background color
 * replies, {@code OSC 52} clipboard reads).
 */
public class OSCResponsePatternTest {

    private static List<Character> chars(String s) {
        return s.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
    }

    @Test
    public void rejectsNonOSCInput() {
        OSCResponsePattern p = new OSCResponsePattern();
        assertNull(p.match(chars("hello")));
        assertNull(p.match(chars("abc")));
    }

    @Test
    public void partialMatchOnEscBracketOnly() {
        OSCResponsePattern p = new OSCResponsePattern();
        // Just ESC ] — start of OSC, waiting for more
        CharacterPattern.Matching m = p.match(chars("\u001B]"));
        assertNotNull(m);
        assertTrue(m.partialMatch);
        assertNull(m.fullMatch);
    }

    @Test
    public void parsesSTTerminatedResponse() {
        OSCResponsePattern p = new OSCResponsePattern();
        // OSC 11 bg color response: ESC ] 11 ; rgb:rrrr/gggg/bbbb ESC \
        CharacterPattern.Matching m = p.match(chars("\u001B]11;rgb:abcd/ef01/2345\u001B\\"));
        assertNotNull(m.fullMatch);
        assertTrue(m.fullMatch instanceof OSCResponseKeyStroke);
        OSCResponseKeyStroke osc = (OSCResponseKeyStroke) m.fullMatch;
        assertEquals("11", osc.code());
        assertEquals("rgb:abcd/ef01/2345", osc.payload());
    }

    @Test
    public void parsesBELTerminatedResponse() {
        OSCResponsePattern p = new OSCResponsePattern();
        // xterm-style BEL-terminated OSC 52 clipboard response
        CharacterPattern.Matching m = p.match(chars("\u001B]52;c;cGVzdA==\u0007"));
        assertNotNull(m.fullMatch);
        OSCResponseKeyStroke osc = (OSCResponseKeyStroke) m.fullMatch;
        assertEquals("52", osc.code());
        assertEquals("c;cGVzdA==", osc.payload());
    }

    @Test
    public void handlesResponseWithoutPayload() {
        OSCResponsePattern p = new OSCResponsePattern();
        // OSC with code but no semicolon/payload
        CharacterPattern.Matching m = p.match(chars("\u001B]99\u001B\\"));
        assertNotNull(m.fullMatch);
        OSCResponseKeyStroke osc = (OSCResponseKeyStroke) m.fullMatch;
        assertEquals("99", osc.code());
        assertEquals("", osc.payload());
    }

    @Test
    public void keyTypeIsOSCResponse() {
        OSCResponsePattern p = new OSCResponsePattern();
        CharacterPattern.Matching m = p.match(chars("\u001B]11;rgb:0/0/0\u001B\\"));
        assertEquals(KeyType.OSC_RESPONSE, m.fullMatch.getKeyType());
    }

    @Test
    public void partialMatchWaitingForTerminator() {
        OSCResponsePattern p = new OSCResponsePattern();
        // OSC payload but no terminator yet
        CharacterPattern.Matching m = p.match(chars("\u001B]11;rgb:abc"));
        assertNotNull(m);
        assertTrue(m.partialMatch);
        assertNull(m.fullMatch);
    }
}
