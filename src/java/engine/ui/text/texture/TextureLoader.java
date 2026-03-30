package engine.ui.text.texture;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

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

public final class TextureLoader {

    public record LoadedTexture(int id, int width, int height) {
    }

    public LoadedTexture loadNearestRgbaTexture(Path texturePath) {
        BufferedImage image = readImage(texturePath);
        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer pixels = toRgbaByteBuffer(image);
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glBindTexture(GL_TEXTURE_2D, 0);

        System.out.printf("[TextureLoader] Loaded texture: %s | id=%d | size=%dx%d%n",
                texturePath, textureId, width, height);
        return new LoadedTexture(textureId, width, height);
    }

    private BufferedImage readImage(Path texturePath) {
        try {
            BufferedImage image = ImageIO.read(texturePath.toFile());
            if (image == null) {
                throw new IllegalStateException("Image format is not supported: " + texturePath);
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read texture: " + texturePath, exception);
        }
    }

    private ByteBuffer toRgbaByteBuffer(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                buffer.put((byte) ((argb >> 16) & 0xFF));
                buffer.put((byte) ((argb >> 8) & 0xFF));
                buffer.put((byte) (argb & 0xFF));
                buffer.put((byte) ((argb >> 24) & 0xFF));
            }
        }

        buffer.flip();
        return buffer;
    }
}
