package engine.display;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
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

public class Cursor {

    private static final Path TEXTURE_PATH = Path.of("src/assets/ui/cursor/classic.png");

    public enum State {
        NORMAL(GLFW_CURSOR_NORMAL),
        HIDDEN(GLFW_CURSOR_HIDDEN),
        CAPTURED(GLFW_CURSOR_DISABLED);

        private final int glfwValue;

        State(int glfwValue) {
            this.glfwValue = glfwValue;
        }

        public int glfwValue() {
            return glfwValue;
        }
    }

    private final Set<Integer> pressedButtons = new HashSet<>();
    private State state = State.NORMAL;
    private double x;
    private double y;
    private int textureId;
    private int textureWidth;
    private int textureHeight;

    public void setState(long windowHandle, State newState) {
        if (newState == null) {
            return;
        }

        state = newState;
        glfwSetInputMode(windowHandle, GLFW_CURSOR, newState.glfwValue());
    }

    public State getState() {
        return state;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setButtonState(int button, boolean pressed) {
        if (pressed) {
            pressedButtons.add(button);
        } else {
            pressedButtons.remove(button);
        }
    }

    public boolean isPressed(int button) {
        return pressedButtons.contains(button);
    }

    public boolean hasTexture() {
        return textureId != 0;
    }

    public int getTextureId() {
        return textureId;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public void loadTextureIfPresent() {
        if (textureId != 0 || !Files.exists(TEXTURE_PATH)) {
            return;
        }

        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer pixels = stbi_load(TEXTURE_PATH.toString(), width, height, channels, 4);
            if (pixels == null) {
                throw new IllegalStateException("Unable to load cursor texture: " + stbi_failure_reason());
            }

            textureWidth = width.get(0);
            textureHeight = height.get(0);
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth, textureHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glBindTexture(GL_TEXTURE_2D, 0);
            stbi_image_free(pixels);
        }
    }

    public void destroy() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
    }
}
