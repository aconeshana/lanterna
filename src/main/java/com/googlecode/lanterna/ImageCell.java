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

import java.util.Objects;

/**
 * Inline image metadata attachable to a {@link TextCharacter} so the {@code Screen}
 * refresh emits an iTerm2-style {@code OSC 1337;File=...} escape sequence at that
 * cell. Supported by iTerm2, WezTerm, Ghostty, Kitty (via its image protocol
 * bridge) and any terminal advertising {@code OSC 1337} capability.
 * <p>
 * The cell carrying the {@code ImageCell} should usually be a space placeholder
 * occupying the first column of the image's display width; the terminal takes
 * care of advancing the cursor past the rendered image.
 * <p>
 * Either {@link #filePath()} or {@link #base64Data()} must be non-null. When
 * both are set, {@code filePath} wins (terminals prefer reading from disk to
 * avoid copying megabytes through the pty).
 *
 * @see TextCharacter#withImageCell(ImageCell)
 */
public final class ImageCell {

    /** Terminal image protocol to use for emission. */
    public enum Protocol {
        /** iTerm2-style OSC 1337;File=... — supported by iTerm2, WezTerm, Ghostty. */
        ITERM_OSC1337,
        /** Kitty graphics protocol APC (\e_G...\e\\) — supported by Kitty, Konsole, WezTerm. */
        KITTY_APC
    }

    private final Protocol protocol;
    private final String  filePath;
    private final String  base64Data;
    private final String  mediaType;          // e.g. "image/png"
    private final String  filename;           // optional, for terminal title
    private final int     widthCells;         // 0 = auto (terminal decides)
    private final int     heightCells;        // 0 = auto
    private final int     widthPx;            // 0 = unspecified
    private final int     heightPx;           // 0 = unspecified
    private final boolean preserveAspectRatio;
    private final boolean inline;             // true = render inline; false = popup

    private ImageCell(Builder b) {
        if (b.filePath == null && b.base64Data == null) {
            throw new IllegalArgumentException("ImageCell requires either filePath or base64Data");
        }
        this.protocol            = b.protocol;
        this.filePath            = b.filePath;
        this.base64Data          = b.base64Data;
        this.mediaType           = b.mediaType;
        this.filename            = b.filename;
        this.widthCells          = b.widthCells;
        this.heightCells         = b.heightCells;
        this.widthPx             = b.widthPx;
        this.heightPx            = b.heightPx;
        this.preserveAspectRatio = b.preserveAspectRatio;
        this.inline              = b.inline;
    }

    /** Which image protocol to use for emission. */
    public Protocol protocol() { return protocol; }
    /** Path on disk the terminal can read directly — preferred for large images. */
    public String filePath()   { return filePath; }
    /** Base64-encoded image bytes — used when no on-disk path is available. */
    public String base64Data() { return base64Data; }
    /** Media type such as {@code "image/png"}; null lets the terminal sniff. */
    public String mediaType()  { return mediaType; }
    /** Optional filename for display in the terminal's image title. */
    public String filename()   { return filename; }
    /** Image width in terminal columns (0 = auto). */
    public int widthCells()    { return widthCells; }
    /** Image height in terminal rows (0 = auto). */
    public int heightCells()   { return heightCells; }
    /** Image width in pixels (0 = unspecified). */
    public int widthPx()       { return widthPx; }
    /** Image height in pixels (0 = unspecified). */
    public int heightPx()      { return heightPx; }
    /** Whether to preserve aspect ratio when scaling. */
    public boolean preserveAspectRatio() { return preserveAspectRatio; }
    /** {@code true} = render inline in the text flow; {@code false} = popup. */
    public boolean inline()    { return inline; }

    /**
     * Build the {@code OSC 1337;File=...} escape sequence for this cell.
     * The caller writes it directly to the terminal before advancing the cursor.
     */
    public String escapeSequence() {
        if (protocol == Protocol.KITTY_APC) {
            return kittyEscapeSequence();
        }
        return itermEscapeSequence();
    }

    /** iTerm2-style OSC 1337;File=... escape sequence. */
    private String itermEscapeSequence() {
        StringBuilder params = new StringBuilder();
        if (filePath != null) {
            params.append("path=").append(escape(filePath));
        } else {
            if (mediaType != null) params.append("type=").append(mediaType).append(";");
            params.append("inline=1");
        }
        if (filename != null)            params.append(";name=").append(escape(filename));
        if (widthCells > 0)              params.append(";width=").append(widthCells);
        else if (widthPx > 0)            params.append(";width=").append(widthPx).append("px");
        if (heightCells > 0)             params.append(";height=").append(heightCells);
        else if (heightPx > 0)           params.append(";height=").append(heightPx).append("px");
        if (preserveAspectRatio)         params.append(";preserveAspectRatio=1");
        if (inline && filePath == null)  params.append(";inline=1");
        else if (inline)                 params.append(";inline=1");

        StringBuilder esc = new StringBuilder();
        esc.append('\u001B');
        esc.append("]1337;File=").append(params);
        if (base64Data != null && filePath == null) {
            esc.append(':').append(base64Data);
        }
        esc.append('\u001B');
        esc.append('\\');
        return esc.toString();
    }

