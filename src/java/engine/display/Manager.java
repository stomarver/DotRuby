package engine.display;

import engine.visual.Overlay;
import engine.visual.utils.BackgroundGradient;
import engine.display.gl.Mesh;
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
import static org.lwjgl.glfw.GLFW.glfwGetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
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
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Manager {

    private final Config config;
    private final ui.Manager uiManager = new ui.Manager();
    private final Overlay overlay = new Overlay();
    private final BackgroundGradient backgroundGradient = new BackgroundGradient();
    private final Mesh mesh = new Mesh();

    private long windowHandle;
    private Mode mode;
    private Fullscreen fullscreen;
    private VSync vSync;
    private final int virtualScale;

    private final int virtualWidth;
    private final int virtualHeight;
    private int framebufferWidth;
    private int framebufferHeight;
    private int physicalX;
    private int physicalY;
    private int physicalWidth;
    private int physicalHeight;
    private int windowedX;
    private int windowedY;
    private int windowedWidth;
    private int windowedHeight;
    private boolean forceVirtualResolution = true;

    public Manager(Config config) {
        this.config = config;
        this.mode = config.getWindowMode();
        this.fullscreen = config.getFullscreen();
        this.vSync = config.getVSync();
        this.virtualScale = config.getVirtualScale();
        this.virtualWidth = config.getWidth();
        this.virtualHeight = config.getHeight();
        this.framebufferWidth = virtualWidth;
        this.framebufferHeight = virtualHeight;
        this.physicalWidth = virtualWidth;
        this.physicalHeight = virtualHeight;
        this.windowedWidth = virtualWidth;
        this.windowedHeight = virtualHeight;
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
        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);

        windowHandle = glfwCreateWindow(config.getWidth(), config.getHeight(), config.getTitle(), NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create window");
        }

        Centering.center(windowHandle, Monitor.primary(config.getWidth(), config.getHeight()), config.getWidth(), config.getHeight());

        glfwMakeContextCurrent(windowHandle);
        applyVSync(vSync);
        glfwShowWindow(windowHandle);
        Borderless.apply(windowHandle);
        rememberWindowedBounds(Monitor.primary(config.getWidth(), config.getHeight()));

        GL.createCapabilities();
        backgroundGradient.init();
        overlay.init();

        glClearColor(config.getClearR(), config.getClearG(), config.getClearB(), config.getClearA());
        glEnable(GL_DEPTH_TEST);
        uiManager.initialize(windowHandle, config.isLockCursor());

        updateViewport();
        glfwSetFramebufferSizeCallback(windowHandle, (window, width, height) -> updateViewport());

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
        return forceVirtualResolution ? Math.max(1, Math.round(getDynamicVirtualWidth())) : Math.max(1, framebufferWidth);
    }

    public int getRenderHeight() {
        return forceVirtualResolution ? Math.max(1, Math.round(getDynamicVirtualHeight())) : Math.max(1, framebufferHeight);
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
        return forceVirtualResolution
                ? virtualScale
                : (framebufferWidth / (float) virtualWidth);
    }

    public float getUiScaleToPhysicalPixelsExact() {
        return forceVirtualResolution
                ? virtualScale
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

    public float getPhysicalPixelsPerVirtualUnitXExact() {
        return forceVirtualResolution
                ? virtualScale
                : (framebufferWidth / (float) virtualWidth);
    }

    public float getPhysicalPixelsPerVirtualUnitYExact() {
        return forceVirtualResolution
                ? virtualScale
                : (framebufferHeight / (float) virtualHeight);
    }

    public float toVirtualX(double physicalScreenX) {
        float scale = forceVirtualResolution ? virtualScale : (framebufferWidth / (float) virtualWidth);
        float offsetX = forceVirtualResolution ? physicalX : 0f;
        return (float) ((physicalScreenX - offsetX) / Math.max(scale, 0.0001f));
    }

    public float toVirtualY(double physicalScreenY) {
        float scale = forceVirtualResolution ? virtualScale : (framebufferHeight / (float) virtualHeight);
        float offsetY = forceVirtualResolution ? physicalY : 0f;
        return (float) ((physicalScreenY - offsetY) / Math.max(scale, 0.0001f));
    }

    public float toPhysicalX(float virtualX) {
        float scale = forceVirtualResolution ? virtualScale : (framebufferWidth / (float) virtualWidth);
        float offsetX = forceVirtualResolution ? physicalX : 0f;
        return offsetX + (virtualX * scale);
    }

    public float toPhysicalY(float virtualY) {
        float scale = forceVirtualResolution ? virtualScale : (framebufferHeight / (float) virtualHeight);
        float offsetY = forceVirtualResolution ? physicalY : 0f;
        return offsetY + (virtualY * scale);
    }

    public int createVertexArray() {
        return mesh.createVertexArray();
    }

    public int createVertexBuffer(float[] vertices) {
        return mesh.createVertexBuffer(vertices);
    }

    public int createVertexBuffer(float[] vertices, int usage) {
        return mesh.createVertexBuffer(vertices, usage);
    }

    public void prepareVertexLayout(int vaoId, int vboId, int attributeIndex, int componentCount, int strideBytes, int offsetBytes) {
        mesh.prepareVertexLayout(vaoId, vboId, attributeIndex, componentCount, strideBytes, offsetBytes);
    }

    public void bindVertexArray(int vaoId) {
        mesh.bindVertexArray(vaoId);
    }

    public void unbindVertexArray() {
        mesh.unbindVertexArray();
    }

    public void deleteVertexArray(int vaoId) {
        mesh.deleteVertexArray(vaoId);
    }

    public void deleteVertexBuffer(int vboId) {
        mesh.deleteVertexBuffer(vboId);
    }

    public void clearFrame() {
        glViewport(0, 0, getFramebufferWidth(), getFramebufferHeight());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        backgroundGradient.render(getFramebufferWidth(), getFramebufferHeight(), (float) glfwGetTime());
        applyRenderViewport();
    }

    public void updateFrame() {
        uiManager.render3D();
        begin2DPass();
        uiManager.render2D(
                overlay,
                getVirtualUnitsForPhysicalPixels(2f),
                getVirtualUnitsForPhysicalPixelsExact(getCursor().getTextureWidth()),
                getVirtualUnitsForPhysicalPixelsExact(getCursor().getTextureHeight())
        );
        end2DPass();
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public void setMode(Mode mode) {
        Mode nextMode = mode == null ? Mode.WINDOWED : mode;
        if (this.mode == Mode.WINDOWED && nextMode == Mode.FULLSCREEN) {
            rememberWindowedBounds(Monitor.primary(config.getWidth(), config.getHeight()));
        }
        this.mode = nextMode;
        uiManager.preserveCursorGridPosition(getDynamicVirtualWidth(), getDynamicVirtualHeight());
        applyWindowMode();
        updateViewport();
    }

    public void setFullscreen(Fullscreen fullscreen) {
        this.fullscreen = fullscreen == null ? Fullscreen.BORDERLESS : fullscreen;
        if (mode == Mode.FULLSCREEN) {
            uiManager.preserveCursorGridPosition(getDynamicVirtualWidth(), getDynamicVirtualHeight());
            applyWindowMode();
            updateViewport();
        }
    }

    public void toggleFullscreen() {
        setMode(mode == Mode.WINDOWED ? Mode.FULLSCREEN : Mode.WINDOWED);
    }

    public ui.Cursor getCursor() {
        return uiManager.getCursor();
    }

    public ui.Manager getUiManager() {
        return uiManager;
    }

    public void beginSelection() {
        uiManager.beginSelection();
    }

    public void updateSelection() {
        uiManager.updateSelection();
    }

    public void clearSelection() {
        uiManager.clearSelection();
    }

    public void applyCursorLock() {
        uiManager.applyCursorLock(windowHandle, config.isLockCursor());
    }

    public void updateCursorPosition(double physicalX, double physicalY) {
        uiManager.updateCursorPosition(
                physicalX,
                physicalY,
                toVirtualX(physicalX),
                toVirtualY(physicalY),
                getPhysicalPixelsPerVirtualUnitXExact(),
                getPhysicalPixelsPerVirtualUnitYExact(),
                getDynamicVirtualWidth(),
                getDynamicVirtualHeight()
        );
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
        backgroundGradient.destroy();
        overlay.destroy();
        uiManager.destroy();
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }
    }

    private void applyRenderViewport() {
        if (forceVirtualResolution) {
            glViewport(physicalX, physicalY, physicalWidth, physicalHeight);
            return;
        }
        glViewport(0, 0, framebufferWidth, framebufferHeight);
    }

    private void begin2DPass() {
        applyRenderViewport();
        overlay.begin(getDynamicVirtualWidth(), getDynamicVirtualHeight());
    }

    private void end2DPass() {
        overlay.end();
    }

    private void updateViewport() {
        int[] fbW = new int[1];
        int[] fbH = new int[1];
        glfwGetFramebufferSize(windowHandle, fbW, fbH);

        framebufferWidth = Math.max(1, fbW[0]);
        framebufferHeight = Math.max(1, fbH[0]);
        physicalX = 0;
        physicalY = 0;
        physicalWidth = framebufferWidth;
        physicalHeight = framebufferHeight;

        if (forceVirtualResolution) {
            glViewport(physicalX, physicalY, physicalWidth, physicalHeight);
        } else {
            glViewport(0, 0, framebufferWidth, framebufferHeight);
        }
    }

    private float getDynamicVirtualWidth() {
        if (!forceVirtualResolution) {
            return Math.max(1, framebufferWidth);
        }
        return framebufferWidth / (float) Math.max(1, virtualScale);
    }

    private float getDynamicVirtualHeight() {
        if (!forceVirtualResolution) {
            return Math.max(1, framebufferHeight);
        }
        return framebufferHeight / (float) Math.max(1, virtualScale);
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
        glfwSetWindowMonitor(
                windowHandle,
                NULL,
                windowedX(monitor),
                windowedY(monitor),
                windowedWidth(monitor),
                windowedHeight(monitor),
                0
        );
        glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_FALSE);
    }

    private int windowedX(Monitor monitor) {
        if (!Monitor.supportsWindowPositioning()) {
            return 0;
        }
        if (windowedWidth > 0 || windowedHeight > 0) {
            return windowedX;
        }
        if (!config.isCentering()) {
            return 0;
        }
        return monitor.centeredX(config.getWidth());
    }

    private int windowedY(Monitor monitor) {
        if (!Monitor.supportsWindowPositioning()) {
            return 0;
        }
        if (windowedWidth > 0 || windowedHeight > 0) {
            return windowedY;
        }
        if (!config.isCentering()) {
            return 0;
        }
        return monitor.centeredY(config.getHeight());
    }

    private int windowedWidth(Monitor monitor) {
        if (windowedWidth > 0) {
            return windowedWidth;
        }
        return config.isCentering() ? Math.min(config.getWidth(), monitor.getWidth()) : config.getWidth();
    }

    private int windowedHeight(Monitor monitor) {
        if (windowedHeight > 0) {
            return windowedHeight;
        }
        return config.isCentering() ? Math.min(config.getHeight(), monitor.getHeight()) : config.getHeight();
    }

    private void rememberWindowedBounds(Monitor monitor) {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(windowHandle, width, height);
        windowedWidth = Math.max(1, width[0]);
        windowedHeight = Math.max(1, height[0]);

        if (!Monitor.supportsWindowPositioning()) {
            windowedX = 0;
            windowedY = 0;
            return;
        }

        int[] x = new int[1];
        int[] y = new int[1];
        glfwGetWindowPos(windowHandle, x, y);
        if (x[0] == 0 && y[0] == 0 && config.isCentering()) {
            windowedX = monitor.centeredX(windowedWidth);
            windowedY = monitor.centeredY(windowedHeight);
            return;
        }
        windowedX = x[0];
        windowedY = y[0];
    }
}
