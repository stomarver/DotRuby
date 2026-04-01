package ui.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Parse {

    public record FontDefinition(String bitmapPath,
                                 int glyphWidth,
                                 int glyphHeight,
                                 int gapX,
                                 int gapY,
                                 int edgeGapX,
                                 int edgeGapY,
                                 int charSpacing,
                                 int spacing,
                                 String[] rows,
                                 Map<Character, GlyphSize> advances) {
    }

    public record GlyphSize(int width, int height) {
    }

    public record Glyph(char value, int atlasX, int atlasY, int atlasWidth, int atlasHeight, int advanceWidth) {
    }

    public record Quad(Glyph glyph, int drawX, int drawY) {
    }

    private Parse() {
    }

    public static FontDefinition font(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            return parseFont(lines);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read font definition: " + path, exception);
        }
    }

    public static List<Quad> text(FontDefinition font, String text) {
        List<Quad> quads = new ArrayList<>();
        int penX = 0;
        int penY = 0;

        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '\n') {
                penX = 0;
                penY += font.glyphHeight();
                continue;
            }
            if (value == ' ') {
                penX += Math.max(1, font.spacing()) + font.charSpacing();
                continue;
            }

            Glyph glyph = glyph(font, value);
            if (glyph == null) {
                penX += font.glyphWidth() + font.charSpacing();
                continue;
            }

            quads.add(new Quad(glyph, penX, penY - glyph.atlasHeight()));
            penX += glyph.advanceWidth() + font.charSpacing();
        }
        return quads;
    }

    public static Glyph glyph(FontDefinition font, char value) {
        int slotWidth = resolvedSlotWidth(font);
        for (int row = 0; row < font.rows().length; row++) {
            int column = font.rows()[row].indexOf(value);
            if (column < 0) {
                continue;
            }

            GlyphSize size = font.advances().getOrDefault(value, new GlyphSize(font.glyphWidth(), font.glyphHeight()));
            int advance = size.width();
            return new Glyph(
                    value,
                    font.edgeGapX() + (column * slotWidth) + (column * font.gapX()),
                    font.edgeGapY() + (row * font.glyphHeight()) + (row * font.gapY()),
                    size.width(),
                    size.height(),
                    advance
            );
        }
        return null;
    }

    private static FontDefinition parseFont(List<String> rawLines) {
        String bitmap = "";
        int glyphWidth = 6;
        int glyphHeight = 8;
        int gapX = 0;
        int gapY = 0;
        int edgeGapX = 0;
        int edgeGapY = 0;
        int charSpacing = 0;
        int spacing = 6;
        List<String> rows = new ArrayList<>();
        Map<Character, GlyphSize> advances = new HashMap<>();

        String section = "";
        for (String rawLine : rawLines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("rows")) {
                section = "rows";
                continue;
            }
            if (line.startsWith("advances")) {
                section = "advances";
                continue;
            }
            if (line.startsWith("diacritics")) {
                section = "diacritics";
                continue;
            }
            if (line.equals(")")) {
                section = "";
                continue;
            }

            if (!section.isEmpty()) {
                if (section.equals("rows")) {
                    rows.add(line);
                } else if (section.equals("advances")) {
                    parseAdvance(line, advances, glyphHeight);
                }
                continue;
            }

            if (line.startsWith("bitmap(")) {
                bitmap = valueInParens(line);
            } else if (line.startsWith("glyph-size(")) {
                int[] values = parsePair(valueInParens(line), "x");
                glyphWidth = values[0];
                glyphHeight = values[1];
            } else if (line.startsWith("glyph-gap(")) {
                int[] values = parsePair(valueInParens(line), ",");
                gapX = values[0];
                gapY = values[1];
            } else if (line.startsWith("edge-gap(")) {
                int[] values = parsePair(valueInParens(line), ",");
                edgeGapX = values[0];
                edgeGapY = values[1];
            } else if (line.startsWith("char-spacing(") || line.startsWith("glyph-spacing(")) {
                charSpacing = parseInt(valueInParens(line));
            } else if (line.startsWith("spacing(")) {
                spacing = parseInt(valueInParens(line));
            }
        }

        return new FontDefinition(
                bitmap,
                glyphWidth,
                glyphHeight,
                gapX,
                gapY,
                edgeGapX,
                edgeGapY,
                charSpacing,
                spacing,
                rows.toArray(String[]::new),
                Map.copyOf(advances)
        );
    }

    private static void parseAdvance(String line, Map<Character, GlyphSize> advances, int defaultHeight) {
        String[] parts = line.split("=", 2);
        if (parts.length != 2) {
            return;
        }
        GlyphSize glyphSize = parseGlyphSize(parts[1].trim(), defaultHeight);
        String symbols = parts[0].trim();
        for (int index = 0; index < symbols.length(); index++) {
            advances.put(symbols.charAt(index), glyphSize);
        }
    }

    private static String stripComment(String line) {
        int index = line.indexOf("//");
        return index >= 0 ? line.substring(0, index) : line;
    }

    private static String valueInParens(String line) {
        int left = line.indexOf('(');
        int right = line.lastIndexOf(')');
        if (left < 0 || right <= left) {
            return "";
        }
        return line.substring(left + 1, right).trim();
    }

    private static int[] parsePair(String body, String separator) {
        String[] parts = body.split(separator, 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected pair in '" + body + "'");
        }
        return new int[] {parseInt(parts[0]), parseInt(parts[1])};
    }

    private static int parseInt(String value) {
        return Integer.parseInt(value.trim().toLowerCase(Locale.ROOT).replace("+", ""));
    }

    private static GlyphSize parseGlyphSize(String value, int defaultHeight) {
        String normalized = value.trim();
        if (normalized.startsWith("(") && normalized.endsWith(")")) {
            int[] values = parsePair(normalized.substring(1, normalized.length() - 1), "x");
            return new GlyphSize(values[0], values[1]);
        }
        return new GlyphSize(parseInt(normalized), defaultHeight);
    }

    private static int resolvedSlotWidth(FontDefinition font) {
        int maxAdvance = font.glyphWidth();
        for (GlyphSize advance : font.advances().values()) {
            if (advance != null) {
                maxAdvance = Math.max(maxAdvance, advance.width());
            }
        }
        return Math.max(1, maxAdvance);
    }
}
