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
 * Shell-integration prompt markers (OSC 133) attachable to a {@link TextCharacter}.
 * <p>
 * When the {@code Screen} refresh encounters a {@code TextCharacter} carrying one
 * of these markers, it emits the corresponding OSC 133 escape sequence before
 * the cell's character. Terminals that understand the protocol (iTerm2, Kitty,
 * WezTerm, Ghostty, latest macOS Terminal) use them to:
 * <ul>
 *   <li>delineate the prompt from user input and command output</li>
 *   <li>detect when a command has finished (for status-line integration)</li>
 *   <li>jump between prompts via Cmd+Up / Cmd+Down</li>
 *   <li>report command exit codes back to the terminal</li>
 * </ul>
 * <p>
 * The escape sequences are:
 * <pre>
 *   OSC 133;A ST  -- start of prompt
 *   OSC 133;B ST  -- end of prompt / start of user input
 *   OSC 133;C ST  -- start of command output
 *   OSC 133;D ST  -- end of command output (optionally followed by exit code)
 * </pre>
 * where {@code ST} is the String Terminator (ESC backslash).
 *
 * @see TextCharacter#withPromptMarker(PromptMarker)
 */
public enum PromptMarker {
    /** Marks the start of a prompt. Attach to the first cell of the prompt text. */
    PROMPT_START("A"),

    /** Marks the end of the prompt / start of user input. Attach to the first cell of the user input area. */
    COMMAND_START("B"),

    /** Marks the start of command output. Attach to the first cell of the command output. */
    OUTPUT_START("C"),

    /** Marks the end of command output. Attach to the last cell of the command output. */
    OUTPUT_END("D");

    private final String code;

    PromptMarker(String code) {
        this.code = code;
    }

    /** The OSC 133 parameter letter (A, B, C or D). */
    public String code() {
        return code;
    }

    /**
     * The full OSC 133 escape sequence for this marker, including the
     * String Terminator. Ready to write directly to the terminal.
     */
    public String escapeSequence() {
        return "\u001B]133;" + code + "\u001B\\";
    }
}
