package engine.ui.text;

import engine.ui.text.font.Regular;
import engine.visual.Overlay;
import engine.visual.TextureLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

public final class TextRenderer {

    private static final List<Path> TEXTURE_PATHS = List.of(
            Path.of("src/assets/ui/font/regular.png"),
            Path.of("src/main/resources/fonts/font.png")
    );
    private static final String TEST_LABEL =
            "Ancient keep records brave quests: Wizards mix jugs, vex nymphs, and forge crazy potions by twilight.\n" +
            "Старый замок хранит руны: Храбрый маг в тиши кует щит, меч, флягу, а юный вестник шлёт весть, АаБбВвГгДдЕеЖжЗзИиКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЪъЫыЬьЭэЮюЯя.";
    private static final float BASE_SCALE = 2f;

    private final Regular regularFont = new Regular();
    private final TextureLoader textureLoader = new TextureLoader();
    private int textureId;
    private int textureWidth;
    private int textureHeight;
    private Path texturePath;

    public void load() {
        if (textureId != 0) {
            return;
        }

        texturePath = resolveTexturePath();
        if (texturePath == null) {
            throw new IllegalStateException("Regular font texture is missing. Checked: " + TEXTURE_PATHS);
        }

        TextureLoader.LoadedTexture loadedTexture = textureLoader.loadNearestRgbaTexture(texturePath);
        textureId = loadedTexture.id();
        textureWidth = loadedTexture.width();
        textureHeight = loadedTexture.height();
    }

    public void render(Overlay overlay) {
        if (textureId == 0) {
            throw new IllegalStateException("Regular font texture is not loaded");
        }

        draw(overlay, TEST_LABEL, 0f, 0f, 1f);
    }

    public void draw(Overlay overlay, String value, float x, float y) {
        draw(overlay, value, x, y, 1f);
    }

    public void draw(Overlay overlay, String value, float x, float y, float size) {
        if (value == null || value.isBlank()) {
            return;
        }

        float resolvedScale = Math.max(0.0001f, size) * BASE_SCALE;
        List<Regular.Quad> quads = regularFont.parse(value);
        for (Regular.Quad quad : quads) {
            float minU = quad.glyph().atlasX() / (float) textureWidth;
            float minV = quad.glyph().atlasY() / (float) textureHeight;
            float maxU = (quad.glyph().atlasX() + quad.glyph().atlasWidth()) / (float) textureWidth;
            float maxV = (quad.glyph().atlasY() + quad.glyph().atlasHeight()) / (float) textureHeight;
            overlay.drawTexturedQuadRegion(
                    textureId,
                    x + (quad.drawX() * resolvedScale),
                    y + (quad.drawY() * resolvedScale),
                    quad.glyph().atlasWidth() * resolvedScale,
                    quad.glyph().atlasHeight() * resolvedScale,
                    minU,
                    minV,
                    maxU,
                    maxV,
                    true
            );
        }
    }

    public void destroy() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
    }

    private Path resolveTexturePath() {
        for (Path candidate : TEXTURE_PATHS) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
