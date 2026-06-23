package com.googlecode.lanterna;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Verifies {@link ImageCell} escape sequences for both iTerm2 OSC 1337
 * and Kitty APC protocols, plus {@link TextCharacter} protocol field
 * propagation.
 */
public class ImageCellTest {

    @Test
    public void itermPathBasedEscapeSequence() {
        ImageCell cell = ImageCell.fromPath("/tmp/img.png");
        String esc = cell.escapeSequence();
        assertTrue("Should start with OSC 1337 prefix",
            esc.startsWith("\u001B]1337;File=path="));
        assertTrue("Should contain the path", esc.contains("/tmp/img.png"));
        assertTrue("Should end with ST", esc.endsWith("\u001B\\"));
    }

    @Test
    public void itermBase64EscapeSequence() {
        ImageCell cell = ImageCell.fromBase64("iVBORw0KGgo=", "image/png");
        String esc = cell.escapeSequence();
        assertTrue("Should contain inline=1", esc.contains("inline=1"));
        assertTrue("Should contain type=image/png", esc.contains("type=image/png"));
        assertTrue("Should contain base64 payload", esc.contains(":iVBORw0KGgo="));
    }

    @Test
    public void kittyProtocolUsesAPC() {
        ImageCell cell = new ImageCell.Builder()
            .protocol(ImageCell.Protocol.KITTY_APC)
            .base64Data("iVBORw0KGgo=")
            .mediaType("image/png")
            .build();
        String esc = cell.escapeSequence();
        // Kitty uses ESC _ G (APC) not ESC ] 1337 (OSC)
        assertTrue("Should start with ESC _ G", esc.startsWith("\u001B_G"));
        assertTrue("Should contain a=T,t=d", esc.contains("a=T,t=d"));
        assertTrue("Should contain f=100 for PNG", esc.contains(",f=100"));
        assertTrue("Should end with ST", esc.endsWith("\u001B\\"));
    }

    @Test
    public void kittyPathModeUsesTF() {
        ImageCell cell = new ImageCell.Builder()
            .protocol(ImageCell.Protocol.KITTY_APC)
            .filePath("/tmp/img.png")
            .mediaType("image/png")
            .build();
        String esc = cell.escapeSequence();
        assertTrue("Should use t=f for file path mode", esc.contains("t=f"));
    }

    @Test
    public void builderRejectsEmptyCell() {
        try {
            new ImageCell.Builder().build();
            fail("Should reject cell with no filePath or base64Data");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void textCharacterCarriesHyperlink() {
        TextCharacter tc = TextCharacter.fromString("X")[0]
            .withHyperlink("https://example.com");
        assertEquals("https://example.com", tc.getHyperlinkUrl());
    }

    @Test
    public void textCharacterCarriesPromptMarker() {
        TextCharacter tc = TextCharacter.fromString(">")[0]
            .withPromptMarker(PromptMarker.PROMPT_START);
        assertEquals(PromptMarker.PROMPT_START, tc.getPromptMarker());
    }

    @Test
    public void textCharacterCarriesImageCell() {
        ImageCell img = ImageCell.fromPath("/tmp/x.png");
        TextCharacter tc = TextCharacter.fromString(" ")[0]
            .withImageCell(img);
        assertEquals(img, tc.getImageCell());
    }

    @Test
    public void textCharacterEqualsIncludesProtocolFields() {
        TextCharacter a = TextCharacter.fromString("X")[0];
        TextCharacter b = TextCharacter.fromString("X")[0];
        assertEquals(a, b);

        TextCharacter c = b.withHyperlink("https://example.com");
        assertNotEquals("Hyperlink change should break equality", a, c);

        TextCharacter d = b.withPromptMarker(PromptMarker.OUTPUT_END);
        assertNotEquals("Prompt marker change should break equality", a, d);
    }

    @Test
    public void withHyperlinkNullClears() {
        TextCharacter tc = TextCharacter.fromString("X")[0]
            .withHyperlink("https://example.com")
            .withHyperlink(null);
        assertNull(tc.getHyperlinkUrl());
    }
}
