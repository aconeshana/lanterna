/*
 * This file is part of lanterna (https://github.com/mabe02/lanterna).
 */
package com.googlecode.lanterna.terminal.ansi;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pins the tty configuration raw mode installs. These flags are what separates an
 * application that reads its own keystrokes from one the kernel keeps intercepting,
 * so a regression here is silent: the program still runs, it just never sees the key.
 */
public class UnixLikeTTYTerminalRawModeTest {

    private static final class RecordingTerminal extends UnixLikeTTYTerminal {
        // Not a field initialiser: the super constructor acquires the terminal, so
        // runSTTYCommand is already being called before this class's own fields exist.
        private List<String> commands;

        RecordingTerminal() throws IOException {
            super(null,
                    new ByteArrayInputStream(new byte[0]),
                    new ByteArrayOutputStream(),
                    StandardCharsets.UTF_8,
                    CtrlCBehaviour.TRAP);
        }

        @Override
        protected String runSTTYCommand(String... parameters) {
            if (commands == null) {
                commands = new ArrayList<>();
            }
            commands.add(String.join(" ", parameters));
            return "";
        }

        @Override
        protected void registerTerminalResizeListener(Runnable onResize) {
            // No signal handling in a unit test.
        }

        List<String> drain() {
            if (commands == null) {
                return Collections.emptyList();
            }
            List<String> drained = new ArrayList<>(commands);
            commands.clear();
            return drained;
        }
    }

    @Test
    public void rawModeClearsEveryInputFlagThatWouldRewriteOrSwallowAKeystroke() throws IOException {
        RecordingTerminal terminal = new RecordingTerminal();
        terminal.drain();

        terminal.canonicalMode(false);

        List<String> issued = terminal.drain();
        assertTrue("line buffering: " + issued, issued.contains("-icanon"));
        assertTrue("CR to LF translation: " + issued, issued.contains("-icrnl"));
        assertTrue("LF to CR translation: " + issued, issued.contains("-inlcr"));
        assertTrue("flow control eats ctrl+s/ctrl+q: " + issued, issued.contains("-ixon"));
        assertTrue("VLNEXT eats ctrl+v: " + issued, issued.contains("-iexten"));
        assertTrue("break synthesises an interrupt: " + issued, issued.contains("-brkint"));
    }

    @Test
    public void leavingRawModeOnlyRestoresLineBuffering() throws IOException {
        // The rest comes back through the "stty -g" snapshot, which holds the user's
        // own settings rather than the ones we happened to switch off.
        RecordingTerminal terminal = new RecordingTerminal();
        terminal.drain();

        terminal.canonicalMode(true);

        assertEquals(Collections.singletonList("icanon"), terminal.drain());
    }

    @Test
    public void signalsAreDisabledByClearingIsigRatherThanBlankingControlCharacters() throws IOException {
        RecordingTerminal terminal = new RecordingTerminal();
        terminal.drain();

        terminal.keyStrokeSignalsEnabled(false);
        assertEquals(Collections.singletonList("-isig"), terminal.drain());

        terminal.keyStrokeSignalsEnabled(true);
        assertEquals(Collections.singletonList("isig"), terminal.drain());
    }

    @Test
    public void takingTheTerminalBackReappliesRawModeWithoutResavingTheUsersSettings() throws IOException {
        RecordingTerminal terminal = new RecordingTerminal();
        terminal.drain();

        terminal.reapplyTerminalSettings();

        List<String> issued = terminal.drain();
        assertTrue("raw mode: " + issued, issued.contains("-icanon"));
        assertTrue("echo: " + issued, issued.contains("-echo"));
        assertTrue("signals: " + issued, issued.contains("-isig"));
        assertFalse("must not overwrite the restore snapshot: " + issued, issued.contains("-g"));
    }
}
