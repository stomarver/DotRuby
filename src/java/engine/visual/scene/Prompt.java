package engine.visual.scene;

import engine.visual.Overlay;
import engine.visual.Render;

public final class Prompt extends SceneTemplate {

    public Prompt() {
        super(SceneIds.LOG_PROMPT, SceneType.TWO_D);
    }

    @Override
    public void render2D(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "Press L for Log", 16f, 16f, 1f);
    }
}
