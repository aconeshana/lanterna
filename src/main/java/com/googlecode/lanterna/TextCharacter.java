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

import java.io.Serializable;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single character with additional metadata such as colors and modifiers. This class is immutable and
 * cannot be modified after creation.
 * @author Martin
 */
public class TextCharacter implements Serializable {
    private static EnumSet<SGR> toEnumSet(SGR... modifiers) {
        if(modifiers.length == 0) {
            return EnumSet.noneOf(SGR.class);
        }
        else {
            return EnumSet.copyOf(Arrays.asList(modifiers));
        }
    }

    public static final TextCharacter DEFAULT_CHARACTER = new TextCharacter(' ', TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT);

    /**
     * Hot-path cache for {@link #fromCharacter(char, TextColor, TextColor, SGR...)} with no SGR
     * modifiers. The terminal redraws the whole screen on every animation tick, so the same
     * (char, fg, bg) tuples are created many thousand times per second; without caching, every
     * call goes through {@code Character.toString} + {@code BreakIterator} + {@code String.intern}
     * which saturates the GUI thread and starves the rest of the JVM.
     *
     * <p>Cache key is a packed {@code long}: {@code (char << 48) | (fgHash << 16) | bgHash}.
     * Capped at 8192 entries — well above the realistic working set
     * (printable ASCII × handful of theme colors) so eviction is rare.
     */
    private static final ConcurrentHashMap<Long, TextCharacter> SIMPLE_CACHE = new ConcurrentHashMap<>();
    private static final int SIMPLE_CACHE_MAX = 8192;

    public static TextCharacter[] fromCharacter(char c) {
        return fromString(Character.toString(c));
    }

    public static TextCharacter[] fromString(String string) {
        return fromString(string, TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT);
    }

    public static TextCharacter fromCharacter(char c, TextColor foregroundColor, TextColor backgroundColor, SGR... modifiers) {
        // Fast path: no SGR modifiers + BMP char (the >99% case during redraws).
        if (modifiers.length == 0 && c >= 0x20 && c < 0xFFFF) {
            long key = ((long) c << 48)
                | (((long) System.identityHashCode(foregroundColor) & 0xFFFFL) << 16)
                | ((long) System.identityHashCode(backgroundColor) & 0xFFFFL);
            TextCharacter cached = SIMPLE_CACHE.get(key);
            if (cached != null
                && cached.foregroundColor.equals(foregroundColor)
                && cached.backgroundColor.equals(backgroundColor)
                && cached.character.length() == 1
                && cached.character.charAt(0) == c) {
                return cached;
            }
            TextCharacter created = fromString(Character.toString(c), foregroundColor, backgroundColor, modifiers)[0];
            if (SIMPLE_CACHE.size() < SIMPLE_CACHE_MAX) {
                SIMPLE_CACHE.putIfAbsent(key, created);
            }
            return created;
        }
        return fromString(Character.toString(c), foregroundColor, backgroundColor, modifiers)[0];
    }

    public static TextCharacter[] fromString(
            String string,
            TextColor foregroundColor,
            TextColor backgroundColor,
            SGR... modifiers) {
        return fromString(string, foregroundColor, backgroundColor, toEnumSet(modifiers));
    }

    public static TextCharacter[] fromString(
            String string,
            TextColor foregroundColor,
            TextColor backgroundColor,
            EnumSet<SGR> modifiers) {

        BreakIterator breakIterator = BreakIterator.getCharacterInstance();
        breakIterator.setText(string);
        List<TextCharacter> result = new ArrayList<>();
        for (int begin = 0, end = 0; (end = breakIterator.next()) != BreakIterator.DONE; begin = breakIterator.current()) {
            result.add(new TextCharacter(string.substring(begin, end), foregroundColor, backgroundColor, modifiers));
        }
        return result.toArray(new TextCharacter[0]);
    }

    /**
     * The "character" might not fit in a Java 16-bit char (emoji and other types) so we store it in a String
     * as of 3.1 instead.
     */
    private final String character;
    private final TextColor foregroundColor;
    private final TextColor backgroundColor;
    private final EnumSet<SGR> modifiers;  //This isn't immutable, but we should treat it as such and not expose it!

