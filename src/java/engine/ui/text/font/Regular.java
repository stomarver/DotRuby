package engine.ui.text.font;

import java.util.ArrayList;
import java.util.List;

public final class Regular {

    private static final int GLYPH_WIDTH = 6;
    private static final int GLYPH_HEIGHT = 8;
    private static final int GLYPH_GAP_X = 1;
    private static final int GLYPH_GAP_Y = 1;
    private static final String[] ROWS = {
            "AaBbCcDdEeFfGgHhIi",
            "JjKkLlMmNnOoPpQqRr",
            "SsTtUuVvWwXxYyZz"
    };

    public record Glyph(char value, int atlasX, int atlasY) {
    }

    public record Quad(Glyph glyph, int drawX, int drawY) {
    }

    public List<Quad> parse(String text) {
        List<Quad> quads = new ArrayList<>();
        int penX = 0;
        int penY = 0;

        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '\n') {
                penX = 0;
                penY += GLYPH_HEIGHT + GLYPH_GAP_Y;
                continue;
            }
            if (value == ' ') {
                penX += GLYPH_WIDTH + GLYPH_GAP_X;
                continue;
            }

            Glyph glyph = glyph(value);
            if (glyph == null) {
                penX += GLYPH_WIDTH + GLYPH_GAP_X;
                continue;
            }

            quads.add(new Quad(glyph, penX, penY));
            penX += GLYPH_WIDTH + GLYPH_GAP_X;
        }

        return quads;
    }

    public Glyph glyph(char value) {
        for (int row = 0; row < ROWS.length; row++) {
            int column = ROWS[row].indexOf(value);
            if (column < 0) {
                continue;
            }

            return new Glyph(
                    value,
                    column * (GLYPH_WIDTH + GLYPH_GAP_X),
                    row * (GLYPH_HEIGHT + GLYPH_GAP_Y)
            );
        }
        return null;
    }

    public int glyphWidth() {
        return GLYPH_WIDTH;
    }

    public int glyphHeight() {
        return GLYPH_HEIGHT;
    }
}
