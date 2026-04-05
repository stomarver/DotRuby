package engine.visual;

import ui.text.Parse;

import java.nio.file.Path;
import java.util.List;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

public final class Render {

    private static final List<Path> FONT_DEFINITION_PATHS = List.of(
            Path.of("src/java/ui/text/font/Regular"),
            Path.of("src/main/resources/fonts/Regular")
    );
    private static final float BASE_SCALE = 2f;

    private final TextureLoader textureLoader = new TextureLoader();
    private Parse.FontDefinition font;
    private int textureId;
    private int textureWidth;
    private int textureHeight;

    public void load() {
        if (textureId != 0) {
            return;
        }

        Path fontPath = existingPath(FONT_DEFINITION_PATHS);
        if (fontPath == null) {
            throw new IllegalStateException("Regular font definition is not found: " + FONT_DEFINITION_PATHS);
        }
        font = Parse.font(fontPath);

        TextureLoader.LoadedTexture loadedTexture = textureLoader.loadNearestRgbaTexture(List.of(
                Path.of(font.bitmapPath()),
                Path.of("src/asset/ui/font/Regular.png"),
                Path.of("src/main/resources/fonts/font.png")
        ));
        textureId = loadedTexture.id();
        textureWidth = loadedTexture.width();
        textureHeight = loadedTexture.height();
    }

    public void drawText(Overlay overlay, String value, float x, float y) {
        drawText(overlay, value, x, y, 1f);
    }

    public void drawText(Overlay overlay, String value, float x, float y, float size) {
        if (textureId == 0) {
            throw new IllegalStateException("Regular font texture is not loaded");
        }
        if (value == null || value.isBlank()) {
            return;
        }
        if (font == null) {
            throw new IllegalStateException("Regular font definition is not loaded");
        }

        float resolvedScale = Math.max(0.0001f, size) * BASE_SCALE;
        List<Parse.Quad> quads = Parse.text(font, value);
        for (Parse.Quad quad : quads) {
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
                    false
            );
        }
    }

    public void destroy() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
        font = null;
    }

    private static Path existingPath(List<Path> candidates) {
        for (Path path : candidates) {
            if (path != null && path.toFile().exists()) {
                return path;
            }
        }
        return null;
    }
}
