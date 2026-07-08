package engine.visual.scene.temp;

import engine.util.resource.Unloader;
import engine.visual.Overlay;
import engine.visual.Render;
import engine.visual.scene.SceneIds;
import engine.visual.scene.SceneTemplate;
import engine.visual.scene.SceneType;

public final class LayoutLabScene extends SceneTemplate {

    private final SceneEngine sceneEngine = new SceneEngine();
    private final TextLayoutEngine titleColumn = new TextLayoutEngine();
    private final TextLayoutEngine rightColumn = new TextLayoutEngine();
    private float elapsedSeconds;

    public LayoutLabScene() {
        super(SceneIds.LAYOUT_LAB, SceneType.TWO_D);
    }

    @Override
    public void initialize(Unloader resources) {
        elapsedSeconds = 0f;
        sceneEngine.clear()
                .panel(14f, 14f, 932f, 512f)
                .panel(40f, 96f, 392f, 336f)
                .panel(472f, 96f, 432f, 336f)
                .label("SCENE ENGINE", 64f, 116f, 2f)
                .label("TEXT LAYOUT ENGINE", 496f, 116f, 2f);

        titleColumn.clear().lineGap(8f)
                .add("scene 2: layout lab", 3f)
                .add("no cube. no portal. no fake 3d.", 2f)
                .add("only flat, stubborn, ruby-like pixels.", 1f, Render.TextScale.fixed());

        rightColumn.clear().lineGap(10f)
                .add("left aligned", 1f)
                .add("center aligned", 1f)
                .add("right aligned", 1f)
                .add("fixed-size text ignores virtual scale", 1f, Render.TextScale.fixed())
                .add("relative text grows from config scale", 1f, Render.TextScale.relative(1f));
    }

    @Override
    public void update(float deltaSeconds) {
        elapsedSeconds += Math.max(0f, deltaSeconds);
        sceneEngine.update(deltaSeconds);
    }

    @Override
    public void render2D(Overlay overlay, Render textRender) {
        sceneEngine.render(overlay, textRender);
        titleColumn.drawColumn(overlay, textRender, 40f, 36f);

        TextLayoutEngine wrapped = new TextLayoutEngine().lineGap(5f);
        wrapped.drawWrapped(
                overlay,
                textRender,
                "SceneEngine collects panels and labels; TextLayoutEngine places columns wraps words and aligns blocks for future editor screens.",
                64f,
                156f,
                320f,
                1f
        );

        rightColumn.drawColumn(overlay, textRender, 520f, 156f, 300f, TextLayoutEngine.Align.LEFT);
        rightColumn.drawColumn(overlay, textRender, 520f, 260f, 300f, TextLayoutEngine.Align.CENTER);
        rightColumn.drawColumn(overlay, textRender, 520f, 364f, 300f, TextLayoutEngine.Align.RIGHT);

        int seconds = (int) elapsedSeconds;
        textRender.drawText(overlay, "F1 prompt | F2 layout lab", 16f, 500f, 2f, Render.TextScale.standard());
        textRender.drawText(overlay, "flat scene runtime: " + seconds + "s", 656f, 500f, 1f, Render.TextScale.fixed());
        sceneEngine.renderPulse(overlay, textRender, 656f, 476f);
    }
}