    /**
     * Optional OSC 8 hyperlink URL. When non-null, the Screen emits
     * {@code ESC ] 8 ; ; URL ESC \} before this cell and closes the hyperlink
     * after the run of cells sharing the same URL. Null means no hyperlink.
     */
    private final String hyperlinkUrl;

    /**
     * Optional OSC 1337 inline image. When non-null, the Screen emits the
     * image escape sequence at this cell. Null means no image.
     */
    private final ImageCell imageCell;

    /**
     * Optional OSC 133 prompt marker. When non-null, the Screen emits the
     * marker's escape sequence before this cell. Null means no marker.
     */
    private final PromptMarker promptMarker;

    /**
     * Creates a {@code ScreenCharacter} based on a supplied character, with default colors and no extra modifiers.
     * @param character Physical character to use
     * @deprecated Use fromCharacter instead
     */
    @Deprecated
    public TextCharacter(char character) {
        this(character, TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT);
    }
    
    /**
     * Copies another {@code ScreenCharacter}
     * @param character screenCharacter to copy from
     * @deprecated TextCharacters are immutable so you shouldn't need to call this
     */
    @Deprecated
    public TextCharacter(TextCharacter character) {
        this(character.getCharacterString(),
                character.getForegroundColor(), 
                character.getBackgroundColor(),
                EnumSet.copyOf(character.getModifiers()));
    }

    /**
     * Creates a new {@code ScreenCharacter} based on a physical character, color information and optional modifiers.
     * @param character Physical character to refer to
     * @param foregroundColor Foreground color the character has
     * @param backgroundColor Background color the character has
     * @param styles Optional list of modifiers to apply when drawing the character
     * @deprecated Use fromCharacter instead
     */
    @SuppressWarnings("WeakerAccess")
    @Deprecated
    public TextCharacter(
            char character,
            TextColor foregroundColor,
            TextColor backgroundColor,
            SGR... styles) {
        
        this(character, 
                foregroundColor, 
                backgroundColor, 
                toEnumSet(styles));
    }

    /**
     * Creates a new {@code ScreenCharacter} based on a physical character, color information and a set of modifiers.
     * @param character Physical character to refer to
     * @param foregroundColor Foreground color the character has
     * @param backgroundColor Background color the character has
     * @param modifiers Set of modifiers to apply when drawing the character
     * @deprecated Use fromCharacter instead
     */
    @Deprecated
    public TextCharacter(
            char character,
            TextColor foregroundColor,
            TextColor backgroundColor,
            EnumSet<SGR> modifiers) {
        this(Character.toString(character), foregroundColor, backgroundColor, modifiers);
    }

    /**
     * Creates a new {@code ScreenCharacter} based on a physical character, color information and a set of modifiers.
     * @param character Physical character to refer to
     * @param foregroundColor Foreground color the character has
     * @param backgroundColor Background color the character has
     * @param modifiers Set of modifiers to apply when drawing the character
     */
    private TextCharacter(
            String character,
            TextColor foregroundColor,
            TextColor backgroundColor,
            EnumSet<SGR> modifiers) {
        this(character, foregroundColor, backgroundColor, modifiers, null, null, null);
    }

    private TextCharacter(
            String character,
            TextColor foregroundColor,
            TextColor backgroundColor,
            EnumSet<SGR> modifiers,
            String hyperlinkUrl,
            ImageCell imageCell,
            PromptMarker promptMarker) {

        if (character.isEmpty()) {
            throw new IllegalArgumentException("Cannot create TextCharacter from an empty string");
        }
        validateSingleCharacter(character);

        // intern the string so we don't waste more memory than necessary
        this.character = character.intern();
        char firstCharacter = character.charAt(0);

        // Don't allow creating a TextCharacter containing a control character
        // For backward-compatibility, do allow tab for now
        if(TerminalTextUtils.isControlCharacter(firstCharacter) && firstCharacter != '\t') {
            throw new IllegalArgumentException("Cannot create a TextCharacter from a control character (0x" + Integer.toHexString(firstCharacter) + ")");
        }

        if(foregroundColor == null) {
            foregroundColor = TextColor.ANSI.DEFAULT;
        }
        if(backgroundColor == null) {
            backgroundColor = TextColor.ANSI.DEFAULT;
        }

        this.foregroundColor = foregroundColor;
        this.backgroundColor = backgroundColor;
        this.modifiers = EnumSet.copyOf(modifiers);
        this.hyperlinkUrl = hyperlinkUrl;
        this.imageCell = imageCell;
        this.promptMarker = promptMarker;
    }

