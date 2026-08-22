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
 * Matches an ESC prefix followed by CR (0x0d) or LF (0x0a). Some terminals
 * (notably Apple Terminal via the {@code Option as Meta} setting, and any
 * terminal where the user has manually mapped Shift+Return to
 * {@code Send Escape Sequence: \r}) emit this two-byte pair to signal
 * "modified Enter". {@link AltAndCharacterPattern} deliberately rejects
 * ISO-control second bytes, so without this pattern the sequence would
 * split into two separate {@link KeyType#ESCAPE} + {@link KeyType#ENTER}
 * key strokes, forcing every consumer to reassemble them.
 * <p>
 * We surface it as {@code KeyStroke(ENTER, ctrl=false, alt=true)} — the
 * xterm convention where {@code ESC + <byte>} means "Alt + byte". Callers
 * that want to treat this as Shift+Enter (multi-line input) can check
 * {@link KeyStroke#isAltDown()} on an ENTER key.
 */
public class EscapeEnterPattern implements CharacterPattern {

    private static final char ESC = '';

    @Override
    public Matching match(List<Character> seq) {
        if (seq == null || seq.isEmpty()) return null;
        if (seq.get(0) != ESC) return null;
        if (seq.size() == 1) return Matching.NOT_YET;
        char second = seq.get(1);
        if (second != '\r' && second != '\n') return null;
        if (seq.size() > 2) return null;
        // KeyStroke(KeyType, ctrl, alt) — no shift field on ENTER, but the
        // alt bit is enough for InputPanel to distinguish Shift+Enter from
        // plain Enter.
        return new Matching(new KeyStroke(KeyType.ENTER, false, true));
    }
}
