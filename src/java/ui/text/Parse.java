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
                                 int charSpacingX,
                                 int charSpacingY,
                                 int spacing,
                                 int glyphBaseline, // Смещение глифа вниз относительно penY
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
            return parseFont(lines, path);
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
                // penY сдвигается на высоту слота + межстрочный отступ.
                // Baseline влияет только на рисование, а не на высоту строки сетки.
                penY += font.glyphHeight() + font.charSpacingY();
                continue;
            }

            if (value == ' ') {
                // ИСПРАВЛЕНИЕ BUG: убрали Math.max(1, ...).
                // Теперь spacing(0) честно даёт 0 ширины, а spacing(-1) - наезд.
                penX += font.spacing() + font.charSpacingX();
                continue;
            }

            Glyph glyph = glyph(font, value);
            if (glyph == null) {
                penX += font.glyphWidth() + font.charSpacingX();
                continue;
            }

            // ИСПРАВЛЕНИЕ FEATURE: добавляем glyphBaseline.
            // glyphBaseline позволяет сместить все буквы вниз (например, для привязки к базовой линии),
            // не меняя логику переноса строк и сетки.
            quads.add(new Quad(glyph, penX, penY + font.glyphBaseline()));

            penX += glyph.advanceWidth() + font.charSpacingX();
        }
        return quads;
    }

    public static Glyph glyph(FontDefinition font, char value) {
        // Размер слота в атласе (фиксированный!)
        int slotWidth = font.glyphWidth();
        int slotHeight = font.glyphHeight();

        for (int row = 0; row < font.rows().length; row++) {
            int column = font.rows()[row].indexOf(value);
            if (column < 0) {
                continue;
            }

            // Позиция слота в атласе по фиксированной сетке:
            // edge + column * (slot + gap)
            int atlasX = font.edgeGapX() + column * (slotWidth + font.gapX());
            int atlasY = font.edgeGapY() + row * (slotHeight + font.gapY());

            // Реальный размер глифа (может быть меньше слота)
            GlyphSize size = font.advances().getOrDefault(
                    value,
                    new GlyphSize(slotWidth, slotHeight)
            );

            // Защита от выхода за границы слота (если в конфиге ошибка)
            int drawWidth = Math.min(size.width(), slotWidth);
            int drawHeight = Math.min(size.height(), slotHeight);

            return new Glyph(
                    value,
                    atlasX,
                    atlasY,
                    drawWidth,
                    drawHeight,
                    size.width() // advance
            );
        }
        return null;
    }

    private static FontDefinition parseFont(List<String> rawLines, Path definitionPath) {
        String atlas = siblingBitmapPath(definitionPath);
        int glyphWidth = 6;
        int glyphHeight = 8;
        int gapX = 0;
        int gapY = 0;
        int edgeGapX = 0;
        int edgeGapY = 0;
        int charSpacingX = 0;
        int charSpacingY = 0;
        int spacing = 3; // Значение по умолчанию
        int glyphBaseline = 0; // Значение по умолчанию (0 = выравнивание по верху)

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

            if (line.startsWith("glyph-size(")) {
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
                String value = valueInParens(line);
                if (value.contains(",")) {
                    int[] values = parsePair(value, ",");
                    charSpacingX = values[0];
                    charSpacingY = values[1];
                } else {
                    charSpacingX = parseInt(value);
                    charSpacingY = 0;
                }
            } else if (line.startsWith("glyph-baseline(")) {
                glyphBaseline = parseInt(valueInParens(line));
            } else if (line.startsWith("spacing(")) {
                spacing = parseInt(valueInParens(line));
            }
        }

        return new FontDefinition(
                atlas,
                glyphWidth,
                glyphHeight,
                gapX,
                gapY,
                edgeGapX,
                edgeGapY,
                charSpacingX,
                charSpacingY,
                spacing,
                glyphBaseline,
                rows.toArray(String[]::new),
                Map.copyOf(advances)
        );
    }

    private static String siblingBitmapPath(Path definitionPath) {
        if (definitionPath == null) {
            return "";
        }

        String fileName = definitionPath.getFileName() == null
                ? ""
                : definitionPath.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        String baseName = extension >= 0 ? fileName.substring(0, extension) : fileName;
        Path directory = definitionPath.getParent();
        Path bitmapPath = (directory == null ? Path.of(baseName + ".png") : directory.resolve(baseName + ".png"));
        return bitmapPath.toString();
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
}
