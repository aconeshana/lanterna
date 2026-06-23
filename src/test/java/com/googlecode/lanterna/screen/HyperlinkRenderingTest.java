package com.googlecode.lanterna.screen;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Verifies that {@link TerminalScreen} correctly handles TextCharacters
 * carrying protocol fields (hyperlinkUrl, promptMarker, imageCell).
 * <p>
 * Since {@link DefaultVirtualTerminal} overrides the protocol emission
 * methods as no-ops (it's an in-memory mock), these tests verify that:
 * <ol>
 *   <li>TextCharacters with protocol fields are stored correctly in the
 *       screen buffer</li>
 *   <li>The equals/hashCode contract includes protocol fields (so the
 *       diff loop detects changes)</li>
 *   <li>Refresh doesn't throw when protocol fields are present</li>
 * </ol>
 */
public class HyperlinkRenderingTest {

    @Test
    public void textCharacterWithHyperlinkStoredInScreenBuffer() throws IOException {
        DefaultVirtualTerminal term = new DefaultVirtualTerminal(new TerminalSize(20, 5));
        TerminalScreen screen = new TerminalScreen(term);
        screen.startScreen();

        TextCharacter linked = new TextCharacter('X', TextColor.ANSI.WHITE, TextColor.ANSI.DEFAULT)
            .withHyperlink("https://example.com");
        screen.setCharacter(0, 0, linked);
        screen.refresh();

        TextCharacter read = screen.getFrontCharacter(0, 0);
        assertEquals("https://example.com", read.getHyperlinkUrl());
        screen.stopScreen();
    }

    @Test
    public void textCharacterWithPromptMarkerStoredInScreenBuffer() throws IOException {
        DefaultVirtualTerminal term = new DefaultVirtualTerminal(new TerminalSize(20, 5));
        TerminalScreen screen = new TerminalScreen(term);
        screen.startScreen();

        TextCharacter marked = new TextCharacter('>', TextColor.ANSI.GREEN, TextColor.ANSI.DEFAULT)
            .withPromptMarker(com.googlecode.lanterna.PromptMarker.PROMPT_START);
        screen.setCharacter(0, 0, marked);
        screen.refresh();

        TextCharacter read = screen.getFrontCharacter(0, 0);
        assertEquals(com.googlecode.lanterna.PromptMarker.PROMPT_START, read.getPromptMarker());
        screen.stopScreen();
    }

    @Test
    public void hyperlinkChangeTriggersDiff() throws IOException {
        DefaultVirtualTerminal term = new DefaultVirtualTerminal(new TerminalSize(20, 5));
        TerminalScreen screen = new TerminalScreen(term);
        screen.startScreen();

        // Set initial character without hyperlink
        TextCharacter plain = new TextCharacter('A', TextColor.ANSI.WHITE, TextColor.ANSI.DEFAULT);
        screen.setCharacter(0, 0, plain);
        screen.refresh();

        // Change to same character but WITH hyperlink — should be detected as different
        TextCharacter linked = plain.withHyperlink("https://test.com");
        assertNotEquals("Plain and linked should differ", plain, linked);

        screen.setCharacter(0, 0, linked);
        screen.refresh();

        TextCharacter read = screen.getFrontCharacter(0, 0);
        assertEquals("https://test.com", read.getHyperlinkUrl());
        screen.stopScreen();
    }

    @Test
    public void refreshDoesNotThrowWithProtocolFields() throws IOException {
        DefaultVirtualTerminal term = new DefaultVirtualTerminal(new TerminalSize(10, 3));
        TerminalScreen screen = new TerminalScreen(term);
        screen.startScreen();

        // Fill screen with various protocol-field-carrying characters
        screen.setCharacter(0, 0, new TextCharacter('A', TextColor.ANSI.RED, TextColor.ANSI.DEFAULT)
            .withHyperlink("https://a.com"));
        screen.setCharacter(1, 0, new TextCharacter('B', TextColor.ANSI.GREEN, TextColor.ANSI.DEFAULT)
            .withPromptMarker(com.googlecode.lanterna.PromptMarker.OUTPUT_START));
        screen.setCharacter(0, 1, new TextCharacter('C', TextColor.ANSI.BLUE, TextColor.ANSI.DEFAULT)
            .withHyperlink("https://c.com"));

        // Should not throw
        screen.refresh();
        screen.refresh();  // second refresh should also work
        screen.stopScreen();
    }

    @Test
    public void clearingHyperlinkWorks() throws IOException {
        DefaultVirtualTerminal term = new DefaultVirtualTerminal(new TerminalSize(20, 5));
        TerminalScreen screen = new TerminalScreen(term);
        screen.startScreen();

        TextCharacter linked = new TextCharacter('X', TextColor.ANSI.WHITE, TextColor.ANSI.DEFAULT)
            .withHyperlink("https://example.com");
        screen.setCharacter(0, 0, linked);
        screen.refresh();

        // Clear the hyperlink
        TextCharacter unlinked = linked.withHyperlink(null);
        screen.setCharacter(0, 0, unlinked);
        screen.refresh();

        TextCharacter read = screen.getFrontCharacter(0, 0);
        assertNull(read.getHyperlinkUrl());
        screen.stopScreen();
    }
}
