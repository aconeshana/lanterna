package com.googlecode.lanterna.input;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Verifies {@link TerminalQueryResponsePattern} correctly parses DECRPM
 * and DA1/DA2 responses from the terminal.
 */
public class TerminalQueryResponsePatternTest {

    private static List<Character> chars(String s) {
        return s.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
    }

    @Test
    public void parsesDecrpmResponse() {
        TerminalQueryResponsePattern p = new TerminalQueryResponsePattern();
        // DECRPM: ESC [ ? 2026 ; 1 $ y → mode 2026 (sync output) is set (status=1)
        CharacterPattern.Matching m = p.match(chars("\u001B[?2026;1$y"));
        assertNotNull(m.fullMatch);
        assertTrue(m.fullMatch instanceof TerminalQueryResponseKeyStroke);
        TerminalQueryResponseKeyStroke r = (TerminalQueryResponseKeyStroke) m.fullMatch;
        assertEquals(TerminalQueryResponseKeyStroke.QueryType.DECRQM, r.queryType());
        assertEquals(2026, r.mode());
        assertEquals(1, r.status());
    }

    @Test
    public void parsesDecrpmNotRecognized() {
        TerminalQueryResponsePattern p = new TerminalQueryResponsePattern();
        // status 0 = not recognized
        CharacterPattern.Matching m = p.match(chars("\u001B[?2026;0$y"));
        TerminalQueryResponseKeyStroke r = (TerminalQueryResponseKeyStroke) m.fullMatch;
        assertEquals(0, r.status());
    }

    @Test
    public void parsesDecrpmReset() {
        TerminalQueryResponsePattern p = new TerminalQueryResponsePattern();
        // status 2 = reset
        CharacterPattern.Matching m = p.match(chars("\u001B[?2004;2$y"));
        TerminalQueryResponseKeyStroke r = (TerminalQueryResponseKeyStroke) m.fullMatch;
        assertEquals(2004, r.mode());
        assertEquals(2, r.status());
    }

    @Test
    public void parsesDa1Response() {
        TerminalQueryResponsePattern p = new TerminalQueryResponsePattern();
        // DA1: ESC [ ? 1 ; 2 ; 6 ; 22 c
        CharacterPattern.Matching m = p.match(chars("\u001B[?1;2;6;22c"));
        assertNotNull(m.fullMatch);
        TerminalQueryResponseKeyStroke r = (TerminalQueryResponseKeyStroke) m.fullMatch;
        assertEquals(TerminalQueryResponseKeyStroke.QueryType.DA1, r.queryType());
        assertEquals("1;2;6;22", r.payload());
    }

    @Test
    public void parsesDa2Response() {
        TerminalQueryResponsePattern p = new TerminalQueryResponsePattern();
        // DA2: ESC [ > 0 ; 95 ; 0 c
        CharacterPattern.Matching m = p.match(chars("\u001B[>0;95;0c"));
        assertNotNull(m.fullMatch);
        TerminalQueryResponseKeyStroke r = (TerminalQueryResponseKeyStroke) m.fullMatch;
        assertEquals(TerminalQueryResponseKeyStroke.QueryType.DA2, r.queryType());
        assertEquals("0;95;0", r.payload());
    }

    @Test
    public void rejectsNonQueryInput() {
        TerminalQueryResponsePattern p = new TerminalQueryResponsePattern();
        assertNull(p.match(chars("hello")));
        assertNull(p.match(chars("\u001B[A"))); // arrow key, not a query response
    }

    @Test
    public void partialMatchWaitingForTerminator() {
        TerminalQueryResponsePattern p = new TerminalQueryResponsePattern();
        CharacterPattern.Matching m = p.match(chars("\u001B[?2026;1$"));
        assertNotNull(m);
        assertTrue(m.partialMatch);
        assertNull(m.fullMatch);
    }

    @Test
    public void keyTypeIsTerminalQueryResponse() {
        TerminalQueryResponsePattern p = new TerminalQueryResponsePattern();
        CharacterPattern.Matching m = p.match(chars("\u001B[?2026;1$y"));
        assertEquals(KeyType.TERMINAL_QUERY_RESPONSE, m.fullMatch.getKeyType());
    }
}
