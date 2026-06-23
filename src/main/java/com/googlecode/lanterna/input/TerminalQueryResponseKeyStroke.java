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
 * {@link KeyStroke} carrying a CSI query response from the terminal.
 * Used by the terminal querier to detect capabilities via DECRQM
 * (request mode status) and DA1 (primary device attributes).
 * <p>
 * DECRQM response: {@code ESC [ ? mode ; status $ y}
 * DA1 response: {@code ESC [ ? attr1 ; attr2 ; ... c}
 *
 * @see TerminalQueryResponsePattern
 */
public class TerminalQueryResponseKeyStroke extends KeyStroke {

    public enum QueryType { DECRQM, DA1, DA2, XTVERSION, UNKNOWN }

    private final QueryType queryType;
    private final int mode;      // DECRQM mode number (e.g., 2026 for synchronized output)
    private final int status;    // DECRQM status (0=not recognized, 1=set, 2=reset, 3=permanently set, 4=permanently reset)
    private final String payload; // DA1/DA2/XTVERSION raw payload

    /**
     * Construct a DECRQM response.
     */
    public TerminalQueryResponseKeyStroke(int mode, int status) {
        super(KeyType.TERMINAL_QUERY_RESPONSE);
        this.queryType = QueryType.DECRQM;
        this.mode = mode;
        this.status = status;
        this.payload = null;
    }

    /**
     * Construct a DA1/DA2/XTVERSION response.
     */
    public TerminalQueryResponseKeyStroke(QueryType type, String payload) {
        super(KeyType.TERMINAL_QUERY_RESPONSE);
        this.queryType = type;
        this.mode = -1;
        this.status = -1;
        this.payload = payload;
    }

    public QueryType queryType() { return queryType; }
    public int mode() { return mode; }
    public int status() { return status; }
    public String payload() { return payload; }

    @Override
    public String toString() {
        if (queryType == QueryType.DECRQM) {
            return "TerminalQueryResponse{DECRQM mode=" + mode + " status=" + status + '}';
        }
        return "TerminalQueryResponse{" + queryType + " payload=" + payload + '}';
    }
}
