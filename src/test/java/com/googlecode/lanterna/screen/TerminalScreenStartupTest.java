/*
 * This file is part of lanterna (https://github.com/mabe02/lanterna).
 */
package com.googlecode.lanterna.screen;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.terminal.PrivateModeTerminal;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TerminalScreenStartupTest {

    // String.repeat is Java 11; this module targets 8 (see maven.compiler.release in pom.xml).
    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, c);
        return new String(chars);
    }

    @Test
    public void initialStartCanReuseTheSizeAlreadyCapturedByTheConstructor() throws IOException {
        CountingTerminal terminal = new CountingTerminal();
        TerminalScreen screen = new TerminalScreen(terminal);
        assertEquals(1, terminal.sizeQueries);

        screen.startScreenWithoutTerminalSizeQuery();

        assertEquals("initial screen startup must not repeat a blocking size query",
                1, terminal.sizeQueries);
        screen.stopScreen(false);
    }

    @Test
    public void fullRefreshCoalescesAdjacentCharactersIntoTerminalWrites() throws IOException {
        CountingStringTerminal terminal = new CountingStringTerminal();
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreenWithoutTerminalSizeQuery();
        terminal.putStringCalls = 0;
        screen.newTextGraphics().putString(0, 0, repeat('x', 60));

        screen.refresh(Screen.RefreshType.COMPLETE);

        assertTrue("expected adjacent cells to be emitted as a text run", terminal.putStringCalls <= 2);
        screen.stopScreen(false);
    }

    @Test
    public void failedPrivateModeEntryLeavesTheScreenRestartable() throws IOException {
        // The flag used to be latched before entering private mode, which made a single
        // failure permanent: startScreen() returns early on a started screen, so a screen
        // holding no alternate buffer could never be put back onto one, and the renderer
        // went on painting absolute rows into a main buffer that scrolls.
        RefusingTerminal terminal = new RefusingTerminal();
        TerminalScreen screen = new TerminalScreen(terminal);

        try {
            screen.startScreen();
            fail("expected the terminal's private-mode failure to propagate");
        } catch (UncheckedIOException expected) {
            // The caller is told, which is the point: it is free to retry.
        }

        terminal.refuse = false;
        screen.startScreen();

        assertEquals("a screen whose first start failed must still be startable",
                1, terminal.privateModeEntries);
        screen.stopScreen(false);
    }

    @Test
    public void reassertingPrivateModeRepairsAStartedScreenThatStartScreenWouldIgnore()
            throws IOException {
        RecordingPrivateModeTerminal terminal = new RecordingPrivateModeTerminal();
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        assertEquals(1, terminal.privateModeEntries);

        // Something else had the terminal and handed it back outside private mode. The screen
        // still believes it is started, so startScreen() is a no-op and cannot help.
        screen.startScreen();
        assertEquals("startScreen must stay a no-op on a started screen",
                1, terminal.privateModeEntries);

        screen.reassertPrivateMode();

        assertEquals("the repair has to re-assert regardless of what the screen believes",
                1, terminal.reasserts);
        screen.stopScreen(false);
    }

    @Test
    public void reassertingPrivateModeOnAScreenThatWasNeverStartedDoesNothing() throws IOException {
        RecordingPrivateModeTerminal terminal = new RecordingPrivateModeTerminal();
        TerminalScreen screen = new TerminalScreen(terminal);

        screen.reassertPrivateMode();

        assertEquals("nothing was taken from us, so there is nothing to take back",
                0, terminal.reasserts);
    }

    @Test
    public void reassertingPrivateModeForcesTheNextRefreshToRepaintEverything()
            throws IOException {
        RecordingPrivateModeTerminal terminal = new RecordingPrivateModeTerminal();
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        screen.refresh(Screen.RefreshType.COMPLETE);
        terminal.clearScreenCalls = 0;

        screen.reassertPrivateMode();
        // AUTOMATIC would otherwise pick a delta against a front buffer that describes an
        // alternate screen which no longer exists.
        screen.refresh(Screen.RefreshType.AUTOMATIC);

        assertEquals("the recovered alternate screen has undefined contents",
                1, terminal.clearScreenCalls);
        screen.stopScreen(false);
    }

    @Test
    public void reassertingPrivateModeIsSilentOnATerminalWithoutTheCapability()
            throws IOException {
        // DefaultVirtualTerminal has no notion of private mode to re-assert.
        TerminalScreen screen = new TerminalScreen(new DefaultVirtualTerminal());
        screen.startScreen();

        screen.reassertPrivateMode();

        screen.stopScreen(false);
    }

    /** Fails private-mode entry on demand, so the screen's own bookkeeping can be examined. */
    private static final class RefusingTerminal extends DefaultVirtualTerminal {
        private boolean refuse = true;
        private int privateModeEntries;

        @Override
        public synchronized void enterPrivateMode() {
            if (refuse) {
                throw new UncheckedIOException(new IOException("terminal refused private mode"));
            }
            privateModeEntries++;
            super.enterPrivateMode();
        }
    }

    /** A terminal that reports and re-asserts private mode, counting both transitions. */
    private static final class RecordingPrivateModeTerminal extends DefaultVirtualTerminal
            implements PrivateModeTerminal {
        private int privateModeEntries;
        private int reasserts;
        private int clearScreenCalls;
        private boolean inPrivateMode;

        @Override
        public synchronized void enterPrivateMode() {
            privateModeEntries++;
            inPrivateMode = true;
            super.enterPrivateMode();
        }

        @Override
        public synchronized void exitPrivateMode() {
            inPrivateMode = false;
            super.exitPrivateMode();
        }

        @Override
        public synchronized void clearScreen() {
            clearScreenCalls++;
            super.clearScreen();
        }

        @Override
        public boolean isInPrivateMode() {
            return inPrivateMode;
        }

        @Override
        public synchronized void reassertPrivateMode() {
            reasserts++;
            inPrivateMode = true;
        }
    }

    private static final class CountingTerminal extends DefaultVirtualTerminal {
        private int sizeQueries;

        @Override
        public synchronized TerminalSize getTerminalSize() {
            sizeQueries++;
            return super.getTerminalSize();
        }
    }

    private static final class CountingStringTerminal extends DefaultVirtualTerminal {
        private int putStringCalls;

        @Override
        public synchronized void putString(String string) {
            putStringCalls++;
            super.putString(string);
        }
    }
}
