package com.googlecode.lanterna.input;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Verifies {@link KittyKeyPattern} correctly parses Kitty keyboard protocol
 * CSI events ({@code ESC [ keycode ; mods u}).
 * <p>
 * The modifier field of a CSI-u event is transmitted as {@code 1 + bitmask}
 * (bit 0 = shift, bit 1 = alt, bit 2 = ctrl), so {@code ;1u} means "no
 * modifiers" and {@code ;5u} means Ctrl.
 */
public class KittyKeyPatternTest {

    private static final String ESC = String.valueOf(KeyDecodingProfile.ESC_CODE);

    private static List<Character> chars(String s) {
        return s.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
    }

    @Test
    public void rejectsNonKittyInput() {
        KittyKeyPattern p = new KittyKeyPattern();
        assertNull(p.match(chars("hello")));
        assertNull(p.match(chars("abc")));
    }

    @Test
    public void partialMatchWaitingForTerminator() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 - waiting for ; or u
        CharacterPattern.Matching m = p.match(chars(ESC + "[97"));
        assertNotNull(m);
        assertTrue(m.partialMatch);
        assertNull(m.fullMatch);
    }

    @Test
    public void parsesSimpleCharacterKey() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 ; 1 u -> key 'a' (codepoint 97) with no modifiers
        CharacterPattern.Matching m = p.match(chars(ESC + "[97;1u"));
        assertNotNull(m.fullMatch);
        assertTrue(m.fullMatch instanceof KittyKeyStroke);
        KittyKeyStroke kks = (KittyKeyStroke) m.fullMatch;
        assertEquals(97, kks.getKeyCode());
        assertEquals(1, kks.getModifiedCode());
        assertFalse(kks.isShiftDown());
        assertFalse(kks.isCtrlDown());
        assertFalse(kks.isAltDown());
        assertEquals(Character.valueOf('a'), kks.getCharacter());
    }

    @Test
    public void parsesShiftModifier() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 ; 2 u -> key 'a' with Shift (1 + bit 0)
        CharacterPattern.Matching m = p.match(chars(ESC + "[97;2u"));
        KittyKeyStroke kks = (KittyKeyStroke) m.fullMatch;
        assertTrue(kks.isShiftDown());
        assertFalse(kks.isCtrlDown());
        assertFalse(kks.isAltDown());
    }

    @Test
    public void parsesCtrlModifier() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 ; 5 u -> key 'a' with Ctrl (1 + bit 2)
        CharacterPattern.Matching m = p.match(chars(ESC + "[97;5u"));
        KittyKeyStroke kks = (KittyKeyStroke) m.fullMatch;
        assertTrue(kks.isCtrlDown());
        assertFalse(kks.isAltDown());
        assertFalse(kks.isShiftDown());
    }

    @Test
    public void parsesAltModifier() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 ; 3 u -> key 'a' with Alt (1 + bit 1)
        CharacterPattern.Matching m = p.match(chars(ESC + "[97;3u"));
        KittyKeyStroke kks = (KittyKeyStroke) m.fullMatch;
        assertTrue(kks.isAltDown());
        assertFalse(kks.isCtrlDown());
        assertFalse(kks.isShiftDown());
    }

    @Test
    public void parsesMultipleModifiers() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 ; 8 u -> key 'a' with Ctrl+Alt+Shift (1 + 1 + 2 + 4)
        CharacterPattern.Matching m = p.match(chars(ESC + "[97;8u"));
        KittyKeyStroke kks = (KittyKeyStroke) m.fullMatch;
        assertTrue(kks.isShiftDown());
        assertTrue(kks.isAltDown());
        assertTrue(kks.isCtrlDown());
    }

    @Test
    public void parsesWithoutModifierCode() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 u -> key 'a' with no modifiers
        CharacterPattern.Matching m = p.match(chars(ESC + "[97u"));
        KittyKeyStroke kks = (KittyKeyStroke) m.fullMatch;
        assertEquals(97, kks.getKeyCode());
        assertEquals(0, kks.getModifiedCode());
        assertFalse(kks.isShiftDown());
        assertFalse(kks.isCtrlDown());
        assertFalse(kks.isAltDown());
    }

    @Test
    public void mapsNamedKeyCodesToKeyTypes() {
        KittyKeyPattern p = new KittyKeyPattern();
        assertEquals(KeyType.ESCAPE, p.match(chars(ESC + "[27u")).fullMatch.getKeyType());
        assertEquals(KeyType.ENTER, p.match(chars(ESC + "[13u")).fullMatch.getKeyType());
        assertEquals(KeyType.TAB, p.match(chars(ESC + "[9u")).fullMatch.getKeyType());
        assertEquals(KeyType.BACKSPACE, p.match(chars(ESC + "[127u")).fullMatch.getKeyType());
        // Shift+Tab (1 + bit 0 = 2) becomes REVERSE_TAB
        assertEquals(KeyType.REVERSE_TAB, p.match(chars(ESC + "[9;2u")).fullMatch.getKeyType());
    }

    @Test
    public void rejectsNonDigitPayload() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ abc u -> not a valid kitty key
        assertNull(p.match(chars(ESC + "[abcu")));
    }
}