    /**
     * Kitty graphics protocol APC sequence: ESC _ G ... ESC \\.
     */
    private String kittyEscapeSequence() {
        StringBuilder params = new StringBuilder();
        if (filePath != null && base64Data == null) {
            params.append("a=T,t=f");
        } else {
            params.append("a=T,t=d");
        }
        if (mediaType != null) {
            String mt = mediaType.toLowerCase();
            if ("image/png".equals(mt))         params.append(",f=100");
            else if ("image/jpeg".equals(mt))   params.append(",f=101");
            else if ("image/gif".equals(mt))    params.append(",f=102");
            else if ("image/webp".equals(mt))   params.append(",f=103");
        }
        if (widthCells > 0)  params.append(",c=").append(widthCells);
        if (heightCells > 0) params.append(",r=").append(heightCells);
        if (widthPx > 0 && heightPx > 0) {
            params.append(",s=").append(widthPx).append("x").append(heightPx);
        }
        if (filename != null) params.append(",n=").append(escape(filename));

        StringBuilder esc = new StringBuilder();
        esc.append('\u001B');
        esc.append("_G").append(params);
        if (base64Data != null) {
            esc.append(";").append(base64Data);
        } else if (filePath != null) {
            esc.append(";").append(java.util.Base64.getEncoder()
                    .encodeToString(filePath.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        esc.append('\u001B');
        esc.append('\\');
        return esc.toString();
    }

    /** Escape semicolons in OSC 1337 parameter values (iTerm2 convention). */
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace(";", "\\;");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageCell)) return false;
        ImageCell that = (ImageCell) o;
        return widthCells == that.widthCells
            && heightCells == that.heightCells
            && widthPx == that.widthPx
            && heightPx == that.heightPx
            && preserveAspectRatio == that.preserveAspectRatio
            && inline == that.inline
            && protocol == that.protocol
            && Objects.equals(filePath, that.filePath)
            && Objects.equals(base64Data, that.base64Data)
            && Objects.equals(mediaType, that.mediaType)
            && Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, filePath, base64Data, mediaType, filename,
                widthCells, heightCells, widthPx, heightPx,
                preserveAspectRatio, inline);
    }

    /** Builder for {@link ImageCell}. */
    public static final class Builder {
        private Protocol protocol = Protocol.ITERM_OSC1337;
        private String  filePath;
        private String  base64Data;
        private String  mediaType;
        private String  filename;
        private int     widthCells;
        private int     heightCells;
        private int     widthPx;
        private int     heightPx;
        private boolean preserveAspectRatio = true;
        private boolean inline = true;

        /** Set the image protocol (default {@link Protocol#ITERM_OSC1337}). */
        public Builder protocol(Protocol p)                   { this.protocol = p; return this; }
        /** Set an on-disk path the terminal can read directly. */
        public Builder filePath(String path)                  { this.filePath = path; return this; }
        /** Set base64-encoded image bytes (alternative to filePath). */
        public Builder base64Data(String data)                { this.base64Data = data; return this; }
        /** Set media type such as {@code "image/png"} (required for base64 mode). */
        public Builder mediaType(String type)                 { this.mediaType = type; return this; }
        /** Set optional filename for display. */
        public Builder filename(String name)                  { this.filename = name; return this; }
        /** Set image width in terminal columns. */
        public Builder widthCells(int w)                      { this.widthCells = w; return this; }
        /** Set image height in terminal rows. */
        public Builder heightCells(int h)                     { this.heightCells = h; return this; }
        /** Set image width in pixels. */
        public Builder widthPx(int w)                         { this.widthPx = w; return this; }
        /** Set image height in pixels. */
        public Builder heightPx(int h)                        { this.heightPx = h; return this; }
        /** Set whether to preserve aspect ratio (default true). */
        public Builder preserveAspectRatio(boolean p)         { this.preserveAspectRatio = p; return this; }
        /** Set whether to render inline (default true) vs popup. */
        public Builder inline(boolean i)                      { this.inline = i; return this; }

        /** Build the immutable {@link ImageCell}. */
        public ImageCell build() { return new ImageCell(this); }
    }

    /** Convenience: create an inline image cell from a file path. */
    public static ImageCell fromPath(String path) {
        return new Builder().filePath(path).build();
    }

    /** Convenience: create an inline image cell from base64 data + media type. */
    public static ImageCell fromBase64(String base64, String mediaType) {
        return new Builder().base64Data(base64).mediaType(mediaType).build();
    }
}
