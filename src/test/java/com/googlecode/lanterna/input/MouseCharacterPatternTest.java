package com.googlecode.lanterna.input;

import com.googlecode.lanterna.TerminalPosition;
import org.junit.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MouseCharacterPatternTest {

    private static final char ESC = '';

    @Test
    public void decodesClickReleaseDragMoveAndWheelWithoutLeakingCharacters() throws Exception {
        List<ExpectedMouse> expected = Arrays.asList(
            new ExpectedMouse(ESC + "[<0;112;22M", MouseActionType.CLICK_DOWN, 111, 21),
            new ExpectedMouse(ESC + "[<32;113;23M", MouseActionType.DRAG, 112, 22),
            new ExpectedMouse(ESC + "[<0;113;23m", MouseActionType.CLICK_RELEASE, 112, 22),
            new ExpectedMouse(ESC + "[<35;114;24M", MouseActionType.MOVE, 113, 23),
            new ExpectedMouse(ESC + "[<64;112;22M", MouseActionType.SCROLL_UP, 111, 21),
            new ExpectedMouse(ESC + "[<65;112;22M", MouseActionType.SCROLL_DOWN, 111, 21));

        StringBuilder input = new StringBuilder();
        for (ExpectedMouse item : expected) {
            input.append(item.sequence);
        }
        InputDecoder decoder = decoderFor(input.toString());

        for (ExpectedMouse item : expected) {
            KeyStroke keyStroke = decoder.getNextCharacter(true);
            assertTrue(item.sequence, keyStroke instanceof MouseAction);
            MouseAction action = (MouseAction) keyStroke;
            assertEquals(item.sequence, item.actionType, action.getActionType());
            assertEquals(item.sequence, new TerminalPosition(item.column, item.row), action.getPosition());
        }
        assertEquals(KeyType.EOF, decoder.getNextCharacter(true).getKeyType());
    }

    @Test
    public void decodesClickButtonsPerMouseActionConvention() throws Exception {
        // xterm SGR encodes button in Cb & 0x3: 0=left, 1=middle, 2=right. MouseAction's
        // own convention (see its Javadoc) is left=1, middle=2, right=3 — a different
        // numbering that the decoder must translate, not pass through verbatim.
        InputDecoder decoder = decoderFor(
            ESC + "[<0;5;3M" + ESC + "[<1;5;3M" + ESC + "[<2;5;3M");

        assertEquals(1, ((MouseAction) decoder.getNextCharacter(true)).getButton());
        assertEquals(2, ((MouseAction) decoder.getNextCharacter(true)).getButton());
        assertEquals(3, ((MouseAction) decoder.getNextCharacter(true)).getButton());
        assertEquals(KeyType.EOF, decoder.getNextCharacter(true).getKeyType());
    }

    @Test
    public void decodesHorizontalWheelAsHorizontalScroll() throws Exception {
        // SGR wheel reports are 64 + direction (0=up, 1=down, 2=left, 3=right).
        // Left/right must not be reported as SCROLL_UP / SCROLL_DOWN, or a sideways
        // trackpad swipe scrolls the viewport vertically.
        List<ExpectedMouse> expected = Arrays.asList(
            new ExpectedMouse(ESC + "[<66;10;5M", MouseActionType.SCROLL_LEFT, 9, 4),
            new ExpectedMouse(ESC + "[<67;10;5M", MouseActionType.SCROLL_RIGHT, 9, 4));

        StringBuilder input = new StringBuilder();
        for (ExpectedMouse item : expected) {
            input.append(item.sequence);
        }
        InputDecoder decoder = decoderFor(input.toString());

        int[] expectedButtons = { 6, 7 };
        for (int i = 0; i < expected.size(); i++) {
            ExpectedMouse item = expected.get(i);
            KeyStroke keyStroke = decoder.getNextCharacter(true);
            assertTrue(item.sequence, keyStroke instanceof MouseAction);
            MouseAction action = (MouseAction) keyStroke;
            assertEquals(item.sequence, item.actionType, action.getActionType());
            assertEquals(item.sequence, expectedButtons[i], action.getButton());
            assertEquals(item.sequence, new TerminalPosition(item.column, item.row), action.getPosition());
        }
        assertEquals(KeyType.EOF, decoder.getNextCharacter(true).getKeyType());
    }

    @Test
    public void scrollEventsAreNeverCoercedIntoMoveOrDrag() throws Exception {
        // A click-down leaves isMouseDown set; the wheel report that follows must not
        // be rewritten into DRAG by the MOVE/DRAG coercion further down the pattern.
        InputDecoder decoder = decoderFor(ESC + "[<0;5;5M" + ESC + "[<64;5;5M");

        assertEquals(MouseActionType.CLICK_DOWN,
                ((MouseAction) decoder.getNextCharacter(true)).getActionType());
        assertEquals(MouseActionType.SCROLL_UP,
                ((MouseAction) decoder.getNextCharacter(true)).getActionType());
        assertEquals(KeyType.EOF, decoder.getNextCharacter(true).getKeyType());
    }

    @Test
    public void decodesSgrMouseCoordinatesBeyondTheOldFifteenCharacterLimit() throws Exception {
        InputDecoder decoder = decoderFor(ESC + "[<65;1234;5678M");

        KeyStroke keyStroke = decoder.getNextCharacter(true);
        assertTrue(keyStroke instanceof MouseAction);
        MouseAction action = (MouseAction) keyStroke;

        assertEquals(MouseActionType.SCROLL_DOWN, action.getActionType());
        assertEquals(new TerminalPosition(1233, 5677), action.getPosition());
        assertEquals(KeyType.EOF, decoder.getNextCharacter(true).getKeyType());
    }

    private static InputDecoder decoderFor(String input) {
        InputDecoder decoder = new InputDecoder(new StringReader(input));
        decoder.addProfile(new DefaultKeyDecodingProfile());
        decoder.setTimeoutUnits(0);
        return decoder;
    }

    private static final class ExpectedMouse {
        private final String sequence;
        private final MouseActionType actionType;
        private final int column;
        private final int row;

        private ExpectedMouse(String sequence, MouseActionType actionType, int column, int row) {
            this.sequence = sequence;
            this.actionType = actionType;
            this.column = column;
            this.row = row;
        }
    }
}
