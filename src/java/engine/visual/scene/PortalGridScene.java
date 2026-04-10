package engine.visual.scene;

import engine.visual.Overlay;
import engine.visual.Render;
import engine.util.resource.Unloader;

public final class PortalGridScene extends SceneTemplate {

    private float phase;
    private final TriangleRenderer3D triangleRenderer = new TriangleRenderer3D();

    public PortalGridScene() {
        super(SceneIds.PORTAL_GRID, SceneType.TWO_D_AND_THREE_D);
    }

    @Override
    public void initialize(Unloader resources) {
        phase = 0f;
        triangleRenderer.init();
        resources.track(triangleRenderer::destroy);
    }

    @Override
    public void update(float deltaSeconds) {
        phase += deltaSeconds;
    }

    @Override
    public void render3D() {
        float pulse = 0.35f + ((float) Math.sin(phase) * 0.15f);
        triangleRenderer.draw(new float[] {
                0f, pulse, 0f, 0.4f, 0.8f, 1.0f,
                -pulse, -pulse, 0f, 0.4f, 0.8f, 1.0f,
                pulse, -pulse, 0f, 0.4f, 0.8f, 1.0f
        });
    }

    @Override
    public void render2D(Overlay overlay, Render textRender) {
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

}
