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
                                 int slotWidth,
                                 int slotHeight,
                                 int glyphWidth,
                                 int glyphHeight,
                                 int gapX,
                                 int gapY,
                                 int edgeGapX,
                                 int edgeGapY,
                                 int charSpacing,
                                 int spacing,
                                 String[] rows,
                                 Map<Character, Integer> advances) {
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
        for (int row = 0; row < font.rows().length; row++) {
            int column = font.rows()[row].indexOf(value);
            if (column < 0) {
                continue;
            }

            int advance = font.advances().getOrDefault(value, font.glyphWidth());
            return new Glyph(
                    value,
                    font.edgeGapX() + (column * font.slotWidth()) + (column * font.gapX()),
                    font.edgeGapY() + (row * font.slotHeight()) + (row * font.gapY()) + (font.slotHeight() - font.glyphHeight()),
                    advance,
                    font.glyphHeight(),
                    advance
            );
        }
        return null;
    }

    private static FontDefinition parseFont(List<String> rawLines) {
        String bitmap = "";
        int slotWidth = 6;
        int slotHeight = 8;
        int glyphWidth = 6;
        int glyphHeight = 8;
        int gapX = 0;
        int gapY = 0;
        int edgeGapX = 0;
        int edgeGapY = 0;
        int charSpacing = 0;
        int spacing = 6;
        List<String> rows = new ArrayList<>();
        Map<Character, Integer> advances = new HashMap<>();

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
                    parseAdvance(line, advances);
                }
                continue;
            }

            if (line.startsWith("bitmap(")) {
                bitmap = valueInParens(line);
            } else if (line.startsWith("slot-size(")) {
                int[] values = parsePair(valueInParens(line), "x");
                slotWidth = values[0];
                slotHeight = values[1];
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
                slotWidth,
                slotHeight,
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

    private static void parseAdvance(String line, Map<Character, Integer> advances) {
        String[] parts = line.split("=", 2);
        if (parts.length != 2) {
            return;
        }
        int width = parseInt(parts[1].trim());
        String symbols = parts[0].trim();
        for (int index = 0; index < symbols.length(); index++) {
            advances.put(symbols.charAt(index), width);
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
}
