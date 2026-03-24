package engine.ui.text;

import engine.ui.text.font.Regular;
import engine.visual.Overlay;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class Text {

    private static final List<Path> TEXTURE_PATHS = List.of(
            Path.of("src/assets/ui/font/regular.png"),
            Path.of("src/main/resources/fonts/font.png")
    );
    private static final String TEST_LABEL = "YOLOOO xoxoxo";
    private static final float LEFT_PADDING = 10f;
    private static final float TOP_PADDING = 2f;
    private static final float BASE_SCALE = 2f;

    private final Regular regular = new Regular();
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

        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer pixels = stbi_load(texturePath.toString(), width, height, channels, 4);
            if (pixels == null) {
                throw new IllegalStateException("Unable to load regular font texture: " + stbi_failure_reason());
            }

            textureWidth = width.get(0);
            textureHeight = height.get(0);
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth, textureHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glBindTexture(GL_TEXTURE_2D, 0);
            stbi_image_free(pixels);
        }
    }

    public void render(Overlay overlay) {
        if (textureId == 0) {
            throw new IllegalStateException("Regular font texture is not loaded");
        }

        draw(overlay, TEST_LABEL, LEFT_PADDING, TOP_PADDING, 1f);
    }

    public void draw(Overlay overlay, String value, float x, float y) {
        draw(overlay, value, x, y, 1f);
    }

    public void draw(Overlay overlay, String value, float x, float y, float size) {
        if (value == null || value.isBlank()) {
            return;
        }

        float resolvedScale = Math.max(0.0001f, size) * BASE_SCALE;
        List<Regular.Quad> quads = regular.parse(value);
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
