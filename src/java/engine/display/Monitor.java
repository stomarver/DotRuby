package engine.display;

import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetMonitorPos;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Monitor {

    private final long handle;
    private final int positionX;
    private final int positionY;
    private final int width;
    private final int height;
    private final int refreshRate;

    private Monitor(long handle, int positionX, int positionY, int width, int height, int refreshRate) {
        this.handle = handle;
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
        this.refreshRate = refreshRate;
    }

    public static Monitor primary(int fallbackWidth, int fallbackHeight) {
        long primaryMonitor = glfwGetPrimaryMonitor();
        if (primaryMonitor == NULL) {
            return new Monitor(NULL, 0, 0, fallbackWidth, fallbackHeight, 0);
        }

        int monitorX = 0;
        int monitorY = 0;

        try (MemoryStack stack = stackPush()) {
            IntBuffer xBuffer = stack.mallocInt(1);
            IntBuffer yBuffer = stack.mallocInt(1);
            glfwGetMonitorPos(primaryMonitor, xBuffer, yBuffer);

            monitorX = xBuffer.get(0);
            monitorY = yBuffer.get(0);
        }

        GLFWVidMode videoMode = glfwGetVideoMode(primaryMonitor);
        int monitorWidth = (videoMode != null) ? videoMode.width() : fallbackWidth;
        int monitorHeight = (videoMode != null) ? videoMode.height() : fallbackHeight;
        int refreshRate = (videoMode != null) ? videoMode.refreshRate() : 0;

        return new Monitor(primaryMonitor, monitorX, monitorY, monitorWidth, monitorHeight, refreshRate);
    }

    public long getHandle() {
        return handle;
    }

    public int getPositionX() {
        return positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRefreshRate() {
        return refreshRate;
    }

    public int centeredX(int windowWidth) {
        return positionX + (width - windowWidth) / 2;
    }

    public int centeredY(int windowHeight) {
        return positionY + (height - windowHeight) / 2;
    }

    public static boolean supportsWindowPositioning() {
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");

        if (sessionType != null && sessionType.equalsIgnoreCase("wayland")) {
            return false;
        }

        return waylandDisplay == null || waylandDisplay.isBlank();
    }
}
