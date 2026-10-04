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
 * Parses the xterm {@code modifyOtherKeys} extended-key format:
 * <pre>
 *   ESC [ 27 ; modifier ; code ~
 * </pre>
 * xterm (and terminals that follow its lead, e.g. iTerm2) emits this
 * whenever {@code modifyOtherKeys} is set to format 2 — enabled via
 * {@code ESC [ > 4 ; 2 m}, see
 * {@link com.googlecode.lanterna.terminal.Terminal#enableKittyKeyboard()} —
 * for any modified key that has no conventional control-byte or CSI-letter
 * encoding of its own. Under format 2 this notably includes plain
 * {@code Ctrl+<letter>} combinations, not just the "ambiguous" ones format 1
 * is restricted to.
 * <p>
 * The leading {@code 27} is a fixed marker. It is unrelated to the Kitty
 * keyboard protocol's {@code CSI code ; mods u} form (handled by
 * {@link KittyKeyPattern}, which terminates on {@code u}) and to the
 * function-key {@code CSI num ~} form (handled by
 * {@link EscapeSequenceCharacterPattern}, which only carries two numeric
 * fields). Without a dedicated pattern for the three-field, tilde-terminated
 * {@code modifyOtherKeys} shape, these sequences fail every registered
 * pattern and fall through to {@link NormalCharacterPattern} one character
 * at a time — the modified key then reaches the application as its own
 * literal text (e.g. Ctrl+E typed into a text box as {@code "27;5;101~"},
 * with the leading {@code ESC [} misread as Alt+{@code [} along the way).
 *
 * @see KittyKeyPattern
 */
public class ModifyOtherKeysPattern implements CharacterPattern {

    private static final char ESC = '\u001b';

    @Override
    public Matching match(List<Character> seq) {
        if (seq == null || seq.isEmpty()) return null;
        if (seq.get(0) != ESC) return null;
        if (seq.size() < 2) return Matching.NOT_YET;
        if (seq.get(1) != '[') return null;

        // Scan for the '~' terminator; only digits and ';' may appear before it.
        int end = -1;
        for (int i = 2; i < seq.size(); i++) {
            char c = seq.get(i);
            if (c == '~') {
                end = i;
                break;
            }
            if (!(Character.isDigit(c) || c == ';')) {
                return null; // not a modifyOtherKeys sequence
            }
        }
        if (end < 0) {
            if (seq.size() > 32) return null; // runaway digit string; give up
            return Matching.NOT_YET;
        }

        StringBuilder payload = new StringBuilder();
        for (int i = 2; i < end; i++) {
            payload.append(seq.get(i));
        }
        String[] parts = payload.toString().split(";");
        // Exactly "27;modifier;code" — a bare function-key "num~" or
        // "num;mods~" belongs to EscapeSequenceCharacterPattern instead.
        if (parts.length != 3 || !"27".equals(parts[0])) {
            return null;
        }

        int modifiedCode;
        int code;
        try {
            modifiedCode = parts[1].isEmpty() ? 1 : Integer.parseInt(parts[1]);
            code = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return null; // malformed
        }

        // Same convention as KittyKeyPattern / EscapeSequenceCharacterPattern:
        // transmitted value is 1 + bit_flags (shift=1, alt=2, ctrl=4).
        int mods = Math.max(0, modifiedCode - 1);
        boolean shift = (mods & 1) != 0;
        boolean alt   = (mods & 2) != 0;
        boolean ctrl  = (mods & 4) != 0;

        // A handful of control codes have their own KeyType, mirroring
        // KittyKeyPattern's handling of the equivalent Kitty keycodes.
        switch (code) {
            case 27:  return new Matching(new KeyStroke(KeyType.ESCAPE, ctrl, alt));
            case 13:  return new Matching(new KeyStroke(KeyType.ENTER, ctrl, alt));
            case 9:   return new Matching(new KeyStroke(shift ? KeyType.REVERSE_TAB : KeyType.TAB, ctrl, alt));
            case 127:
            case 8:   return new Matching(new KeyStroke(KeyType.BACKSPACE, ctrl, alt));
            default:
                break;
        }

        if (code < 32 || code > 0x10FFFF || Character.isISOControl((char) code)) {
            return null; // unrecognised control code — let other input reach the app unmangled
        }
        return new Matching(new KeyStroke((char) code, ctrl, alt, shift));
    }
}
