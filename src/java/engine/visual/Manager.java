package engine.visual;

public final class Manager {

    private final Render textRender = new Render();

    public void initialize() {
        textRender.load();
    }

    public void drawText(Overlay overlay, String text, float x, float y, float size) {
        textRender.drawText(overlay, text, x, y, size);
    }

    public void destroy() {
        textRender.destroy();
    }
}
