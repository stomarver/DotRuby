package engine.display;

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
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glVertex2f;
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
    private double lastPhysicalX;
    private double lastPhysicalY;
    private boolean physicalTrackingInitialized;
    private int textureId;
    private int textureWidth;
    private int textureHeight;

    public void setState(long windowHandle, State newState) {
        if (newState == null) {
            return;
        }

        state = newState;
        glfwSetInputMode(windowHandle, GLFW_CURSOR, newState.glfwValue());
        resetMotionTracking();
    }

    public State getState() {
        return state;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setClampedPosition(double x, double y, int virtualWidth, int virtualHeight) {
        this.x = clampX(x, virtualWidth);
        this.y = clampY(y, virtualHeight);
    }

    public void updateCapturedPosition(double physicalX, double physicalY, int virtualWidth, int virtualHeight) {
        if (!physicalTrackingInitialized) {
            lastPhysicalX = physicalX;
            lastPhysicalY = physicalY;
            physicalTrackingInitialized = true;
            return;
        }

        double deltaX = physicalX - lastPhysicalX;
        double deltaY = physicalY - lastPhysicalY;
        lastPhysicalX = physicalX;
        lastPhysicalY = physicalY;

        setClampedPosition(x + deltaX, y + deltaY, virtualWidth, virtualHeight);
    }

    public void resetMotionTracking() {
        physicalTrackingInitialized = false;
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

    public int getTextureId() {
        return textureId;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public void loadTexture() {
        if (!Files.exists(TEXTURE_PATH)) {
            throw new IllegalStateException("Cursor texture is missing: " + TEXTURE_PATH);
        }
        if (textureId != 0) {
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
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth, textureHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glBindTexture(GL_TEXTURE_2D, 0);
            stbi_image_free(pixels);
        }
    }

    public void render(int renderWidth, int renderHeight) {
        if (textureId == 0) {
            throw new IllegalStateException("Cursor texture is not loaded");
        }

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, renderWidth, renderHeight, 0.0, -1.0, 1.0);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glColor4f(1f, 1f, 1f, 1f);

        float drawX = (float) x;
        float drawY = (float) y;
        float drawWidth = Math.max(1, textureWidth);
        float drawHeight = Math.max(1, textureHeight);

        glBegin(GL_QUADS);
        glTexCoord2f(0f, 0f); glVertex2f(drawX, drawY);
        glTexCoord2f(1f, 0f); glVertex2f(drawX + drawWidth, drawY);
        glTexCoord2f(1f, 1f); glVertex2f(drawX + drawWidth, drawY + drawHeight);
        glTexCoord2f(0f, 1f); glVertex2f(drawX, drawY + drawHeight);
        glEnd();

        glBindTexture(GL_TEXTURE_2D, 0);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    public void destroy() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
    }

    private double clampX(double value, int virtualWidth) {
        double maxX = Math.max(0, virtualWidth - 1);
        return Math.max(0, Math.min(Math.round(value), maxX));
    }

    private double clampY(double value, int virtualHeight) {
        double maxY = Math.max(0, virtualHeight - 1);
        return Math.max(0, Math.min(Math.round(value), maxY));
    }
}
