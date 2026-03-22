package engine.display;

import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class Centering {

    private Centering() {
    }

    public static void center(long windowHandle, Monitor monitor) {
        if (!Monitor.supportsWindowPositioning()) {
            return;
        }

        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            glfwGetWindowSize(windowHandle, width, height);
            glfwSetWindowPos(windowHandle, monitor.centeredX(width.get(0)), monitor.centeredY(height.get(0)));
        }
    }
}
