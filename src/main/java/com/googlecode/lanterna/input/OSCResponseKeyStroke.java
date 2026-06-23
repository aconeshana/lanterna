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
 * {@link KeyStroke} carrying an unsolicited OSC (Operating System Command)
 * response from the terminal, e.g. the reply to an {@code OSC 11} background
 * color query or an {@code OSC 52} clipboard read.
 * <p>
 * The {@code code} field is the numeric OSC code (e.g., {@code "11"} or
 * {@code "52"}); the {@code payload} is everything after the first semicolon
 * up to the String Terminator.
 *
 * @see OSCResponsePattern
 */
public class OSCResponseKeyStroke extends KeyStroke {

    private final String code;
    private final String payload;

    /**
     * @param code    the numeric OSC code (e.g. {@code "11"} for bg color)
     * @param payload text after the first semicolon, before the terminator
     */
    public OSCResponseKeyStroke(String code, String payload) {
        super(KeyType.OSC_RESPONSE);
        this.code = code == null ? "" : code;
        this.payload = payload == null ? "" : payload;
    }

    /** The OSC numeric code (e.g. {@code "11"}, {@code "52"}). */
    public String code() { return code; }

    /** The OSC payload text (everything after the first semicolon). */
    public String payload() { return payload; }

    @Override
    public String toString() {
        return "OSCResponseKeyStroke{code=" + code + ", payload=" + payload + '}';
    }
}
