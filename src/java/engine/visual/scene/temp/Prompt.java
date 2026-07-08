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
        textRender.drawText(overlay, "Text Engine Demo", 16f, 16f, 3f, Render.TextScale.standard());
        textRender.drawText(overlay, "- Integer-only scale policy", 16f, 48f, 2f, Render.TextScale.standard());
        textRender.drawText(overlay, "- Shadowed bitmap glyph rendering", 16f, 72f, 2f, Render.TextScale.standard());
        textRender.drawText(overlay, "- UTF-8 safe string pipeline", 16f, 96f, 2f, Render.TextScale.standard());

        textRender.drawText(overlay, "STANDARD MODE", 16f, 136f, 2f, Render.TextScale.standard());
        textRender.drawText(overlay, "Scale 1", 32f, 160f, 1f, Render.TextScale.standard(1f));
        textRender.drawText(overlay, "Scale 2", 32f, 180f, 1f, Render.TextScale.standard(2f));
        textRender.drawText(overlay, "Scale 3", 32f, 212f, 1f, Render.TextScale.standard(3f));

        textRender.drawText(overlay, "FIXED MODE", 360f, 136f, 2f, Render.TextScale.standard());
        textRender.drawText(overlay, "Fixed 1", 376f, 160f, 1f, Render.TextScale.fixed());
        textRender.drawText(overlay, "Fixed 2", 376f, 180f, 2f, Render.TextScale.fixed());
        textRender.drawText(overlay, "Fixed 3", 376f, 212f, 3f, Render.TextScale.fixed());

        textRender.drawText(overlay, "RELATIVE MODE", 680f, 136f, 2f, Render.TextScale.standard());
        textRender.drawText(overlay, "Relative -1", 696f, 160f, 1f, Render.TextScale.relative(-1f));
        textRender.drawText(overlay, "Relative +0", 696f, 180f, 1f, Render.TextScale.relative(0f));
        textRender.drawText(overlay, "Relative +1", 696f, 212f, 1f, Render.TextScale.relative(1f));

        textRender.drawText(overlay, "0123456789   !?@#$%^&*   /()[]{}<>", 16f, 280f, 2f, Render.TextScale.standard());
        textRender.drawText(overlay, "The quick brown fox jumps over the lazy dog", 16f, 308f, 2f, Render.TextScale.standard());

        textRender.drawText(overlay, "F1 Text  |  F2 Layout Lab", 16f, 500f, 2f, Render.TextScale.standard());
        textRender.drawText(overlay, "Press L for hardware/specs log | F2 opens layout lab", 16f, 524f, 1f, Render.TextScale.standard());
    }
}
