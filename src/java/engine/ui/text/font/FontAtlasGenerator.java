package engine.ui.text.font;

public final class FontAtlasGenerator {

    private FontAtlasGenerator() {
    }

    public static RegularFontAtlas.Glyph gridGlyph(char value,
                                                   int row,
                                                   int column,
                                                   int glyphWidth,
                                                   int glyphHeight,
                                                   int glyphGapX,
                                                   int glyphGapY,
                                                   int advanceWidth) {
        return new RegularFontAtlas.Glyph(
                value,
                column * (glyphWidth + glyphGapX),
                row * (glyphHeight + glyphGapY),
                glyphWidth,
                glyphHeight,
                advanceWidth
        );
    }
}
