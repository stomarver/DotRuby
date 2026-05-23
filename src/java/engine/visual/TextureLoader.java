package engine.visual;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.io.IOException;
import java.io.InputStream;
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
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class TextureLoader {

    public record LoadedTexture(int id, int width, int height) {
    }


    public LoadedTexture loadNearestRgbaTextureByResource(List<String> resourcePaths) {
        for (String resourcePath : resourcePaths) {
            if (resourcePath == null || resourcePath.isBlank()) {
                continue;
            }
            LoadedTexture loaded = loadNearestRgbaTextureFromResource(resourcePath);
            if (loaded != null) {
                return loaded;
            }
        }
        throw new IllegalStateException("Texture resource is missing. Checked: " + resourcePaths);
    }

    public LoadedTexture loadNearestRgbaTextureFromResource(String resourcePath) {
        String normalized = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream stream = TextureLoader.class.getResourceAsStream(normalized)) {
            if (stream == null) {
                return null;
            }
            byte[] bytes = stream.readAllBytes();
            ByteBuffer encoded = org.lwjgl.BufferUtils.createByteBuffer(bytes.length);
            encoded.put(bytes).flip();
            return loadNearestRgbaTextureFromEncodedMemory(encoded, normalized);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read texture resource: " + normalized, exception);
        }
    }

    public LoadedTexture loadNearestRgbaTexture(List<Path> texturePaths, List<String> resourcePaths) {
        for (Path candidate : texturePaths) {
            if (Files.exists(candidate)) {
                return loadNearestRgbaTexture(candidate);
            }
        }
        return loadNearestRgbaTextureByResource(resourcePaths);
    }
    public LoadedTexture loadNearestRgbaTexture(List<Path> texturePaths) {
        for (Path candidate : texturePaths) {
            if (Files.exists(candidate)) {
                return loadNearestRgbaTexture(candidate);
            }
        }
        throw new IllegalStateException("Texture is missing. Checked: " + texturePaths);
    }

    public LoadedTexture loadNearestRgbaTexture(Path texturePath) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer pixels = stbi_load(texturePath.toString(), width, height, channels, 4);
            if (pixels == null) {
                throw new IllegalStateException("Unable to load texture: " + texturePath + " | " + stbi_failure_reason());
            }

            return uploadLoadedTexture(pixels, width.get(0), height.get(0), texturePath.toString());
        }
    }

    private LoadedTexture loadNearestRgbaTextureFromEncodedMemory(ByteBuffer encoded, String label) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer pixels = stbi_load_from_memory(encoded, width, height, channels, 4);
            if (pixels == null) {
                throw new IllegalStateException("Unable to load texture resource: " + label + " | " + stbi_failure_reason());
            }

            return uploadLoadedTexture(pixels, width.get(0), height.get(0), label);
        }
    }

    private LoadedTexture uploadLoadedTexture(ByteBuffer pixels, int textureWidth, int textureHeight, String label) {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth, textureHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glBindTexture(GL_TEXTURE_2D, 0);
        stbi_image_free(pixels);

        System.out.printf("[TextureLoader] Loaded texture: %s | id=%d | size=%dx%d%n",
                label, textureId, textureWidth, textureHeight);
        return new LoadedTexture(textureId, textureWidth, textureHeight);
    }
}
