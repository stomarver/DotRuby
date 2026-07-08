package engine.visual.scene.temp;

import engine.visual.Overlay;
import engine.visual.Render;

import java.util.ArrayList;
import java.util.List;

public final class SceneEngine {

    private final TextLayoutEngine text = new TextLayoutEngine();
    private final List<Section> sections = new ArrayList<>();
    private String title = "";
    private String subtitle = "";
    private float timeSeconds;

    public SceneEngine clear() {
        sections.clear();
        title = "";
        subtitle = "";
        timeSeconds = 0f;
        return this;
    }

    public SceneEngine title(String title, String subtitle) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        return this;
    }

    public SceneEngine section(String heading, String body, List<String> bullets) {
        sections.add(new Section(heading, body, List.copyOf(bullets)));
        return this;
    }

    public void update(float deltaSeconds) {
        timeSeconds += Math.max(0f, deltaSeconds);
    }

    public void render(Overlay overlay, Render render, float x, float y, float width) {
        float penY = text.drawHeading(overlay, render, title, x, y);
        penY = text.drawParagraph(overlay, render, subtitle, x, penY, width, 1f) + 14f;

        for (Section section : sections) {
            penY = text.drawLine(overlay, render, section.heading(), x, penY, 1f) + 4f;
            penY = text.drawParagraph(overlay, render, section.body(), x + 16f, penY, width - 16f, 1f) + 4f;
            penY = text.drawBullets(overlay, render, section.bullets(), x + 16f, penY, width - 16f) + 10f;
        }
    }

    public void renderStatus(Overlay overlay, Render render, float x, float y) {
        int phase = (int) (timeSeconds * 3f) % 4;
        String cursor = switch (phase) {
            case 0 -> "|";
            case 1 -> "/";
            case 2 -> "-";
            default -> "\\";
        };
        render.drawText(overlay, "scene-clock " + cursor + " " + (int) timeSeconds + "s", x, y, 1f, Render.TextScale.fixed());
    }

    private record Section(String heading, String body, List<String> bullets) {
    }
}