    private void validateSingleCharacter(String character) {
        BreakIterator breakIterator = BreakIterator.getCharacterInstance();
        breakIterator.setText(character);
        String firstCharacter = null;
        for (int begin = 0, end = 0; (end = breakIterator.next()) != BreakIterator.DONE; begin = breakIterator.current()) {
            if (firstCharacter == null) {
                firstCharacter = character.substring(begin, end);
            }
            else {
                throw new IllegalArgumentException("Invalid String for TextCharacter, can only have one logical character");
            }
        }
    }

    public boolean is(char otherCharacter) {
        return otherCharacter == character.charAt(0) && character.length() == 1;
    }

    /**
     * The actual character this TextCharacter represents
     * @return character of the TextCharacter
     * @deprecated This won't work with advanced characters like emoji
     */
    @Deprecated
    public char getCharacter() {
        return character.charAt(0);
    }

    /**
     * Returns the character this TextCharacter represents as a String. This is not returning a char
     * @return
     */
    public String getCharacterString() {
        return character;
    }


    /**
     * Foreground color specified for this TextCharacter
     * @return Foreground color of this TextCharacter
     */
    public TextColor getForegroundColor() {
        return foregroundColor;
    }

    /**
     * Background color specified for this TextCharacter
     * @return Background color of this TextCharacter
     */
    public TextColor getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Returns a set of all active modifiers on this TextCharacter
     * @return Set of active SGR codes
     */
    public EnumSet<SGR> getModifiers() {
        return EnumSet.copyOf(modifiers);
    }

    /**
     * Returns true if this TextCharacter has the bold modifier active
     * @return {@code true} if this TextCharacter has the bold modifier active
     */
    public boolean isBold() {
        return modifiers.contains(SGR.BOLD);
    }

    /**
     * Returns true if this TextCharacter has the reverse modifier active
     * @return {@code true} if this TextCharacter has the reverse modifier active
     */
    public boolean isReversed() {
        return modifiers.contains(SGR.REVERSE);
    }

    /**
     * Returns true if this TextCharacter has the underline modifier active
     * @return {@code true} if this TextCharacter has the underline modifier active
     */
    public boolean isUnderlined() {
        return modifiers.contains(SGR.UNDERLINE);
    }

    /**
     * Returns true if this TextCharacter has the blink modifier active
     * @return {@code true} if this TextCharacter has the blink modifier active
     */
    public boolean isBlinking() {
        return modifiers.contains(SGR.BLINK);
    }

    /**
     * Returns true if this TextCharacter has the bordered modifier active
     * @return {@code true} if this TextCharacter has the bordered modifier active
     */
    public boolean isBordered() {
        return modifiers.contains(SGR.BORDERED);
    }

    /**
     * Returns true if this TextCharacter has the crossed-out modifier active
     * @return {@code true} if this TextCharacter has the crossed-out modifier active
     */
    public boolean isCrossedOut() {
        return modifiers.contains(SGR.CROSSED_OUT);
    }

    /**
     * Returns true if this TextCharacter has the italic modifier active
     * @return {@code true} if this TextCharacter has the italic modifier active
     */
    public boolean isItalic() {
        return modifiers.contains(SGR.ITALIC);
    }

    /**
     * Returns a new TextCharacter with the same colors and modifiers but a different underlying character
     * @param character Character the copy should have
     * @return Copy of this TextCharacter with different underlying character
     */
    @SuppressWarnings("SameParameterValue")
    public TextCharacter withCharacter(char character) {
        if(this.character.equals(Character.toString(character))) {
            return this;
        }
        return new TextCharacter(Character.toString(character), foregroundColor, backgroundColor, modifiers,
                hyperlinkUrl, imageCell, promptMarker);
    }

