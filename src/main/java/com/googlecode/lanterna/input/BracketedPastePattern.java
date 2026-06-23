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
 * Detects xterm-style bracketed paste sequences ({@code ESC [ 200 ~ ... ESC [ 201 ~})
 * in the input stream and emits a {@link PasteKeyStroke} carrying the verbatim
 * pasted text. Requires the terminal to have bracketed-paste mode enabled
 * (the host sends {@code ESC [ ? 2004 h} before reading).
 * <p>
 * Matching rules:
 * <ul>
 *   <li>Sequence starts with the prefix {@code ESC [ 2 0 0 ~} (6 chars)</li>
 *   <li>After the prefix, scan for the suffix {@code ESC [ 2 0 1 ~} (6 chars)</li>
 *   <li>When both are present, return a full match with the text between them</li>
 *   <li>While waiting for the suffix, return {@code NOT_YET} so the
 *       {@code InputDecoder} keeps reading without timing out</li>
 *   <li>Sequences not starting with the prefix are not matched (return null)</li>
 * </ul>
 * <p>
 * The pattern is stateless — the {@code InputDecoder} re-invokes {@link #match(List)}
 * after every appended character, so we operate purely on the incoming sequence.
 */
public class BracketedPastePattern implements CharacterPattern {

    // ESC [ 2 0 0 ~  — start marker
    private static final char[] PREFIX = {'\u001B', '[', '2', '0', '0', '~'};
    // ESC [ 2 0 1 ~  — end marker
    private static final char[] SUFFIX = {'\u001B', '[', '2', '0', '1', '~'};

    @Override
    public Matching match(List<Character> seq) {
        if (seq == null || seq.isEmpty()) {
            return null;
        }
        // Reject early if the sequence can't possibly start with the prefix.
        if (!startsWithPrefix(seq)) {
            return null;
        }
        // We have at least the prefix; find the suffix.
        int suffixStart = findSuffix(seq);
        if (suffixStart < 0) {
            // Still waiting for the end marker — keep accumulating.
            return Matching.NOT_YET;
        }
        // Extract text between prefix and suffix.
        int textStart = PREFIX.length;
        int textEnd = suffixStart;
        StringBuilder sb = new StringBuilder(textEnd - textStart);
        for (int i = textStart; i < textEnd; i++) {
            sb.append(seq.get(i));
        }
        return new Matching(new PasteKeyStroke(sb.toString()));
    }

    /** True when {@code seq} starts with the 6-char bracketed-paste prefix. */
    private static boolean startsWithPrefix(List<Character> seq) {
        if (seq.size() < PREFIX.length) {
            // Could still become a prefix — check what we have so far.
            for (int i = 0; i < seq.size(); i++) {
                if (seq.get(i) != PREFIX[i]) return false;
            }
            return true;
        }
        for (int i = 0; i < PREFIX.length; i++) {
            if (seq.get(i) != PREFIX[i]) return false;
        }
        return true;
    }

    /**
     * Find the index of the suffix in {@code seq}, scanning from index
     * {@code PREFIX.length} onward. Returns -1 if not present.
     */
    private static int findSuffix(List<Character> seq) {
        int from = PREFIX.length;
        if (seq.size() - from < SUFFIX.length) {
            return -1;
        }
        outer:
        for (int i = from; i <= seq.size() - SUFFIX.length; i++) {
            for (int j = 0; j < SUFFIX.length; j++) {
                if (seq.get(i + j) != SUFFIX[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
