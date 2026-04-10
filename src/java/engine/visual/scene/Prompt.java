package engine.visual.scene;

import engine.visual.Overlay;
import engine.visual.Render;

public final class Prompt extends SceneTemplate {

    public Prompt() {
        super(SceneIds.LOG_PROMPT, SceneType.TWO_D);
    }

    @Override
    public void render2D(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "Standard x1.0", 16f, 16f, 1f, Render.TextScale.standard());
        textRender.drawText(overlay, "Standard x1.5", 16f, 32f, 1.5f, Render.TextScale.standard());
        textRender.drawText(overlay, "Standard x2.0", 16f, 52f, 2f, Render.TextScale.standard());

        textRender.drawText(overlay, "Fixed scale 1.0", 16f, 88f, 1f, Render.TextScale.fixed(1f));
        textRender.drawText(overlay, "Fixed scale 2.0", 16f, 104f, 1f, Render.TextScale.fixed(2f));
        textRender.drawText(overlay, "Fixed scale 3.0", 16f, 124f, 1f, Render.TextScale.fixed(3f));

        textRender.drawText(overlay, "Relative -1.0", 16f, 160f, 1f, Render.TextScale.relative(-1f));
        textRender.drawText(overlay, "Relative +0.0", 16f, 176f, 1f, Render.TextScale.relative(0f));
        textRender.drawText(overlay, "Relative +1.0", 16f, 192f, 1f, Render.TextScale.relative(1f));

        textRender.drawText(overlay, "Press L for Log", 16f, 230f, 1f, Render.TextScale.standard());
    }
}
