package engine.visual.scene.temp;

import engine.util.resource.Unloader;
import engine.visual.Overlay;
import engine.visual.Render;
import engine.visual.scene.SceneIds;
import engine.visual.scene.SceneTemplate;

import java.util.List;

public final class LayoutLabScene extends SceneTemplate {

    private final SceneEngine sceneEngine = new SceneEngine();
    private final TextLayoutEngine text = new TextLayoutEngine();

    public LayoutLabScene() {
        super(SceneIds.LAYOUT_LAB);
    }

    @Override
    public void initialize(Unloader resources) {
        sceneEngine.clear()
                .title(
                        "DotRuby layout notebook",
                        "A plain 2D scene for testing text rhythm, document flow and future editor screens. No stage props: just readable blocks on the virtual grid."
                )
                .section(
                        "01 scene engine",
                        "SceneEngine now behaves like a tiny document runner: title, sections, body text, bullets and a clock. It is intentionally boring, because boring structure is reusable.",
                        List.of("keeps scene text grouped", "renders through one predictable pass", "can evolve into data-loaded scene descriptions")
                )
                .section(
                        "02 text layout engine",
                        "TextLayoutEngine is the place for columns, wrapping, rules and key-value rows. It should become the tool that makes bitmap text pleasant instead of chaotic.",
                        List.of("wraps words inside a measured width", "supports left center and right alignment", "keeps scale decisions explicit")
                )
                .section(
                        "03 next useful target",
                        "The immediate useful target is not a flashy demo. It is a small menu/editor surface where assets, fonts and scene descriptions can be inspected without touching Java code.",
                        List.of("scene browser", "font specimen page", "asset validation screen")
                );
    }

    @Override
    public void update(float deltaSeconds) {
        sceneEngine.update(deltaSeconds);
    }

    @Override
    public void render2D(Overlay overlay, Render textRender) {
        sceneEngine.render(overlay, textRender, 32f, 28f, 560f);
        renderSpecimenColumn(overlay, textRender, 640f, 52f, 280f);
        sceneEngine.renderStatus(overlay, textRender, 640f, 500f);
        textRender.drawText(overlay, "F1 prompt | F2 layout notebook", 16f, 524f, 1f, Render.TextScale.fixed());
    }

    private void renderSpecimenColumn(Overlay overlay, Render textRender, float x, float y, float width) {
        float penY = text.drawLine(overlay, textRender, "type specimen", x, y, width, 2f, TextLayoutEngine.Align.CENTER) + 10f;
        penY = text.drawRule(overlay, textRender, x, penY, 32) + 10f;
        penY = text.drawKeyValue(overlay, textRender, "standard", "scales with requested size", x, penY, 88f);
        penY = text.drawKeyValue(overlay, textRender, "fixed", "stays tied to virtual grid", x, penY, 88f);
        penY = text.drawKeyValue(overlay, textRender, "relative", "offsets configured scale", x, penY, 88f) + 14f;

        textRender.drawText(overlay, "AaBbCc 0123", x, penY, 1f, Render.TextScale.standard());
        penY += 18f;
        textRender.drawText(overlay, "AaBbCc 0123", x, penY, 2f, Render.TextScale.standard());
        penY += 28f;
        textRender.drawText(overlay, "fixed sample", x, penY, 1f, Render.TextScale.fixed());
        penY += 18f;
        textRender.drawText(overlay, "relative +1", x, penY, 1f, Render.TextScale.relative(1f));
        penY += 30f;

        text.drawParagraph(
                overlay,
                textRender,
                "This column is deliberately quiet: it tests spacing, wrapping and scale without pretending to be a finished interface.",
                x,
                penY,
                width,
                1f
        );
    }
}
