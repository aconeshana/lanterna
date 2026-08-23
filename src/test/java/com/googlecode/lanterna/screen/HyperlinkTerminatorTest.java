package com.googlecode.lanterna.screen;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

/**
 * Guards the exact bytes of the OSC 8 hyperlink terminators.
 * <p>
 * Which terminator {@link TerminalScreen} picks is decided in a static initialiser from the
 * environment, so the alternative cannot be exercised through the public API from a test.
 * The constants are read reflectively instead — reflection also sidesteps javac inlining
 * compile-time String constants into this test class.
 * <p>
 * This exists because the ST form once carried one backslash too many
 * ({@code ESC \\ \\} instead of {@code ESC \\}), which leaks a stray backslash into the
 * output of every Kitty session; nothing in the rendering tests looked at the bytes.
 */
public class HyperlinkTerminatorTest {

    private static final char ESC = (char) 0x1B;
    private static final char BEL = (char) 0x07;

    private static String constant(String name) throws Exception {
        Field field = TerminalScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    @Test
    public void stTerminatorIsEscapeFollowedByExactlyOneBackslash() throws Exception {
        assertEquals(ESC + "\\", constant("HYPERLINK_OPEN_SUFFIX_ST"));
        assertEquals(ESC + "]8;;" + ESC + "\\", constant("HYPERLINK_CLOSE_ST"));
    }

    @Test
    public void belTerminatorIsASingleBellCharacter() throws Exception {
        assertEquals(String.valueOf(BEL), constant("HYPERLINK_OPEN_SUFFIX_BEL"));
        assertEquals(ESC + "]8;;" + BEL, constant("HYPERLINK_CLOSE_BEL"));
    }

    @Test
    public void openPrefixIsTheStandardOsc8Introducer() throws Exception {
        assertEquals(ESC + "]8;;", constant("HYPERLINK_OPEN_PREFIX"));
    }
}