    /**
     * Returns a copy of this TextCharacter with a specified foreground color
     * @param foregroundColor Foreground color the copy should have
     * @return Copy of the TextCharacter with a different foreground color
     */
    public TextCharacter withForegroundColor(TextColor foregroundColor) {
        if(this.foregroundColor == foregroundColor || this.foregroundColor.equals(foregroundColor)) {
            return this;
        }
        return new TextCharacter(character, foregroundColor, backgroundColor, modifiers,
                hyperlinkUrl, imageCell, promptMarker);
    }

    /**
     * Returns a copy of this TextCharacter with a specified background color
     * @param backgroundColor Background color the copy should have
     * @return Copy of the TextCharacter with a different background color
     */
    public TextCharacter withBackgroundColor(TextColor backgroundColor) {
        if(this.backgroundColor == backgroundColor || this.backgroundColor.equals(backgroundColor)) {
            return this;
        }
        return new TextCharacter(character, foregroundColor, backgroundColor, modifiers,
                hyperlinkUrl, imageCell, promptMarker);
    }

    /**
     * Returns a copy of this TextCharacter with specified list of SGR modifiers. None of the currently active SGR codes
     * will be carried over to the copy, only those in the passed in value.
     * @param modifiers SGR modifiers the copy should have
     * @return Copy of the TextCharacter with a different set of SGR modifiers
     */
    public TextCharacter withModifiers(Collection<SGR> modifiers) {
        EnumSet<SGR> newSet = EnumSet.copyOf(modifiers);
        if(modifiers.equals(newSet)) {
            return this;
        }
        return new TextCharacter(character, foregroundColor, backgroundColor, newSet,
                hyperlinkUrl, imageCell, promptMarker);
    }

    /**
     * Returns a copy of this TextCharacter with an additional SGR modifier. All of the currently active SGR codes
     * will be carried over to the copy, in addition to the one specified.
     * @param modifier SGR modifiers the copy should have in additional to all currently present
     * @return Copy of the TextCharacter with a new SGR modifier
     */
    public TextCharacter withModifier(SGR modifier) {
        if(modifiers.contains(modifier)) {
            return this;
        }
        EnumSet<SGR> newSet = EnumSet.copyOf(this.modifiers);
        newSet.add(modifier);
        return new TextCharacter(character, foregroundColor, backgroundColor, newSet,
                hyperlinkUrl, imageCell, promptMarker);
    }

    /**
     * Returns a copy of this TextCharacter with an SGR modifier removed. All of the currently active SGR codes
     * will be carried over to the copy, except for the one specified. If the current TextCharacter doesn't have the
     * SGR specified, it will return itself.
     * @param modifier SGR modifiers the copy should not have
     * @return Copy of the TextCharacter without the SGR modifier
     */
    public TextCharacter withoutModifier(SGR modifier) {
        if(!modifiers.contains(modifier)) {
            return this;
        }
        EnumSet<SGR> newSet = EnumSet.copyOf(this.modifiers);
        newSet.remove(modifier);
        return new TextCharacter(character, foregroundColor, backgroundColor, newSet,
                hyperlinkUrl, imageCell, promptMarker);
    }

    /**
     * Returns the OSC 8 hyperlink URL attached to this cell, or {@code null} if none.
     * @return hyperlink URL or null
     */
    public String getHyperlinkUrl() {
        return hyperlinkUrl;
    }

    /**
     * Returns the OSC 1337 inline image attached to this cell, or {@code null} if none.
     * @return image cell or null
     */
    public ImageCell getImageCell() {
        return imageCell;
    }

    /**
     * Returns the OSC 133 prompt marker attached to this cell, or {@code null} if none.
     * @return prompt marker or null
     */
    public PromptMarker getPromptMarker() {
        return promptMarker;
    }

    /**
     * Returns a copy of this TextCharacter with the specified OSC 8 hyperlink URL.
     * Pass {@code null} to remove any existing hyperlink.
     * @param hyperlinkUrl URL the hyperlink should point to, or null to clear
     * @return Copy of the TextCharacter with the hyperlink set
     */
    public TextCharacter withHyperlink(String hyperlinkUrl) {
        if (Objects.equals(this.hyperlinkUrl, hyperlinkUrl)) {
            return this;
        }
        return new TextCharacter(character, foregroundColor, backgroundColor, modifiers,
                hyperlinkUrl, imageCell, promptMarker);
    }

