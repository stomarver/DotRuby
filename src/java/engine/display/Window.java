package engine.display;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private long windowHandle;

    private final int width = 560;
    private final int height = 540;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("GLFW is not initialized");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_TRUE);

        long primaryMonitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = (primaryMonitor != NULL) ? glfwGetVideoMode(primaryMonitor) : null;

        int windowWidth = width;
        int windowHeight = height;

        windowHandle = glfwCreateWindow(windowWidth, windowHeight, "DotRuby", NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create window");
        }

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(windowHandle, pWidth, pHeight);

            IntBuffer monitorX = stack.mallocInt(1);
            IntBuffer monitorY = stack.mallocInt(1);
            glfwGetMonitorPos(primaryMonitor, monitorX, monitorY);

            int monitorW = (vidmode != null) ? vidmode.width() : width;
            int monitorH = (vidmode != null) ? vidmode.height() : height;

            glfwSetWindowPos(
                    windowHandle,
                    monitorX.get(0) + (monitorW - pWidth.get(0)) / 2,
                    monitorY.get(0) + (monitorH - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1);
        glfwShowWindow(windowHandle);

        GL.createCapabilities();

        glClearColor(0.08f, 0.10f, 0.14f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, windowWidth, windowHeight);
    }

    private void loop() {
        while (!glfwWindowShouldClose(windowHandle)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glfwSwapBuffers(windowHandle);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }
    }
}
