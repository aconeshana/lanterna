package com.googlecode.lanterna;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Verifies {@link CursorStyle} enum values map to the correct DECSCUSR
 * parameter codes.
 */
public class CursorStyleTest {

    @Test
    public void defaultStyleIsZero() {
        assertEquals(0, CursorStyle.DEFAULT.code());
    }

    @Test
    public void blockStyles() {
        assertEquals(1, CursorStyle.BLINKING_BLOCK.code());
        assertEquals(2, CursorStyle.STEADY_BLOCK.code());
    }

    @Test
    public void underlineStyles() {
        assertEquals(3, CursorStyle.BLINKING_UNDERLINE.code());
        assertEquals(4, CursorStyle.STEADY_UNDERLINE.code());
    }

    @Test
    public void barStyles() {
        assertEquals(5, CursorStyle.BLINKING_BAR.code());
        assertEquals(6, CursorStyle.STEADY_BAR.code());
    }

    @Test
    public void allCodesInRange0to6() {
        for (CursorStyle cs : CursorStyle.values()) {
            assertTrue("Code " + cs.code() + " out of range",
                cs.code() >= 0 && cs.code() <= 6);
        }
    }
}
