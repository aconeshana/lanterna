package com.googlecode.lanterna.input;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Verifies {@link EscapeEnterPattern} maps the ESC-prefixed Enter sequences that
 * terminals emit for Alt/Option+Enter, and — just as importantly — that it actually
 * wins the arbitration inside {@link InputDecoder}.
 * <p>
 * The decoder does not stop at the first full match: {@code getBestMatch()} lets the
 * last registered pattern that fully matches win. {@link CtrlAltAndCharacterPattern}
 * also fully matches ESC+CR / ESC+LF (as ctrl+alt+m / ctrl+alt+j), so registering
 * this pattern before it silently turns it into dead code. The decoder-level tests
 * below are what guard that ordering.
 */
public class EscapeEnterPatternTest {

    private static final char ESC = KeyDecodingProfile.ESC_CODE;

    private static List<Character> chars(String s) {
        return s.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
    }

    private static KeyStroke decodeSingle(String input) throws Exception {
        InputDecoder decoder = new InputDecoder(new StringReader(input));
        decoder.addProfile(new DefaultKeyDecodingProfile());
        return decoder.getNextCharacter(false);
    }

    @Test
    public void matchesEscapeCarriageReturn() {
        EscapeEnterPattern pattern = new EscapeEnterPattern();
        CharacterPattern.Matching m = pattern.match(chars(ESC + "\r"));
        assertNotNull(m);
        assertNotNull(m.fullMatch);
        assertEquals(KeyType.ENTER, m.fullMatch.getKeyType());
        assertTrue(m.fullMatch.isAltDown());
        assertFalse(m.fullMatch.isCtrlDown());
    }

    @Test
    public void matchesEscapeLineFeed() {
        EscapeEnterPattern pattern = new EscapeEnterPattern();
        CharacterPattern.Matching m = pattern.match(chars(ESC + "\n"));
        assertNotNull(m);
        assertNotNull(m.fullMatch);
        assertEquals(KeyType.ENTER, m.fullMatch.getKeyType());
        assertTrue(m.fullMatch.isAltDown());
    }

    @Test
    public void bareEscapeIsPartialMatch() {
        EscapeEnterPattern pattern = new EscapeEnterPattern();
        CharacterPattern.Matching m = pattern.match(chars(String.valueOf(ESC)));
        assertNotNull(m);
        assertTrue(m.partialMatch);
        assertNull(m.fullMatch);
    }

    @Test
    public void rejectsSequencesItDoesNotOwn() {
        EscapeEnterPattern pattern = new EscapeEnterPattern();
        assertNull(pattern.match(chars("\r")));
        assertNull(pattern.match(chars(ESC + "a")));
        assertNull(pattern.match(chars(ESC + "[A")));
        // Anything longer than the two-character sequence belongs to another pattern
        assertNull(pattern.match(chars(ESC + "\r\r")));
    }

    @Test
    public void decoderPrefersEscapeEnterOverCtrlAlt() throws Exception {
        // Registration order regression guard: CtrlAltAndCharacterPattern would
        // otherwise report ctrl+alt+m here.
        KeyStroke ks = decodeSingle(ESC + "\r");
        assertEquals(KeyType.ENTER, ks.getKeyType());
        assertTrue(ks.isAltDown());
        assertFalse(ks.isCtrlDown());
    }

    @Test
    public void decoderMapsEscapeLineFeedToAltEnter() throws Exception {
        // Would otherwise be reported as ctrl+alt+j.
        KeyStroke ks = decodeSingle(ESC + "\n");
        assertEquals(KeyType.ENTER, ks.getKeyType());
        assertTrue(ks.isAltDown());
        assertFalse(ks.isCtrlDown());
    }

    @Test
    public void decoderStillMapsBareCarriageReturnToPlainEnter() throws Exception {
        KeyStroke ks = decodeSingle("\r");
        assertEquals(KeyType.ENTER, ks.getKeyType());
        assertFalse(ks.isAltDown());
        assertFalse(ks.isCtrlDown());
    }

    @Test
    public void decoderStillMapsCtrlAltLetters() throws Exception {
        // Make sure moving this pattern after CtrlAltAndCharacterPattern did not
        // steal the ordinary ctrl+alt+<letter> combinations. 0x01 is ctrl+a.
        KeyStroke ks = decodeSingle(ESC + "\u0001");
        assertEquals(KeyType.CHARACTER, ks.getKeyType());
        assertEquals(Character.valueOf('a'), ks.getCharacter());
        assertTrue(ks.isCtrlDown());
        assertTrue(ks.isAltDown());
    }
}
