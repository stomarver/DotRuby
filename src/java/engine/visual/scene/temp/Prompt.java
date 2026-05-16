package engine.visual.scene.temp;

import engine.visual.scene.SceneIds;
import engine.visual.scene.SceneTemplate;
import engine.visual.scene.SceneType;

import engine.visual.Overlay;
import engine.visual.Render;

public final class Prompt extends SceneTemplate {

    public Prompt() {
        super(SceneIds.LOG_PROMPT, SceneType.TWO_D);
    }

    @Override
    public void render2D(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "Standard", 16f, 16f, 1f, Render.TextScale.standard());
        textRender.drawText(overlay, "Standard", 16f, 32f, 2f, Render.TextScale.standard());
        textRender.drawText(overlay, "Standard", 16f, 52f, 2f, Render.TextScale.standard());

        textRender.drawText(overlay, "Fixed", 16f, 88f, 1f, Render.TextScale.fixed(1f));
        textRender.drawText(overlay, "Fixed", 16f, 104f, 1f, Render.TextScale.fixed(2f));
        textRender.drawText(overlay, "Fixed", 16f, 124f, 1f, Render.TextScale.fixed(3f));

        textRender.drawText(overlay, "Relative", 16f, 160f, 1f, Render.TextScale.relative(-1f));
        textRender.drawText(overlay, "Relative", 16f, 176f, 1f, Render.TextScale.relative(0f));
        textRender.drawText(overlay, "Relative", 16f, 192f, 1f, Render.TextScale.relative(1f));

        textRender.drawText(overlay, "Press L for Log", 16f, 230f, 1f, Render.TextScale.standard());
    }
}
