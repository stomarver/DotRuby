package ui;

import engine.util.EngineConstraints;
import engine.visual.Overlay;

public final class Selection {

    private boolean active;
    private float startX;
    private float startY;
    private float endX;
    private float endY;

    public void begin(float x, float y) {
        EngineConstraints.requirePixelAligned(x, "Selection.begin(x)");
        EngineConstraints.requirePixelAligned(y, "Selection.begin(y)");
        active = true;
        startX = x;
        startY = y;
        EngineConstraints.requirePixelAligned(x, "Selection.begin(x)");
        EngineConstraints.requirePixelAligned(y, "Selection.begin(y)");
        endX = x;
        endY = y;
    }

    public void update(float x, float y) {
        if (!active) {
            return;
        }
        EngineConstraints.requirePixelAligned(x, "Selection.begin(x)");
        EngineConstraints.requirePixelAligned(y, "Selection.begin(y)");
        endX = x;
        endY = y;
    }

    public void clear() {
        active = false;
    }

    public void render(Overlay overlay, float borderThickness) {
        if (!active) {
            return;
        }

        float minX = Math.min(startX, endX);
        float maxX = Math.max(startX, endX);
        float minY = Math.min(startY, endY);
        float maxY = Math.max(startY, endY);
        if (maxX <= minX || maxY <= minY) {
            return;
        }
        EngineConstraints.requireIntegerScale(borderThickness, "Selection.render(borderThickness)");
        float thickness = Math.max(1f, borderThickness);
        overlay.drawOutlineRect(minX, minY, maxX, maxY, thickness);
    }
}
