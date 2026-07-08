package engine.visual.scene.temp;

import engine.visual.Overlay;
import engine.visual.Render;

import java.util.ArrayList;
import java.util.List;

public final class SceneEngine {

    private final List<Panel> panels = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private float timeSeconds;

    public SceneEngine clear() {
        panels.clear();
        labels.clear();
        return this;
    }

    public SceneEngine panel(float x, float y, float width, float height) {
        panels.add(new Panel(x, y, width, height));
        return this;
    }

    public SceneEngine label(String value, float x, float y, float scale) {
        labels.add(new Label(value, x, y, scale));
        return this;
    }

    public void update(float deltaSeconds) {
        timeSeconds += Math.max(0f, deltaSeconds);
    }

    public void render(Overlay overlay, Render textRender) {
        for (Panel panel : panels) {
            drawPanel(overlay, panel);
        }
        for (Label label : labels) {
            textRender.drawText(overlay, label.value(), label.x(), label.y(), label.scale(), Render.TextScale.standard());
        }
    }

    public void renderPulse(Overlay overlay, Render textRender, float x, float y) {
        int dotCount = 1 + ((int) (timeSeconds * 2f) % 4);
        textRender.drawText(overlay, "engine tick" + ".".repeat(dotCount), x, y, 1f, Render.TextScale.fixed());
    }

    private static void drawPanel(Overlay overlay, Panel panel) {
        float x = panel.x();
        float y = panel.y();
        float right = x + panel.width();
        float bottom = y + panel.height();
        overlay.drawOutlineRect(x, y, right, bottom, 2f);
        overlay.drawTriangle(x + 8f, y + 8f, x + 22f, y + 8f, x + 8f, y + 22f);
        overlay.drawTriangle(right - 8f, bottom - 8f, right - 22f, bottom - 8f, right - 8f, bottom - 22f);
    }

    private record Panel(float x, float y, float width, float height) {
    }

    private record Label(String value, float x, float y, float scale) {
    }
}
