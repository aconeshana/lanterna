package com.googlecode.lanterna.input;

/**
 * {@link KeyStroke} carrying a DEC 1004 focus event. Emitted by
 * {@link FocusEventPattern} when the terminal reports focus gain/loss.
 *
 * @see FocusEventPattern
 */
public class FocusEventKeyStroke extends KeyStroke {

    private final boolean focused;

    /**
     * @param focused {@code true} if the window gained focus, {@code false} if lost
     */
    public FocusEventKeyStroke(boolean focused) {
        super(KeyType.FOCUS_EVENT);
        this.focused = focused;
    }

    /** {@code true} if the window gained focus, {@code false} if lost. */
    public boolean isFocused() { return focused; }

    @Override
    public String toString() {
        return "FocusEventKeyStroke{focused=" + focused + '}';
    }
}
