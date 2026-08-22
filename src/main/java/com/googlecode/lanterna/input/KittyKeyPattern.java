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

import java.util.List;

/**
 * Parses Kitty keyboard protocol CSI events ({@code ESC [ code ; mods u}).
 * <p>
 * When the Kitty keyboard protocol is enabled (via {@code ESC [ > 1 u}),
 * the terminal sends key events in a uniform format:
 * <pre>
 *   ESC [ keycode ; modified-keycode u
 * </pre>
 * where {@code keycode} is a Linux input-event code and {@code modified-keycode}
 * encodes both the key and the active modifiers (ctrl/alt/shift/etc.).
 * <p>
 * This pattern also matches the xterm {@code modifyOtherKeys} format which
 * uses the same {@code ESC [ code ; mods u} encoding.
 * <p>
 * The pattern emits {@link KittyKeyStroke} instances carrying the parsed
 * keycode and modifier flags.
 *
 * @see KittyKeyStroke
 */
public class KittyKeyPattern implements CharacterPattern {

    private static final char ESC = '';

    @Override
    public Matching match(List<Character> seq) {
        if (seq == null || seq.isEmpty()) return null;
        // Must start with ESC [
        if (seq.get(0) != ESC) return null;
        if (seq.size() < 2) return Matching.NOT_YET;
        if (seq.get(1) != '[') return null;

        // Scan for the 'u' terminator
        int end = -1;
        for (int i = 2; i < seq.size(); i++) {
            if (seq.get(i) == 'u') {
                end = i;
                break;
            }
            // Only allow digits and semicolons in the payload
            char c = seq.get(i);
            if (!(Character.isDigit(c) || c == ';')) {
                return null; // not a kitty key sequence
            }
        }
        if (end < 0) {
            // No terminator yet — partial match (waiting for 'u')
            // But limit buffer to avoid unbounded growth
            if (seq.size() > 32) return null;
            return Matching.NOT_YET;
        }

        // Parse the payload between [ and u
        StringBuilder payload = new StringBuilder();
        for (int i = 2; i < end; i++) {
            payload.append(seq.get(i));
        }
        String[] parts = payload.toString().split(";");
        int keyCode = 0;
        int modifiedCode = 0;
        try {
            if (parts.length >= 1 && !parts[0].isEmpty()) {
                keyCode = Integer.parseInt(parts[0]);
            }
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                modifiedCode = Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException e) {
            return null; // malformed
        }

        // Extract modifiers from the modified code.
        // CSI-u / Kitty encoding: the transmitted value is `1 + bit_flags`,
        // where bit_flags is: bit 0 = shift, bit 1 = alt, bit 2 = control
        // (higher bits encode super/hyper/meta/caps-lock/num-lock). A value
        // of 1 means "no modifier". Subtract 1 before masking to recover the
        // actual modifier bits — without this, ctrl-only (transmitted as 5)
        // reads back as shift+ctrl (5 & 1 = 1). Absent field (parts.length<2)
        // defaults to 1 (no modifier).
        int mods = Math.max(0, (parts.length >= 2 ? modifiedCode : 1) - 1);
        boolean shift = (mods & 1) != 0;
        boolean alt   = (mods & 2) != 0;
        boolean ctrl  = (mods & 4) != 0;

        // Map control-character keycodes to standard KeyType values so that
        // callers can use key.getKeyType() == KeyType.ESCAPE etc. normally.
        // Without this, keyCode=27 would be constructed as CHARACTER(' ') by
        // KittyKeyStroke (because kittyKeyCodeToChar(27) returns null and the
        // fallback is ' '), which breaks every ESCAPE check in handleInput().
        switch (keyCode) {
            case 27:  // ESC
                return new Matching(new KeyStroke(KeyType.ESCAPE, ctrl, alt));
            case 13:  // CR → Enter
                return new Matching(new KeyStroke(KeyType.ENTER, ctrl, alt));
            case 9:   // HT → Tab / Shift+Tab
                return new Matching(new KeyStroke(shift ? KeyType.REVERSE_TAB : KeyType.TAB, ctrl, alt));
            case 127: // DEL → Backspace
            case 8:   // BS  → Backspace
                return new Matching(new KeyStroke(KeyType.BACKSPACE, ctrl, alt));
        }

        // Convert Kitty keycode to a character if possible
        Character ch = kittyKeyCodeToChar(keyCode, shift);

        return new Matching(new KittyKeyStroke(keyCode, modifiedCode, ch, ctrl, alt, shift));
    }

    /**
     * Convert a Kitty keycode (Linux input event code) to a Java Character.
     * Returns null for non-character keys (arrows, function keys, etc.).
     */
    private static Character kittyKeyCodeToChar(int keyCode, boolean shift) {
        // Kitty uses Unicode codepoints for character keys
        // (keycode = the Unicode codepoint of the unmodified key)
        if (keyCode >= 32 && keyCode <= 0x10FFFF) {
            // Filter out control characters
            char c = (char) keyCode;
            if (Character.isISOControl(c)) return null;
            return c;
        }
        return null;
    }
}
