package com.googlecode.lanterna.input;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Verifies {@link BracketedPastePattern} correctly decodes the DEC 2004
 * bracketed-paste wrapper ({@code ESC [ 200 ~ ... ESC [ 201 ~}) into a
 * {@link PasteKeyStroke} carrying the verbatim pasted text.
 */
public class BracketedPastePatternTest {

    private static List<Character> chars(String s) {
        return s.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
    }

    @Test
    public void rejectsNonPasteSequence() {
        BracketedPastePattern p = new BracketedPastePattern();
        assertNull(p.match(chars("hello")));
        assertNull(p.match(chars("\u001B[A")));  // arrow key, not paste
    }

    @Test
    public void partialMatchOnPrefixOnly() {
        BracketedPastePattern p = new BracketedPastePattern();
        // Just the start marker, no end yet — should be NOT_YET (partial)
        CharacterPattern.Matching m = p.match(chars("\u001B[200~"));
        assertNotNull(m);
        assertTrue(m.partialMatch);
        assertNull(m.fullMatch);
    }

    @Test
    public void fullMatchWithEmptyPaste() {
        BracketedPastePattern p = new BracketedPastePattern();
        // Start + end with nothing between → empty paste
        CharacterPattern.Matching m = p.match(chars("\u001B[200~\u001B[201~"));
        assertNotNull(m);
        assertNotNull(m.fullMatch);
        assertTrue(m.fullMatch instanceof PasteKeyStroke);
        assertEquals("", ((PasteKeyStroke) m.fullMatch).getPastedText());
    }

    @Test
    public void fullMatchWithSimpleText() {
        BracketedPastePattern p = new BracketedPastePattern();
        CharacterPattern.Matching m = p.match(chars("\u001B[200~hello world\u001B[201~"));
        assertNotNull(m.fullMatch);
        assertTrue(m.fullMatch instanceof PasteKeyStroke);
        assertEquals("hello world", ((PasteKeyStroke) m.fullMatch).getPastedText());
    }

    @Test
    public void preservesNewlinesInMultilinePaste() {
        BracketedPastePattern p = new BracketedPastePattern();
        String pasted = "line1\nline2\nline3";
        CharacterPattern.Matching m = p.match(chars("\u001B[200~" + pasted + "\u001B[201~"));
        assertEquals(pasted, ((PasteKeyStroke) m.fullMatch).getPastedText());
    }

    @Test
    public void handlesEmbeddedEscapesInPayload() {
        BracketedPastePattern p = new BracketedPastePattern();
        // Pasted text containing an ESC char (e.g., from a copied ANSI sequence)
        String pasted = "text\u001B[31mred\u001B[0m";
        CharacterPattern.Matching m = p.match(chars("\u001B[200~" + pasted + "\u001B[201~"));
        assertEquals(pasted, ((PasteKeyStroke) m.fullMatch).getPastedText());
    }

    @Test
    public void partialMatchWhileWaitingForEndMarker() {
        BracketedPastePattern p = new BracketedPastePattern();
        // Start + some text, no end marker yet
        CharacterPattern.Matching m = p.match(chars("\u001B[200~partial text"));
        assertNotNull(m);
        assertTrue(m.partialMatch);
        assertNull(m.fullMatch);
    }

    @Test
    public void prefixMustMatchExactly() {
        BracketedPastePattern p = new BracketedPastePattern();
        // Wrong start marker (201 instead of 200) — should not match
        assertNull(p.match(chars("\u001B[201~text\u001B[201~")));
        // Wrong number (199 instead of 200)
        assertNull(p.match(chars("\u001B[199~text\u001B[201~")));
    }

    @Test
    public void keyTypeIsPaste() {
        BracketedPastePattern p = new BracketedPastePattern();
        CharacterPattern.Matching m = p.match(chars("\u001B[200~x\u001B[201~"));
        assertEquals(KeyType.PASTE, m.fullMatch.getKeyType());
    }
}
