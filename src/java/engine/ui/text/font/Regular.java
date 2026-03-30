package engine.ui.text.font;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Regular {

    private static final int GLYPH_WIDTH = 6;
    private static final int GLYPH_HEIGHT = 8;
    private static final int GLYPH_GAP_X = 1;
    private static final int GLYPH_GAP_Y = 1;
    private static final int ADVANCE_WIDE = 6;
    private static final int ADVANCE_EXTRA_WIDE = 7;
    private static final int ADVANCE_COMMON = 5;
    private static final int ADVANCE_NARROW = 4;
    private static final int ADVANCE_THIN = 3;
    private static final int ADVANCE_SLIM = 2;
    private static final char[] ADVANCE_WIDE_CHARS = {
            'M', 'm', 'T', 'V', 'W', 'w',
            'Д', 'Ж', 'ж', 'Т', 'т', 'Ф', 'ф', 'Х', 'Ц', 'ц', 'Ш', 'ш', 'Ъ', 'ъ'
    };
    private static final char[] ADVANCE_EXTRA_WIDE_CHARS = {'Щ', 'щ', 'Ы', 'ы', 'Ю', 'ю'};
    private static final char[] ADVANCE_COMMON_CHARS = {
            'A', 'a', 'B', 'b', 'C', 'c', 'D', 'd', 'E', 'e', 'F', 'G', 'g', 'H', 'h',
            'J', 'j', 'K', 'k', 'L', 'N', 'n', 'O', 'o', 'P', 'p', 'Q', 'q', 'R', 'r',
            'S', 's', 'U', 'u', 'v', 'X', 'x', 'Y', 'y', 'Z', 'z',
            'А', 'а', 'Б', 'б', 'В', 'в', 'Г', 'д', 'Е', 'е', 'З', 'з', 'И', 'и',
            'К', 'к', 'Л', 'л', 'Ь', 'ь', 'Н', 'н', 'О', 'о', 'П', 'п', 'Р', 'р', 'С', 'с',
            'У', 'у', 'х', 'Ч', 'ч', 'Э', 'э', 'Я', 'я'
    };
    private static final char[] ADVANCE_NARROW_CHARS = {'I', 'f', 't', 'г'};
    private static final char[] ADVANCE_THIN_CHARS = {'l'};
    private static final char[] ADVANCE_SLIM_CHARS = {'i'};
    private static final String[] ROWS = {
            "AaBbCcDdEeFfGgHhIi",
            "JjKkLlMmNnOoPpQqRr",
            "SsTtUuVvWwXxYyZz",
            "АаБбВвГгДдЕеЖжЗзИи",
            "КкЛлМмНнОоПпРрСсТт",
            "УуФфХхЦцЧчШшЩщЪъЫы",
            "ЬьЭэЮюЯя"
    };

    public record Glyph(char value, int atlasX, int atlasY, int atlasWidth, int atlasHeight, int advanceWidth) {
    }

    public record GlyphParameters(int advanceWidth) {
    }

    public record Quad(Glyph glyph, int drawX, int drawY) {
    }

    private final Map<Character, GlyphParameters> glyphParameters = new HashMap<>();

    public Regular() {
        withAdvanceWidth(ADVANCE_WIDE_CHARS, ADVANCE_WIDE);
        withAdvanceWidth(ADVANCE_EXTRA_WIDE_CHARS, ADVANCE_EXTRA_WIDE);
        withAdvanceWidth(ADVANCE_COMMON_CHARS, ADVANCE_COMMON);
        withAdvanceWidth(ADVANCE_NARROW_CHARS, ADVANCE_NARROW);
        withAdvanceWidth(ADVANCE_THIN_CHARS, ADVANCE_THIN);
        withAdvanceWidth(ADVANCE_SLIM_CHARS, ADVANCE_SLIM);
    }

    public Regular withAdvanceWidth(char value, int advanceWidth) {
        glyphParameters.put(value, new GlyphParameters(Math.max(1, Math.min(advanceWidth, GLYPH_WIDTH))));
        return this;
    }

    public Regular withAdvanceWidth(char[] values, int advanceWidth) {
        for (char value : values) {
            withAdvanceWidth(value, advanceWidth);
        }
        return this;
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
            penX += glyph.advanceWidth() + GLYPH_GAP_X;
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
                    row * (GLYPH_HEIGHT + GLYPH_GAP_Y),
                    GLYPH_WIDTH,
                    GLYPH_HEIGHT,
                    parameters(value).advanceWidth()
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

    private GlyphParameters parameters(char value) {
        return glyphParameters.getOrDefault(value, new GlyphParameters(GLYPH_WIDTH));
    }
}
