package engine.display;

import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;

public final class Centering {

    private Centering() {
    }

    public static void center(long windowHandle, Monitor monitor, int width, int height) {
        if (!Monitor.supportsWindowPositioning()) {
            return;
        }

        glfwSetWindowPos(windowHandle, monitor.centeredX(width), monitor.centeredY(height));
    }
}
