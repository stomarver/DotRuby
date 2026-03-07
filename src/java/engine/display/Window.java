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

    private final int width = 960;
    private final int height = 540;
    private final String title = "";  // не отображается, т.к. без декораций

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        // Настройка обработки ошибок GLFW
        GLFWErrorCallback.createPrint(System.err).set();

        // Инициализация GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW is not initialized");
        }

        // Настройки окна: без рамки и заголовка
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);           // спрячем до готовности
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);         // фиксированный размер
        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);         // без рамки и заголовка
        glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_TRUE);

        long primaryMonitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = (primaryMonitor != NULL) ? glfwGetVideoMode(primaryMonitor) : null;

        // Wayland-композиторы (в т.ч. KDE) могут игнорировать undecorated для оконного режима.
        // Фолбэк: создаём fullscreen-окно без декораций, чтобы гарантированно убрать рамку.
        boolean forceFullscreenFallback = isLinuxWaylandSession() && primaryMonitor != NULL && vidmode != null;

        int windowWidth = forceFullscreenFallback ? vidmode.width() : width;
        int windowHeight = forceFullscreenFallback ? vidmode.height() : height;
        long monitorForWindow = forceFullscreenFallback ? primaryMonitor : NULL;

        // Создаём окно
        windowHandle = glfwCreateWindow(windowWidth, windowHeight, title, monitorForWindow, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create window");
        }

        // Дополнительно применяем атрибут после создания (для платформ, где hint применяется непредсказуемо)
        glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_FALSE);

        // Центрируем только обычное оконное окно (fullscreen центрировать не нужно)
        if (!forceFullscreenFallback) {
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
        }

        // Делаем контекст текущим
        glfwMakeContextCurrent(windowHandle);

        // Включаем VSync (можно отключить: glfwSwapInterval(0))
        glfwSwapInterval(1);

        // Показываем окно
        glfwShowWindow(windowHandle);

        // Инициализация OpenGL
        GL.createCapabilities();

        // Базовая настройка OpenGL
        glClearColor(0.08f, 0.10f, 0.14f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, windowWidth, windowHeight);
    }

    private boolean isLinuxWaylandSession() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        return osName.contains("linux") && sessionType != null && sessionType.equalsIgnoreCase("wayland");
    }

    private void loop() {
        while (!glfwWindowShouldClose(windowHandle)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Здесь будет основной рендер игры

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
