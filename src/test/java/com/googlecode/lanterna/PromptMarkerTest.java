package com.googlecode.lanterna;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Verifies {@link PromptMarker} escape sequences match the OSC 133 spec.
 */
public class PromptMarkerTest {

    @Test
    public void promptStartEmitsCorrectCode() {
        assertEquals("A", PromptMarker.PROMPT_START.code());
        assertEquals("\u001B]133;A\u001B\\", PromptMarker.PROMPT_START.escapeSequence());
    }

    @Test
    public void commandStartEmitsCorrectCode() {
        assertEquals("B", PromptMarker.COMMAND_START.code());
        assertEquals("\u001B]133;B\u001B\\", PromptMarker.COMMAND_START.escapeSequence());
    }

    @Test
    public void outputStartEmitsCorrectCode() {
        assertEquals("C", PromptMarker.OUTPUT_START.code());
        assertEquals("\u001B]133;C\u001B\\", PromptMarker.OUTPUT_START.escapeSequence());
    }

    @Test
    public void outputEndEmitsCorrectCode() {
        assertEquals("D", PromptMarker.OUTPUT_END.code());
        assertEquals("\u001B]133;D\u001B\\", PromptMarker.OUTPUT_END.escapeSequence());
    }

    @Test
    public void allMarkersStartWithEscBracket() {
        for (PromptMarker m : PromptMarker.values()) {
            String esc = m.escapeSequence();
            assertTrue("Marker " + m + " should start with ESC ]",
                esc.startsWith("\u001B]133;"));
            assertTrue("Marker " + m + " should end with ESC \\",
                esc.endsWith("\u001B\\"));
        }
    }
}
