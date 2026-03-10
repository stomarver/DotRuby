package engine.util;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

public final class Screenshot {

    private Screenshot() {
    }

    public static File takeFromFramebuffer(int framebufferId, int width, int height) {
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (x + (width * y)) * 4;
                int r = pixels.get(i) & 0xFF;
                int g = pixels.get(i + 1) & 0xFF;
                int b = pixels.get(i + 2) & 0xFF;
                image.setRGB(x, height - (y + 1), (r << 16) | (g << 8) | b);
            }
        }

        return write(image);
    }

    private static File write(BufferedImage image) {
        try {
            File file = nextFile();
            ImageIO.write(image, "png", file);
            return file;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save screenshot", e);
        }
    }

    private static File nextFile() {
        String home = System.getProperty("user.home");
        File folder = new File(home, "Pictures" + File.separator + "DotRuby");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Unable to create screenshot directory: " + folder.getAbsolutePath());
        }

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File target = new File(folder, stamp + ".png");
        int index = 1;
        while (target.exists()) {
            target = new File(folder, stamp + "_" + index + ".png");
            index++;
        }
        return target;
    }
}
