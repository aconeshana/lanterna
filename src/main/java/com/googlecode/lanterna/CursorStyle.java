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
package com.googlecode.lanterna;

/**
 * Cursor shape styles for the {@code DECSCUSR} escape sequence
 * ({@code ESC [ N q}). Terminals that support this protocol (xterm,
 * iTerm2, Kitty, WezTerm, VTE, Ghostty) allow the application to choose
 * the cursor's visual shape, typically used to indicate the current
 * editing mode (block for NORMAL, bar for INSERT, underline for VISUAL).
 * <p>
 * The corresponding {@code N} values are:
 * <ul>
 *   <li>0 - default shape (terminal-defined)</li>
 *   <li>1 - blinking block</li>
 *   <li>2 - steady block</li>
 *   <li>3 - blinking underline</li>
 *   <li>4 - steady underline</li>
 *   <li>5 - blinking bar (xterm/iTerm2 extension)</li>
 *   <li>6 - steady bar (xterm/iTerm2 extension)</li>
 * </ul>
 *
 * @see Terminal#setCursorStyle(CursorStyle)
 */
public enum CursorStyle {
    /** Default cursor shape (terminal-defined). Emits {@code ESC [ 0 q}. */
    DEFAULT(0),
    /** Blinking block cursor. Emits {@code ESC [ 1 q}. */
    BLINKING_BLOCK(1),
    /** Steady (non-blinking) block cursor. Emits {@code ESC [ 2 q}. */
    STEADY_BLOCK(2),
    /** Blinking underline cursor. Emits {@code ESC [ 3 q}. */
    BLINKING_UNDERLINE(3),
    /** Steady (non-blinking) underline cursor. Emits {@code ESC [ 4 q}. */
    STEADY_UNDERLINE(4),
    /** Blinking bar (vertical line) cursor. Emits {@code ESC [ 5 q}. */
    BLINKING_BAR(5),
    /** Steady (non-blinking) bar cursor. Emits {@code ESC [ 6 q}. */
    STEADY_BAR(6);

    private final int code;

    CursorStyle(int code) {
        this.code = code;
    }

    /** The DECSCUSR parameter value (0-6) used in {@code ESC [ N q}. */
    public int code() {
        return code;
    }
}
