package engine.display;

public final class Selection {

    private boolean active;
    private float startX;
    private float startY;
    private float endX;
    private float endY;

    public void begin(float x, float y) {
        active = true;
        startX = x;
        startY = y;
        endX = x;
        endY = y;
    }

    public void update(float x, float y) {
        if (!active) {
            return;
        }
        endX = x;
        endY = y;
    }

    public void clear() {
        active = false;
    }

    public void render(OverlayRenderer overlayRenderer, float borderThickness) {
        if (!active) {
            return;
        }

        float minX = Math.min(startX, endX);
        float maxX = Math.max(startX, endX);
        float minY = Math.min(startY, endY);
        float maxY = Math.max(startY, endY);
        minX = (float) Math.floor(minX);
        maxX = (float) Math.floor(maxX);
        minY = (float) Math.floor(minY);
        maxY = (float) Math.floor(maxY);
        if (maxX <= minX || maxY <= minY) {
            return;
        }
        float thickness = Math.max(0.1f, borderThickness);
        overlayRenderer.drawOutlineRect(minX, minY, maxX, maxY, thickness);
    }
}
