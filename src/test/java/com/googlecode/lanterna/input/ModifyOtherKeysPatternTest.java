package com.googlecode.lanterna.input;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Verifies {@link ModifyOtherKeysPattern} correctly parses xterm's
 * {@code modifyOtherKeys} extended-key events ({@code ESC [ 27 ; mods ; code ~}).
 * <p>
 * The modifier field uses the same {@code 1 + bitmask} convention as
 * {@link KittyKeyPattern} (bit 0 = shift, bit 1 = alt, bit 2 = ctrl).
 */
public class ModifyOtherKeysPatternTest {

    private static final String ESC = String.valueOf(KeyDecodingProfile.ESC_CODE);

    private static List<Character> chars(String s) {
        return s.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
    }

    @Test
    public void rejectsNonMatchingInput() {
        ModifyOtherKeysPattern p = new ModifyOtherKeysPattern();
        assertNull(p.match(chars("hello")));
        assertNull(p.match(chars(ESC + "[A"))); // plain arrow-up, not this pattern's shape
    }

    @Test
    public void partialMatchWaitingForTerminator() {
        ModifyOtherKeysPattern p = new ModifyOtherKeysPattern();
        CharacterPattern.Matching m = p.match(chars(ESC + "[27;5;101"));
        assertNotNull(m);
        assertTrue(m.partialMatch);
        assertNull(m.fullMatch);
    }

    @Test
    public void parsesCtrlE() {
        ModifyOtherKeysPattern p = new ModifyOtherKeysPattern();
        // ESC [ 27 ; 5 ; 101 ~ -> Ctrl+E (101 = 'e', 5 = 1 + ctrl bit)
        CharacterPattern.Matching m = p.match(chars(ESC + "[27;5;101~"));
        assertNotNull(m.fullMatch);
        assertEquals(KeyType.CHARACTER, m.fullMatch.getKeyType());
        assertEquals(Character.valueOf('e'), m.fullMatch.getCharacter());
        assertTrue(m.fullMatch.isCtrlDown());
        assertFalse(m.fullMatch.isAltDown());
        assertFalse(m.fullMatch.isShiftDown());
    }

    @Test
    public void parsesAltShiftModifiers() {
        ModifyOtherKeysPattern p = new ModifyOtherKeysPattern();
        // ESC [ 27 ; 4 ; 65 ~ -> Alt+Shift+A (65 = 'A', 4 = 1 + shift(1) + alt(2))
        CharacterPattern.Matching m = p.match(chars(ESC + "[27;4;65~"));
        assertEquals(Character.valueOf('A'), m.fullMatch.getCharacter());
        assertTrue(m.fullMatch.isAltDown());
        assertTrue(m.fullMatch.isShiftDown());
        assertFalse(m.fullMatch.isCtrlDown());
    }

    @Test
    public void mapsNamedControlCodesToKeyTypes() {
        ModifyOtherKeysPattern p = new ModifyOtherKeysPattern();
        assertEquals(KeyType.ESCAPE, p.match(chars(ESC + "[27;5;27~")).fullMatch.getKeyType());
        assertEquals(KeyType.ENTER, p.match(chars(ESC + "[27;5;13~")).fullMatch.getKeyType());
        assertEquals(KeyType.TAB, p.match(chars(ESC + "[27;5;9~")).fullMatch.getKeyType());
        assertEquals(KeyType.BACKSPACE, p.match(chars(ESC + "[27;5;127~")).fullMatch.getKeyType());
    }

    @Test
    public void rejectsFunctionKeyShapeWithoutLeading27() {
        ModifyOtherKeysPattern p = new ModifyOtherKeysPattern();
        // ESC [ 17 ; 6 ~ is Ctrl+Shift+F6 (EscapeSequenceCharacterPattern's
        // territory) -- the leading field is not the fixed "27" marker.
        assertNull(p.match(chars(ESC + "[17;6~")));
    }

    @Test
    public void rejectsNonDigitPayload() {
        ModifyOtherKeysPattern p = new ModifyOtherKeysPattern();
        assertNull(p.match(chars(ESC + "[27;a;b~")));
    }

    @Test
    public void doesNotCollideWithKittyProtocolTerminator() {
        ModifyOtherKeysPattern p = new ModifyOtherKeysPattern();
        // Kitty's CSI-u form terminates on 'u', not '~' -- this pattern must
        // not claim it (KittyKeyPattern owns that shape).
        CharacterPattern.Matching m = p.match(chars(ESC + "[101;5u"));
        assertNull(m);
    }
}
