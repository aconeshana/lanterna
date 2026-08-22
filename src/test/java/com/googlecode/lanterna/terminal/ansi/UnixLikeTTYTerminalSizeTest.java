/*
 * This file is part of lanterna (https://github.com/mabe02/lanterna).
 */
package com.googlecode.lanterna.terminal.ansi;

import com.googlecode.lanterna.TerminalSize;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UnixLikeTTYTerminalSizeTest {

    @Test
    public void parsesSttyRowsAndColumnsWithoutAnAnsiCursorQuery() {
        assertEquals(new TerminalSize(132, 43),
                UnixLikeTTYTerminal.parseSTTYSize("43 132"));
        assertEquals(new TerminalSize(80, 24),
                UnixLikeTTYTerminal.parseSTTYSize("  24   80  \n"));
    }

    @Test
    public void rejectsMalformedOrNonPositiveSttySizes() {
        assertNull(UnixLikeTTYTerminal.parseSTTYSize(null));
        assertNull(UnixLikeTTYTerminal.parseSTTYSize(""));
        assertNull(UnixLikeTTYTerminal.parseSTTYSize("24"));
        assertNull(UnixLikeTTYTerminal.parseSTTYSize("rows columns"));
        assertNull(UnixLikeTTYTerminal.parseSTTYSize("0 80"));
        assertNull(UnixLikeTTYTerminal.parseSTTYSize("24 -1"));
    }
}
