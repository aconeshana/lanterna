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
 * Detects unsolicited OSC (Operating System Command) responses in the input
 * stream — typically replies to {@code OSC 11} (background color query),
 * {@code OSC 10} (foreground color query), {@code OSC 52} (clipboard read),
 * or {@code OSC 4} (palette query).
 * <p>
 * Format: {@code ESC ] code ; payload ESC \} (ST terminator) or
 * {@code ESC ] code ; payload BEL} (BEL terminator, xterm convention).
 * <p>
 * The pattern extracts the numeric {@code code} (everything up to the first
 * semicolon) and the {@code payload} (everything after, before the terminator).
 * Both are exposed via {@link OSCResponseKeyStroke}.
 * <p>
 * Note: this pattern only matches OSC sequences with semicolons in the payload.
 * Other OSC sequences (e.g., the {@code ESC ] 8 ;; URL ESC \} hyperlink opener)
 * are emitted by the application and never appear on stdin, so they don't
 * conflict.
 */
public class OSCResponsePattern implements CharacterPattern {

    private static final char ESC = '\u001B';

    @Override
    public Matching match(List<Character> seq) {
        if (seq == null || seq.isEmpty()) return null;
        // Must start with ESC ]
        if (seq.get(0) != ESC) return null;
        if (seq.size() < 2) return Matching.NOT_YET;
        if (seq.get(1) != ']') return null;

        // Find the terminator: ESC \  or  BEL (0x07)
        int end = -1;
        boolean belTerminator = false;
        for (int i = 2; i < seq.size(); i++) {
            char c = seq.get(i);
            if (c == ESC && i + 1 < seq.size() && seq.get(i + 1) == '\\') {
                end = i;
                break;
            }
            if (c == '\u0007') {
                end = i;
                belTerminator = true;
                break;
            }
            // Embedded ESC not followed by '\' is malformed — bail out.
            if (c == ESC) return null;
        }
        if (end < 0) {
            // No terminator yet — but if the buffer is very long without one,
            // give up to avoid unbounded growth on malformed input.
            if (seq.size() > 4096) return null;
            return Matching.NOT_YET;
        }

        // Extract code (up to first ';') and payload (after first ';').
        int semicolon = -1;
        for (int i = 2; i < end; i++) {
            if (seq.get(i) == ';') { semicolon = i; break; }
        }
        String code;
        String payload;
        if (semicolon < 0) {
            code = charsToString(seq, 2, end);
            payload = "";
        } else {
            code = charsToString(seq, 2, semicolon);
            payload = charsToString(seq, semicolon + 1, end);
        }
        // Silence unused-variable warning in case the field is queried later
        if (belTerminator) {
            // Terminator was BEL — same payload semantics, no extra handling.
        }
        return new Matching(new OSCResponseKeyStroke(code, payload));
    }

    private static String charsToString(List<Character> seq, int from, int to) {
        StringBuilder sb = new StringBuilder(to - from);
        for (int i = from; i < to; i++) sb.append(seq.get(i));
        return sb.toString();
    }
}
