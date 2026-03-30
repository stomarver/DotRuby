package engine.visual.scene;

import engine.visual.Overlay;
import engine.visual.Render;
import engine.util.ResourceDisposer;

public final class PortalGridScene implements Scene {

    private float phase;

    @Override
    public String id() {
        return "scene.portal-grid";
    }

    @Override
    public void initialize(ResourceDisposer resources) {
        phase = 0f;
    }

    @Override
    public void update(float deltaSeconds) {
        phase += deltaSeconds;
    }

    @Override
    public void render(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "Prototype Portal Grid", 16f, 16f, 1f);

        int columns = 10;
        int rows = 6;
        float tileWidth = 960f / columns;
        float tileHeight = 540f / rows;
        float pulse = 1.0f + ((float) Math.sin(phase * 2f) * 0.5f);
        float thickness = Math.max(1f, 1f * pulse);

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                float minX = x * tileWidth;
                float minY = y * tileHeight;
                float maxX = minX + tileWidth;
                float maxY = minY + tileHeight;
                overlay.drawOutlineRect(minX + 2f, minY + 2f, maxX - 2f, maxY - 2f, thickness);
            }
        }
    }

    @Override
    public void destroy() {
    }
}
