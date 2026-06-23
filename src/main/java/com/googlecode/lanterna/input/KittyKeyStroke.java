/*
 * This file is part of lanterna (https://github.com/mabe02/lanterna).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2024 Martin Berglund
 */
package com.googlecode.lanterna.input;

/**
 * {@link KeyStroke} carrying a Kitty keyboard protocol key event.
 * <p>
 * The Kitty protocol sends {@code ESC [ keycode ; modified-keycode u} for
 * every key press, including modifier-only keys (Shift, Ctrl, Alt alone)
 * and keys that don't produce characters. This class exposes both the raw
 * {@code keycode} (Linux input event code) and the decoded modifier flags.
 *
 * @see KittyKeyPattern
 */
public class KittyKeyStroke extends KeyStroke {

    private final int keyCode;
    private final int modifiedCode;

    /**
     * @param keyCode      the Kitty keycode (Linux input event code or Unicode codepoint)
     * @param modifiedCode the modified-keycode encoding modifier flags in bits 0-2
     * @param character    the decoded character, or null for non-character keys
     * @param ctrlDown     whether Ctrl was held
     * @param altDown      whether Alt was held
     * @param shiftDown    whether Shift was held
     */
    public KittyKeyStroke(int keyCode, int modifiedCode, Character character,
                          boolean ctrlDown, boolean altDown, boolean shiftDown) {
        super(character != null ? character : ' ',
              ctrlDown, altDown, shiftDown);
        this.keyCode = keyCode;
        this.modifiedCode = modifiedCode;
    }

    /** The raw Kitty keycode (Linux input event code). */
    public int getKeyCode() { return keyCode; }

    /** The modified-keycode value carrying modifier flags. */
    public int getModifiedCode() { return modifiedCode; }

    @Override
    public String toString() {
        return "KittyKeyStroke{keycode=" + keyCode
            + ", modified=" + modifiedCode
            + ", char=" + getCharacter()
            + ", ctrl=" + isCtrlDown()
            + ", alt=" + isAltDown()
            + ", shift=" + isShiftDown() + '}';
    }
}
