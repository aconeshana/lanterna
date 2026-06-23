package com.googlecode.lanterna.input;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Verifies {@link KittyKeyPattern} correctly parses Kitty keyboard protocol
 * CSI events ({@code ESC [ keycode ; mods u}).
 */
public class KittyKeyPatternTest {

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
        // ESC [ 97 — waiting for ; or u
        CharacterPattern.Matching m = p.match(chars("\u001B[97"));
        assertNotNull(m);
        assertTrue(m.partialMatch);
        assertNull(m.fullMatch);
    }

    @Test
    public void parsesSimpleCharacterKey() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 ; 1 u → key 'a' (codepoint 97) with Shift modifier
        CharacterPattern.Matching m = p.match(chars("\u001B[97;1u"));
        assertNotNull(m.fullMatch);
        assertTrue(m.fullMatch instanceof KittyKeyStroke);
        KittyKeyStroke kks = (KittyKeyStroke) m.fullMatch;
        assertEquals(97, kks.getKeyCode());
        assertEquals(1, kks.getModifiedCode());
        assertTrue(kks.isShiftDown());
        assertFalse(kks.isCtrlDown());
        assertFalse(kks.isAltDown());
        assertEquals(Character.valueOf('a'), kks.getCharacter());
    }

    @Test
    public void parsesCtrlModifier() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 ; 4 u → key 'a' with Ctrl (bit 2 = 4)
        CharacterPattern.Matching m = p.match(chars("\u001B[97;4u"));
        KittyKeyStroke kks = (KittyKeyStroke) m.fullMatch;
        assertTrue(kks.isCtrlDown());
        assertFalse(kks.isAltDown());
        assertFalse(kks.isShiftDown());
    }

    @Test
    public void parsesAltModifier() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 ; 2 u → key 'a' with Alt (bit 1 = 2)
        CharacterPattern.Matching m = p.match(chars("\u001B[97;2u"));
        KittyKeyStroke kks = (KittyKeyStroke) m.fullMatch;
        assertTrue(kks.isAltDown());
        assertFalse(kks.isCtrlDown());
        assertFalse(kks.isShiftDown());
    }

    @Test
    public void parsesMultipleModifiers() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 ; 7 u → key 'a' with Ctrl+Alt+Shift (1+2+4=7)
        CharacterPattern.Matching m = p.match(chars("\u001B[97;7u"));
        KittyKeyStroke kks = (KittyKeyStroke) m.fullMatch;
        assertTrue(kks.isShiftDown());
        assertTrue(kks.isAltDown());
        assertTrue(kks.isCtrlDown());
    }

    @Test
    public void parsesWithoutModifierCode() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ 97 u → key 'a' with no modifiers
        CharacterPattern.Matching m = p.match(chars("\u001B[97u"));
        KittyKeyStroke kks = (KittyKeyStroke) m.fullMatch;
        assertEquals(97, kks.getKeyCode());
        assertEquals(0, kks.getModifiedCode());
        assertFalse(kks.isShiftDown());
        assertFalse(kks.isCtrlDown());
        assertFalse(kks.isAltDown());
    }

    @Test
    public void rejectsNonDigitPayload() {
        KittyKeyPattern p = new KittyKeyPattern();
        // ESC [ abc u → not a valid kitty key
        assertNull(p.match(chars("\u001B[abcu")));
    }
}
