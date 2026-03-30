package engine.ui;

import engine.visual.Overlay;
import engine.visual.TextureLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;

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
    private final TextureLoader textureLoader = new TextureLoader();
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

    public void updateCapturedPosition(double physicalX,
                                       double physicalY,
                                       float physicalPixelsPerVirtualX,
                                       float physicalPixelsPerVirtualY,
                                       int virtualWidth,
                                       int virtualHeight) {
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

        double scaleX = Math.max(physicalPixelsPerVirtualX, 0.0001f);
        double scaleY = Math.max(physicalPixelsPerVirtualY, 0.0001f);
        setClampedPosition(x + (deltaX / scaleX), y + (deltaY / scaleY), virtualWidth, virtualHeight);
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

        TextureLoader.LoadedTexture loadedTexture = textureLoader.loadNearestRgbaTexture(TEXTURE_PATH);
        textureId = loadedTexture.id();
        textureWidth = loadedTexture.width();
        textureHeight = loadedTexture.height();
    }

    public void render(Overlay overlay, float drawWidth, float drawHeight) {
        if (textureId == 0) {
            throw new IllegalStateException("Cursor texture is not loaded");
        }

        glDisable(GL_DEPTH_TEST);
        float drawX = (float) Math.floor(x);
        float drawY = (float) Math.floor(y);
        drawWidth = Math.max(1f, drawWidth);
        drawHeight = Math.max(1f, drawHeight);
        overlay.drawTexturedQuad(textureId, drawX, drawY, drawWidth, drawHeight);
    }

    public void destroy() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
    }

    private double clampX(double value, int virtualWidth) {
        double maxX = Math.max(0, virtualWidth - 1);
        return Math.max(0, Math.min(value, maxX));
    }

    private double clampY(double value, int virtualHeight) {
        double maxY = Math.max(0, virtualHeight);
        return Math.max(0, Math.min(value, maxY));
    }
}
