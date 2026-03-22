package engine.input;

import config.Display;
import config.Input;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RAW_MOUSE_MOTION;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwRawMouseMotionSupported;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;

public final class Bind {

    private Bind() {
    }

    public static void apply(long windowHandle,
                             engine.display.Manager displayManager,
                             Manager manager,
                             Input inputConfig,
                             Display displayConfig) {
        Config config = manager.getConfig();

        if (config.isKeyboardTrackingEnabled()) {
            glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
                boolean pressed = action != GLFW_RELEASE;
                manager.getKeyboard().setKeyState(key, pressed);
                manager.pushEvent(new Event(Event.Type.KEY, key, action));

                if (inputConfig != null && displayConfig != null && inputConfig.isFullscreenToggle(key, action, mods)) {
                    manager.toggleFullscreen(displayManager, displayConfig);
                }
            });
        }

        if (config.isMouseTrackingEnabled()) {
            glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
                boolean pressed = action == GLFW_PRESS;
                manager.getMouse().setButtonState(button, pressed);
                manager.pushEvent(new Event(Event.Type.MOUSE, button, action));
            });

            glfwSetCursorPosCallback(windowHandle, (window, x, y) -> manager.getMouse().setPosition(x, y));
        }

        applyRawInput(windowHandle, manager);
    }

    public static void applyRawInput(long windowHandle, Manager manager) {
        boolean rawInputEnabled = manager.getConfig().isRawMouseInputEnabled() && glfwRawMouseMotionSupported();
        glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, rawInputEnabled ? GLFW_PRESS : GLFW_RELEASE);
        manager.setRawInputEnabled(rawInputEnabled);
    }
}
