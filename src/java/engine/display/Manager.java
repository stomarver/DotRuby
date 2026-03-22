package engine.display;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_AUTO_ICONIFY;
import static org.lwjgl.glfw.GLFW.GLFW_DECORATED;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_FOCUS_ON_SHOW;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowAttrib;
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Manager {

    private final Config config;
    private final Cursor cursor = new Cursor();

    private long windowHandle;
    private Mode mode;
    private Fullscreen fullscreen;
    private VSync vSync;

    private final int virtualWidth;
    private final int virtualHeight;
    private int framebufferWidth;
    private int framebufferHeight;
    private int physicalX;
    private int physicalY;
    private int physicalWidth;
    private int physicalHeight;
    private boolean forceVirtualResolution = true;

    public Manager(Config config) {
        this.config = config;
        this.mode = config.getWindowMode();
        this.fullscreen = config.getFullscreen();
        this.vSync = config.getVSync();
        this.virtualWidth = config.getWidth();
        this.virtualHeight = config.getHeight();
        this.framebufferWidth = virtualWidth;
        this.framebufferHeight = virtualHeight;
        this.physicalWidth = virtualWidth;
        this.physicalHeight = virtualHeight;
    }

    public long createWindow() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW is not initialized");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, config.isResizable() ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_TRUE);

        windowHandle = glfwCreateWindow(config.getWidth(), config.getHeight(), config.getTitle(), NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create window");
        }

        Centering.center(windowHandle, Monitor.primary(config.getWidth(), config.getHeight()), config.getWidth(), config.getHeight());

        glfwMakeContextCurrent(windowHandle);
        applyVSync(vSync);
        glfwShowWindow(windowHandle);

        GL.createCapabilities();

        glClearColor(config.getClearR(), config.getClearG(), config.getClearB(), config.getClearA());
        glEnable(GL_DEPTH_TEST);
        cursor.loadTextureIfPresent();

        updateViewport();
        glfwSetFramebufferSizeCallback(windowHandle, (window, width, height) -> updateViewport());
        applyCursorLock();

        return windowHandle;
    }

    public void applyVSync(VSync vSync) {
        this.vSync = vSync == null ? VSync.DOUBLE_BUFFERED : vSync;
        glfwSwapInterval(this.vSync.getSwapInterval());
    }

    public void enableDoubleVSync() {
        applyVSync(VSync.DOUBLE_BUFFERED);
    }

    public void enableTripleVSync() {
        applyVSync(VSync.TRIPLE_BUFFERED);
    }

    public void disableVSync() {
        applyVSync(VSync.DISABLED);
    }

    public VSync getVSync() {
        return vSync;
    }

    public int getVirtualWidth() {
        return virtualWidth;
    }

    public int getVirtualHeight() {
        return virtualHeight;
    }

    public int getRenderWidth() {
        return forceVirtualResolution ? virtualWidth : Math.max(1, framebufferWidth);
    }

    public int getRenderHeight() {
        return forceVirtualResolution ? virtualHeight : Math.max(1, framebufferHeight);
    }

    public int getFramebufferWidth() {
        return Math.max(1, framebufferWidth);
    }

    public int getFramebufferHeight() {
        return Math.max(1, framebufferHeight);
    }

    public int getPhysicalX() {
        return physicalX;
    }

    public int getPhysicalY() {
        return physicalY;
    }

    public int getPhysicalWidth() {
        return physicalWidth;
    }

    public int getPhysicalHeight() {
        return physicalHeight;
    }

    public boolean isForceVirtualResolution() {
        return forceVirtualResolution;
    }

    public void setForceVirtualResolution(boolean forceVirtualResolution) {
        this.forceVirtualResolution = forceVirtualResolution;
        updateViewport();
    }

    public float getUiScaleToPhysicalPixels() {
        float rawScale = forceVirtualResolution
                ? (physicalWidth / (float) virtualWidth)
                : (framebufferWidth / (float) virtualWidth);

        if (forceVirtualResolution && mode == Mode.FULLSCREEN) {
            return Math.max(1f, Math.round(rawScale));
        }
        return rawScale;
    }

    public float getUiScaleToPhysicalPixelsExact() {
        return forceVirtualResolution
                ? (physicalWidth / (float) virtualWidth)
                : (framebufferWidth / (float) virtualWidth);
    }

    public float getVirtualUnitsForPhysicalPixels(float pixels) {
        float scale = getUiScaleToPhysicalPixels();
        return scale <= 0f ? pixels : pixels / scale;
    }

    public float getVirtualUnitsForPhysicalPixelsExact(float pixels) {
        float scale = getUiScaleToPhysicalPixelsExact();
        return scale <= 0f ? pixels : pixels / scale;
    }

    public float toVirtualX(double physicalScreenX) {
        float scale = forceVirtualResolution
                ? (physicalWidth / (float) virtualWidth)
                : (framebufferWidth / (float) virtualWidth);
        float offsetX = forceVirtualResolution ? physicalX : 0f;
        return (float) ((physicalScreenX - offsetX) / Math.max(scale, 0.0001f));
    }

    public float toVirtualY(double physicalScreenY) {
        float scale = forceVirtualResolution
                ? (physicalHeight / (float) virtualHeight)
                : (framebufferHeight / (float) virtualHeight);
        float offsetY = forceVirtualResolution ? physicalY : 0f;
        return (float) ((physicalScreenY - offsetY) / Math.max(scale, 0.0001f));
    }

    public float toPhysicalX(float virtualX) {
        float scale = forceVirtualResolution
                ? (physicalWidth / (float) virtualWidth)
                : (framebufferWidth / (float) virtualWidth);
        float offsetX = forceVirtualResolution ? physicalX : 0f;
        return offsetX + (virtualX * scale);
    }

    public float toPhysicalY(float virtualY) {
        float scale = forceVirtualResolution
                ? (physicalHeight / (float) virtualHeight)
                : (framebufferHeight / (float) virtualHeight);
        float offsetY = forceVirtualResolution ? physicalY : 0f;
        return offsetY + (virtualY * scale);
    }

    public int createVertexArray() {
        return glGenVertexArrays();
    }

    public int createVertexBuffer(float[] vertices) {
        return createVertexBuffer(vertices, GL_STATIC_DRAW);
    }

    public int createVertexBuffer(float[] vertices, int usage) {
        int bufferId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, bufferId);
        glBufferData(GL_ARRAY_BUFFER, vertices, usage);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return bufferId;
    }

    public void prepareVertexLayout(int vaoId, int vboId, int attributeIndex, int componentCount, int strideBytes, int offsetBytes) {
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glEnableVertexAttribArray(attributeIndex);
        glVertexAttribPointer(attributeIndex, componentCount, GL_FLOAT, false, strideBytes, offsetBytes);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void bindVertexArray(int vaoId) {
        glBindVertexArray(vaoId);
    }

    public void unbindVertexArray() {
        glBindVertexArray(0);
    }

    public void deleteVertexArray(int vaoId) {
        glDeleteVertexArrays(vaoId);
    }

    public void deleteVertexBuffer(int vboId) {
        glDeleteBuffers(vboId);
    }

    public void clearFrame() {
        glViewport(0, 0, getFramebufferWidth(), getFramebufferHeight());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void updateFrame() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.WINDOWED : mode;
        applyWindowMode();
        updateViewport();
    }

    public void setFullscreen(Fullscreen fullscreen) {
        this.fullscreen = fullscreen == null ? Fullscreen.BORDERLESS : fullscreen;
        if (mode == Mode.FULLSCREEN) {
            applyWindowMode();
            updateViewport();
        }
    }

    public void toggleFullscreen() {
        setMode(mode == Mode.WINDOWED ? Mode.FULLSCREEN : Mode.WINDOWED);
    }

    public Cursor getCursor() {
        return cursor;
    }

    public void applyCursorLock() {
        cursor.setState(windowHandle, config.isLockCursor() ? Cursor.State.CAPTURED : Cursor.State.NORMAL);
    }

    public Mode getMode() {
        return mode;
    }

    public Fullscreen getFullscreen() {
        return fullscreen;
    }

    public long getWindowHandle() {
        return windowHandle;
    }

    public void destroyWindow() {
        cursor.destroy();
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }
    }

    private void updateViewport() {
        int[] fbW = new int[1];
        int[] fbH = new int[1];
        glfwGetFramebufferSize(windowHandle, fbW, fbH);

        framebufferWidth = Math.max(1, fbW[0]);
        framebufferHeight = Math.max(1, fbH[0]);

        float targetAspect = virtualWidth / (float) virtualHeight;
        float windowAspect = framebufferWidth / (float) framebufferHeight;

        if (windowAspect > targetAspect) {
            physicalHeight = framebufferHeight;
            physicalWidth = (int) (framebufferHeight * targetAspect);
            physicalX = (framebufferWidth - physicalWidth) / 2;
            physicalY = 0;
        } else {
            physicalWidth = framebufferWidth;
            physicalHeight = (int) (framebufferWidth / targetAspect);
            physicalX = 0;
            physicalY = (framebufferHeight - physicalHeight) / 2;
        }

        if (forceVirtualResolution) {
            glViewport(physicalX, physicalY, physicalWidth, physicalHeight);
        } else {
            glViewport(0, 0, framebufferWidth, framebufferHeight);
        }
    }

    private void applyWindowMode() {
        Monitor monitor = Monitor.primary(config.getWidth(), config.getHeight());
        if (mode == Mode.FULLSCREEN && fullscreen == Fullscreen.EXCLUSIVE) {
            glfwSetWindowAttrib(windowHandle, GLFW_AUTO_ICONIFY, GLFW_TRUE);
            glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_FALSE);
            glfwSetWindowMonitor(
                    windowHandle,
                    monitor.getHandle(),
                    0,
                    0,
                    monitor.getWidth(),
                    monitor.getHeight(),
                    monitor.getRefreshRate()
            );
            return;
        }

        if (mode == Mode.FULLSCREEN) {
            glfwSetWindowAttrib(windowHandle, GLFW_AUTO_ICONIFY, GLFW_FALSE);
            glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_FALSE);
            glfwSetWindowMonitor(
                    windowHandle,
                    monitor.getHandle(),
                    0,
                    0,
                    monitor.getWidth(),
                    monitor.getHeight(),
                    monitor.getRefreshRate()
            );
            return;
        }

        glfwSetWindowAttrib(windowHandle, GLFW_AUTO_ICONIFY, GLFW_TRUE);
        glfwSetWindowMonitor(windowHandle, NULL, 0, 0, config.getWidth(), config.getHeight(), 0);
        glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_TRUE);
        centerWindowedIfConfigured(monitor);
    }

    private void centerWindowedIfConfigured(Monitor monitor) {
        if (!config.isCentering()) {
            return;
        }
        Centering.center(windowHandle, monitor, config.getWidth(), config.getHeight());
    }
}