    /**
     * Returns a copy of this TextCharacter with the specified OSC 1337 inline image.
     * Pass {@code null} to remove any existing image.
     * @param imageCell image metadata, or null to clear
     * @return Copy of the TextCharacter with the image set
     */
    public TextCharacter withImageCell(ImageCell imageCell) {
        if (Objects.equals(this.imageCell, imageCell)) {
            return this;
        }
        return new TextCharacter(character, foregroundColor, backgroundColor, modifiers,
                hyperlinkUrl, imageCell, promptMarker);
    }

    /**
     * Returns a copy of this TextCharacter with the specified OSC 133 prompt marker.
     * Pass {@code null} to remove any existing marker.
     * @param promptMarker marker enum, or null to clear
     * @return Copy of the TextCharacter with the marker set
     */
    public TextCharacter withPromptMarker(PromptMarker promptMarker) {
        if (Objects.equals(this.promptMarker, promptMarker)) {
            return this;
        }
        return new TextCharacter(character, foregroundColor, backgroundColor, modifiers,
                hyperlinkUrl, imageCell, promptMarker);
    }

    public boolean isDoubleWidth() {
        // TODO: make this better to work properly with emoji and other complicated "characters"
        return TerminalTextUtils.isCharDoubleWidth(character.charAt(0)) ||
                isEmoji(character) ||
                // If the character takes up more than one char, assume it's double width (unless thai)
                (character.length() > 1 && !TerminalTextUtils.isCharThai(character.charAt(0)));
    }

    private static boolean isEmoji(final String s) {
        // This is really hard to do properly and would require an emoji library as a dependency, so here's a hack that
        // basically assumes anything NOT a regular latin1/CJK/thai character is an emoji
        char firstCharacter = s.charAt(0);
        return s.length() > 1 ||
                !(TerminalTextUtils.isCharCJK(firstCharacter) ||
                        TerminalTextUtils.isPrintableCharacter(firstCharacter) ||
                        TerminalTextUtils.isCharThai(firstCharacter) ||
                        TerminalTextUtils.isCharCJK(firstCharacter) ||
                        TerminalTextUtils.isControlCharacter(firstCharacter));
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final TextCharacter other = (TextCharacter) obj;
        if(!Objects.equals(this.character, other.character)) {
            return false;
        }
        if(!Objects.equals(this.foregroundColor, other.foregroundColor)) {
            return false;
        }
        if(!Objects.equals(this.backgroundColor, other.backgroundColor)) {
            return false;
        }
        if(!Objects.equals(this.modifiers, other.modifiers)) {
            return false;
        }
        if(!Objects.equals(this.hyperlinkUrl, other.hyperlinkUrl)) {
            return false;
        }
        if(!Objects.equals(this.imageCell, other.imageCell)) {
            return false;
        }
        return Objects.equals(this.promptMarker, other.promptMarker);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.character.hashCode();
        hash = 37 * hash + (this.foregroundColor != null ? this.foregroundColor.hashCode() : 0);
        hash = 37 * hash + (this.backgroundColor != null ? this.backgroundColor.hashCode() : 0);
        hash = 37 * hash + (this.modifiers != null ? this.modifiers.hashCode() : 0);
        hash = 37 * hash + (this.hyperlinkUrl != null ? this.hyperlinkUrl.hashCode() : 0);
        hash = 37 * hash + (this.imageCell != null ? this.imageCell.hashCode() : 0);
        hash = 37 * hash + (this.promptMarker != null ? this.promptMarker.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "TextCharacter{" + "character=" + character
                + ", foregroundColor=" + foregroundColor
                + ", backgroundColor=" + backgroundColor
                + ", modifiers=" + modifiers
                + (hyperlinkUrl != null ? ", hyperlink=" + hyperlinkUrl : "")
                + (imageCell != null ? ", image=" + imageCell : "")
                + (promptMarker != null ? ", marker=" + promptMarker : "")
                + '}';
    }
}
