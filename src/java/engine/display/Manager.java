package engine.display;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_DECORATED;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_FOCUS_ON_SHOW;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
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

    public Manager(Config config) {
        this.config = config;
        this.mode = config.getWindowMode();
        this.fullscreen = config.getFullscreen();
        this.vSync = config.getVSync();
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

        Centering.center(windowHandle, Monitor.primary(config.getWidth(), config.getHeight()));

        glfwMakeContextCurrent(windowHandle);
        applyVSync(vSync);
        glfwShowWindow(windowHandle);

        GL.createCapabilities();

        glClearColor(config.getClearR(), config.getClearG(), config.getClearB(), config.getClearA());
        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, config.getWidth(), config.getHeight());

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
    }

    public void setFullscreen(Fullscreen fullscreen) {
        this.fullscreen = fullscreen == null ? Fullscreen.BORDERLESS : fullscreen;
        if (mode == Mode.FULLSCREEN) {
            applyWindowMode();
        }
    }

    public void toggleFullscreen() {
        setMode(mode == Mode.WINDOWED ? Mode.FULLSCREEN : Mode.WINDOWED);
    }

    public Cursor getCursor() {
        return cursor;
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
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }
    }

    private void applyWindowMode() {
        Monitor monitor = Monitor.primary(config.getWidth(), config.getHeight());
        if (mode == Mode.FULLSCREEN && fullscreen == Fullscreen.EXCLUSIVE) {
            glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_FALSE);
            glfwSetWindowMonitor(
                    windowHandle,
                    monitor.getHandle(),
                    0,
                    0,
                    monitor.getWidth(),
                    monitor.getHeight(),
                    0
            );
            Centering.center(windowHandle, monitor);
            return;
        }

        if (mode == Mode.FULLSCREEN) {
            glfwSetWindowMonitor(windowHandle, NULL, monitor.getPositionX(), monitor.getPositionY(), monitor.getWidth(), monitor.getHeight(), 0);
            glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_FALSE);
            Centering.center(windowHandle, monitor);
            return;
        }

        glfwSetWindowMonitor(windowHandle, NULL, 0, 0, config.getWidth(), config.getHeight(), 0);
        glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_TRUE);
        centerWindowOnWindowedExit(monitor);
    }

    private void centerWindowOnWindowedExit(Monitor monitor) {
        if (!config.isCenterOnWindowedExit()) {
            return;
        }
        Centering.center(windowHandle, monitor);
    }
}
