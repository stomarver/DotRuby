package engine.visual.scene.temp;

import engine.visual.Overlay;
import engine.visual.Render;

import java.util.ArrayList;
import java.util.List;

public final class TextLayoutEngine {

    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

    public record TextLine(String value, float scale, Render.TextScale textScale) {
        public TextLine(String value, float scale) {
            this(value, scale, Render.TextScale.standard());
        }
    }

    private final List<TextLine> lines = new ArrayList<>();
    private float lineGap = 6f;

    public TextLayoutEngine lineGap(float lineGap) {
        this.lineGap = Math.max(0f, lineGap);
        return this;
    }

    public TextLayoutEngine clear() {
        lines.clear();
        return this;
    }

    public TextLayoutEngine add(String value, float scale) {
        return add(value, scale, Render.TextScale.standard());
    }

    public TextLayoutEngine add(String value, float scale, Render.TextScale textScale) {
        lines.add(new TextLine(value, Math.max(1f, scale), textScale));
        return this;
    }

    public void drawColumn(Overlay overlay, Render render, float x, float y) {
        drawColumn(overlay, render, x, y, 0f, Align.LEFT);
    }

    public void drawColumn(Overlay overlay, Render render, float x, float y, float width, Align align) {
        float penY = y;
        for (TextLine line : lines) {
            float drawX = switch (align) {
                case LEFT -> x;
                case CENTER -> x + Math.max(0f, (width - estimateWidth(line)) * 0.5f);
                case RIGHT -> x + Math.max(0f, width - estimateWidth(line));
            };
            render.drawText(overlay, line.value(), drawX, penY, line.scale(), line.textScale());
            penY += estimateHeight(line) + lineGap;
        }
    }

    public void drawWrapped(Overlay overlay, Render render, String text, float x, float y, float width, float scale) {
        float penX = x;
        float penY = y;
        float spaceWidth = 4f * scale;
        for (String word : text.split(" ")) {
            float wordWidth = Math.max(1, word.length()) * 5f * scale;
            if (penX > x && penX + wordWidth > x + width) {
                penX = x;
                penY += 8f * scale + lineGap;
            }
            render.drawText(overlay, word, penX, penY, scale, Render.TextScale.standard());
            penX += wordWidth + spaceWidth;
        }
    }

    private static float estimateWidth(TextLine line) {
        return Math.max(1, line.value() == null ? 0 : line.value().length()) * 5f * line.scale();
    }

    private static float estimateHeight(TextLine line) {
        return 8f * line.scale();
    }
}
