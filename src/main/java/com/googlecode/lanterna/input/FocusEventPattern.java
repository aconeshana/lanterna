package com.googlecode.lanterna.input;

import java.util.List;

/**
 * Detects DEC 1004 focus event sequences in the input stream.
 * When focus reporting is enabled (via {@code Terminal.enableFocusReporting()}),
 * the terminal sends:
 * <ul>
 *   <li>{@code ESC [ I} — window gained focus</li>
 *   <li>{@code ESC [ O} — window lost focus</li>
 * </ul>
 * This pattern decodes them into {@link FocusEventKeyStroke} instances so
 * the application can react to focus changes (e.g., pause animations).
 */
public class FocusEventPattern implements CharacterPattern {

    private static final char ESC = '';

    @Override
    public Matching match(List<Character> seq) {
        if (seq == null || seq.isEmpty()) return null;
        // Must start with ESC
        if (seq.get(0) != ESC) return null;
        if (seq.size() < 2) return Matching.NOT_YET;
        // ESC [ — partial match, waiting for I or O
        if (seq.get(1) != '[') return null;
        if (seq.size() < 3) return Matching.NOT_YET;
        char c = seq.get(2);
        if (c == 'I') {
            return new Matching(new FocusEventKeyStroke(true));
        }
        if (c == 'O') {
            return new Matching(new FocusEventKeyStroke(false));
        }
        // ESC [ followed by something else — not a focus event
        return null;
    }
}
