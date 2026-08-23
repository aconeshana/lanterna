/*
 * This file is part of lanterna (https://github.com/mabe02/lanterna).
 */
package com.googlecode.lanterna.screen;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
