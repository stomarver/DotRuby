package engine.visual.scene;

import engine.visual.Overlay;
import engine.visual.Render;

public final class LogPromptScene implements Scene {

    @Override
    public String id() {
        return "scene.log-prompt";
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update(float deltaSeconds) {
    }

    @Override
    public void render(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "Press L for Log", 16f, 16f, 1f);
    }

    @Override
    public void destroy() {
    }
}
