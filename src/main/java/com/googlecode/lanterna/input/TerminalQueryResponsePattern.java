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
 * Parses CSI query responses from the terminal:
 * <ul>
 *   <li>DECRPM: {@code ESC [ ? mode ; status $ y} — response to DECRQM
 *       (request DEC mode status, e.g., mode 2026 for synchronized output)</li>
 *   <li>DA1: {@code ESC [ ? attr1 ; attr2 ; ... c} — primary device attributes</li>
 *   <li>DA2: {@code ESC [ > attr1 ; attr2 ; attr3 c} — secondary device attributes</li>
 * </ul>
 * Emits {@link TerminalQueryResponseKeyStroke} instances carrying the parsed data.
 *
 * @see TerminalQueryResponseKeyStroke
 */
public class TerminalQueryResponsePattern implements CharacterPattern {

    private static final char ESC = '';

    @Override
    public Matching match(List<Character> seq) {
        if (seq == null || seq.isEmpty()) return null;
        // Must start with ESC [
        if (seq.get(0) != ESC) return null;
        if (seq.size() < 2) return Matching.NOT_YET;
        if (seq.get(1) != '[') return null;

        // Check for private mode marker (? or >) — query responses always have one
        if (seq.size() < 3) return Matching.NOT_YET;
        char privateMarker = seq.get(2);
        if (privateMarker != '?' && privateMarker != '>') return null;

        // Find terminator: 'y' for DECRPM, 'c' for DA1/DA2
        int end = -1;
        char terminator = 0;
        for (int i = 3; i < seq.size(); i++) {
            char c = seq.get(i);
            if (c == 'y' || c == 'c') {
                end = i;
                terminator = c;
                break;
            }
            // Only allow digits, semicolons, $, ?, > in the payload
            if (!(Character.isDigit(c) || c == ';' || c == '$' || c == '?' || c == '>')) {
                return null; // not a query response
            }
        }
        if (end < 0) {
            if (seq.size() > 64) return null; // too long, give up
            return Matching.NOT_YET;
        }

        // Extract payload between [ and terminator
        StringBuilder payload = new StringBuilder();
        int start = 3;
        for (int i = start; i < end; i++) {
            payload.append(seq.get(i));
        }
        String payloadStr = payload.toString();

        // DECRPM: ESC [ ? mode ; status $ y
        if (terminator == 'y' && privateMarker == '?') {
            // Format: mode ; status $ (the $ is before the y)
            // payload is like "2026;1$" or "2026;2$"
            String[] parts = payloadStr.split("[;$]");
            if (parts.length >= 2) {
                try {
                    int mode = Integer.parseInt(parts[0].trim());
                    int status = Integer.parseInt(parts[1].trim());
                    return new Matching(new TerminalQueryResponseKeyStroke(mode, status));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        // DA1: ESC [ ? attrs c
        if (terminator == 'c' && privateMarker == '?') {
            return new Matching(
                new TerminalQueryResponseKeyStroke(
                    TerminalQueryResponseKeyStroke.QueryType.DA1, payloadStr));
        }

        // DA2: ESC [ > attrs c
        if (terminator == 'c' && privateMarker == '>') {
            return new Matching(
                new TerminalQueryResponseKeyStroke(
                    TerminalQueryResponseKeyStroke.QueryType.DA2, payloadStr));
        }

        return null;
    }
}
