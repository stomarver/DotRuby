package engine.display;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_FOCUS_ON_SHOW;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Manager {

    private final Config config;
    private final Cursor cursor = new Cursor();

    private long windowHandle;
    private Mode mode = Mode.WINDOWED;

    public Manager(Config config) {
        this.config = config;
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

        centerWindow(Monitor.primary(config.getWidth(), config.getHeight()));

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(config.isVSync() ? 1 : 0);
        glfwShowWindow(windowHandle);

        GL.createCapabilities();

        glClearColor(config.getClearR(), config.getClearG(), config.getClearB(), config.getClearA());
        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, config.getWidth(), config.getHeight());

        return windowHandle;
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
        if (mode == null || mode == this.mode) {
            return;
        }

        Monitor monitor = Monitor.primary(config.getWidth(), config.getHeight());
        if (mode == Mode.FULLSCREEN) {
            glfwSetWindowMonitor(
                    windowHandle,
                    monitor.getHandle(),
                    0,
                    0,
                    monitor.getWidth(),
                    monitor.getHeight(),
                    0
            );
        } else {
            glfwSetWindowMonitor(windowHandle, NULL, 0, 0, config.getWidth(), config.getHeight(), 0);
            centerWindow(monitor);
        }

        this.mode = mode;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public Mode getMode() {
        return mode;
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

    private void centerWindow(Monitor monitor) {
        if (!Monitor.supportsWindowPositioning() || mode != Mode.WINDOWED) {
            return;
        }

        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            glfwGetWindowSize(windowHandle, width, height);

            glfwSetWindowPos(
                    windowHandle,
                    monitor.centeredX(width.get(0)),
                    monitor.centeredY(height.get(0))
            );
        }
    }
}
