package engine.visual.scene.temp;

import engine.visual.Overlay;
import engine.visual.Render;
import engine.visual.scene.SceneIds;
import engine.visual.scene.SceneTemplate;
import engine.visual.scene.SceneType;

public final class Prompt extends SceneTemplate {

    public Prompt() {
        super(SceneIds.LOG_PROMPT, SceneType.TWO_D);
    }

    @Override
    public void render2D(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "^2DotRuby^0 text showcase", 16f, 16f, 1f, Render.TextScale.standard(1f));
        textRender.drawText(overlay, "Legacy scale model: 1->x2, 2->x4", 16f, 44f, 1f, Render.TextScale.standard(1f));

        textRender.drawText(overlay, "STANDARD", 16f, 88f, 1f, Render.TextScale.standard(1f));
        textRender.drawText(overlay, "scale 1", 32f, 112f, 1f, Render.TextScale.standard(1f));
        textRender.drawText(overlay, "scale 2", 32f, 140f, 1f, Render.TextScale.standard(2f));

        textRender.drawText(overlay, "FIXED (follows config)", 360f, 88f, 1f, Render.TextScale.standard(1f));
        textRender.drawText(overlay, "fixed 1", 376f, 112f, 1f, Render.TextScale.fixed());
        textRender.drawText(overlay, "fixed 2", 376f, 140f, 2f, Render.TextScale.fixed());

        textRender.drawText(overlay, "RELATIVE", 720f, 88f, 1f, Render.TextScale.standard(1f));
        textRender.drawText(overlay, "relative -1", 736f, 112f, 1f, Render.TextScale.relative(-1f));
        textRender.drawText(overlay, "relative +0", 736f, 140f, 1f, Render.TextScale.relative(0f));
        textRender.drawText(overlay, "relative +1", 736f, 168f, 1f, Render.TextScale.relative(1f));

        textRender.drawText(overlay, "0123456789 !?@#$%^&* /()[]{}<>", 16f, 230f, 1f, Render.TextScale.standard(1f));
        textRender.drawText(overlay, "The quick brown fox jumps over the lazy dog", 16f, 258f, 1f, Render.TextScale.standard(1f));
        textRender.drawText(overlay, "F1 Text  |  F2 Cube  |  F3 Lighting", 16f, 500f, 1f, Render.TextScale.standard(1f));
    }
}
