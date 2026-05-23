package engine.visual;

import engine.util.EngineConstraints;
import ui.text.Parse;

import java.nio.file.Path;
import java.util.Optional;
import java.util.List;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

public final class Render {

    private static final List<Path> FONT_DEFINITION_PATHS = List.of(
            Path.of("src/asset/ui/font/Regular.fnt"),
            Path.of("src/java/ui/text/font/Regular"),
            Path.of("src/main/resources/fonts/Regular")
    );
    private static final float BASE_SCALE = 1f;
    // New intuitive logical scale model:
    // 1 -> old 2, 2 -> old 3, 3 -> old 4, ...
    private static final float LOGICAL_SCALE_OFFSET = 1f;
    private static final float SHADOW_ALPHA = 0.5f;
    private float configuredVirtualScale = 1f;

    public enum ScaleMode {
        STANDARD,
        FIXED,
        RELATIVE
    }

    public record TextScale(ScaleMode mode, float value) {
        public static TextScale standard() {
            return standard(1f);
        }

        public static TextScale standard(float standardScale) {
            return new TextScale(ScaleMode.STANDARD, standardScale);
        }

        public static TextScale fixed() {
            return new TextScale(ScaleMode.FIXED, 0f);
        }

        public static TextScale relative(float relativeOffset) {
            return new TextScale(ScaleMode.RELATIVE, relativeOffset);
        }
    }

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
        if (fontPath != null) {
            font = Parse.font(fontPath);
        } else {
            font = Parse.font(List.of("ui/font/Regular.fnt"));
        }

        TextureLoader.LoadedTexture loadedTexture = textureLoader.loadNearestRgbaTexture(List.of(
                Path.of(font.bitmapPath()),
                Path.of("src/asset/ui/font/Regular.png"),
                Path.of("src/main/resources/fonts/font.png")
        ), List.of("ui/font/Regular.png"));
        textureId = loadedTexture.id();
        textureWidth = loadedTexture.width();
        textureHeight = loadedTexture.height();
    }

    public void drawText(Overlay overlay, String value, float x, float y) {
        drawText(overlay, value, x, y, 1f, TextScale.standard());
    }

    public void drawText(Overlay overlay, String value, float x, float y, float size) {
        drawText(overlay, value, x, y, size, TextScale.standard());
    }

    public void drawText(Overlay overlay, String value, float x, float y, float size, TextScale textScale) {
        if (textureId == 0) {
            throw new IllegalStateException("Regular font texture is not loaded");
        }
        if (value == null || value.isBlank()) {
            return;
        }
        if (font == null) {
            throw new IllegalStateException("Regular font definition is not loaded");
        }

        EngineConstraints.requireIntegerScale(size, "Render.drawText(size)");
        if (textScale != null) {
            EngineConstraints.requireIntegerScale(textScale.value(), "Render.drawText(textScale)");
        }
        float resolvedScale = Math.max(0.0001f, size) * BASE_SCALE * resolveScaleMultiplier(textScale);
        float shadowOffsetVirtual = resolvedScale;
        List<Parse.Quad> quads = Parse.text(font, value);
        for (Parse.Quad quad : quads) {
            float minU = quad.glyph().atlasX() / (float) textureWidth;
            float minV = quad.glyph().atlasY() / (float) textureHeight;
            float maxU = (quad.glyph().atlasX() + quad.glyph().atlasWidth()) / (float) textureWidth;
            float maxV = (quad.glyph().atlasY() + quad.glyph().atlasHeight()) / (float) textureHeight;

            float drawX = x + (quad.drawX() * resolvedScale);
            float drawY = y + (quad.drawY() * resolvedScale);
            float drawWidth = quad.glyph().atlasWidth() * resolvedScale;
            float drawHeight = quad.glyph().atlasHeight() * resolvedScale;

            overlay.drawTexturedQuadRegionTint(
                    textureId,
                    drawX + shadowOffsetVirtual,
                    drawY + shadowOffsetVirtual,
                    drawWidth,
                    drawHeight,
                    minU,
                    minV,
                    maxU,
                    maxV,
                    0f,
                    0f,
                    0f,
                    SHADOW_ALPHA,
                    false
            );
            overlay.drawTexturedQuadRegion(
                    textureId,
                    drawX,
                    drawY,
                    drawWidth,
                    drawHeight,
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

    public void setConfiguredVirtualScale(float configuredVirtualScale) {
        this.configuredVirtualScale = Math.max(0.0001f, configuredVirtualScale);
    }

    private float resolveScaleMultiplier(TextScale textScale) {
        TextScale resolved = textScale == null ? TextScale.standard() : textScale;
        float configScale = Math.max(0.0001f, configuredVirtualScale);

        float logicalTarget = switch (resolved.mode()) {
            case FIXED -> configScale;
            case RELATIVE -> Math.max(0.0001f, configScale + resolved.value());
            case STANDARD -> Math.max(0.0001f, resolved.value());
        };

        float physicalTarget = toPhysicalScale(logicalTarget);
        return physicalTarget / configScale;
    }

    private float toPhysicalScale(float logicalScale) {
        return Math.max(0.0001f, logicalScale + LOGICAL_SCALE_OFFSET);
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
