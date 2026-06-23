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
 * Specialised {@link KeyStroke} carrying the pasted text delivered by a terminal
 * bracketed-paste sequence ({@code ESC [ 200 ~} ... {@code ESC [ 201 ~}).
 * <p>
 * The {@code KeyType} is {@link KeyType#PASTE}; the payload is the verbatim
 * string between the start and end markers (no transformation, no trimming).
 * Multi-line pastes preserve their embedded newlines.
 *
 * @see BracketedPastePattern
 */
public class PasteKeyStroke extends KeyStroke {

    private final String pastedText;

    /**
     * @param pastedText verbatim text between the bracketed-paste markers
     */
    public PasteKeyStroke(String pastedText) {
        super(KeyType.PASTE);
        this.pastedText = pastedText == null ? "" : pastedText;
    }

    /** The verbatim pasted text. Never null. */
    public String getPastedText() {
        return pastedText;
    }

    @Override
    public String toString() {
        return "PasteKeyStroke{length=" + pastedText.length()
                + ", preview=" + preview(pastedText, 40) + '}';
    }

    private static String preview(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
