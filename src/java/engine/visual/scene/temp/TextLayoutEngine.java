package engine.visual.scene.temp;

import engine.visual.Overlay;
import engine.visual.Render;

import java.util.List;

public final class TextLayoutEngine {

    private static final float GLYPH_WIDTH = 5f;
    private static final float GLYPH_HEIGHT = 8f;
    private static final float WORD_GAP = 4f;

    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

    public float drawLine(Overlay overlay, Render render, String value, float x, float y, float scale) {
        render.drawText(overlay, value, x, y, scale, Render.TextScale.standard());
        return y + lineHeight(scale);
    }

    public float drawLine(Overlay overlay, Render render, String value, float x, float y, float width, float scale, Align align) {
        float drawX = switch (align) {
            case LEFT -> x;
            case CENTER -> x + Math.max(0f, (width - estimateWidth(value, scale)) * 0.5f);
            case RIGHT -> x + Math.max(0f, width - estimateWidth(value, scale));
        };
        return drawLine(overlay, render, value, drawX, y, scale);
    }

    public float drawHeading(Overlay overlay, Render render, String value, float x, float y) {
        float nextY = drawLine(overlay, render, value, x, y, 2f);
        return drawRule(overlay, render, x, nextY + 2f, value.length()) + 8f;
    }

    public float drawParagraph(Overlay overlay, Render render, String value, float x, float y, float width, float scale) {
        float penX = x;
        float penY = y;
        for (String word : value.split(" ")) {
            float wordWidth = estimateWidth(word, scale);
            if (penX > x && penX + wordWidth > x + width) {
                penX = x;
                penY += lineHeight(scale) + 2f;
            }
            render.drawText(overlay, word, penX, penY, scale, Render.TextScale.standard());
            penX += wordWidth + (WORD_GAP * scale);
        }
        return penY + lineHeight(scale);
    }

    public float drawBullets(Overlay overlay, Render render, List<String> values, float x, float y, float width) {
        float penY = y;
        for (String value : values) {
            penY = drawParagraph(overlay, render, "> " + value, x, penY, width, 1f) + 4f;
        }
        return penY;
    }

    public float drawKeyValue(Overlay overlay, Render render, String key, String value, float x, float y, float keyWidth) {
        render.drawText(overlay, key, x, y, 1f, Render.TextScale.fixed());
        render.drawText(overlay, value, x + keyWidth, y, 1f, Render.TextScale.standard());
        return y + lineHeight(1f) + 4f;
    }

    public float drawRule(Overlay overlay, Render render, float x, float y, int cells) {
        String rule = "-".repeat(Math.max(4, cells));
        render.drawText(overlay, rule, x, y, 1f, Render.TextScale.fixed());
        return y + lineHeight(1f);
    }

    private static float estimateWidth(String value, float scale) {
        return Math.max(1, value == null ? 0 : value.length()) * GLYPH_WIDTH * Math.max(1f, scale);
    }

    private static float lineHeight(float scale) {
        return GLYPH_HEIGHT * Math.max(1f, scale);
    }
}
