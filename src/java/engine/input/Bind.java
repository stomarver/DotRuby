package engine.input;

import engine.util.Specs;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_L;
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
                             Manager manager) {
        Config config = manager.getConfig();

        if (config.isKeyboardTrackingEnabled()) {
            glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
                boolean pressed = action != GLFW_RELEASE;
                manager.getKeyboard().setKeyState(key, pressed);
                manager.pushEvent(new Event(Event.Type.KEY, key, action));

                if (config.isFullscreenToggle(key, action, mods)) {
                    manager.toggleFullscreen(displayManager);
                }

                if (key == GLFW_KEY_L && action == GLFW_PRESS) {
                    Specs.updateLogs();
                }
            });
        }

        if (config.isMouseTrackingEnabled()) {
            glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
                boolean pressed = action == GLFW_PRESS;
                manager.getMouse().setButtonState(button, pressed);
                displayManager.getUiManager().setCursorButtonState(button, pressed);
                manager.pushEvent(new Event(Event.Type.MOUSE, button, action));

                if (button == GLFW_MOUSE_BUTTON_LEFT) {
                    if (pressed) {
                        displayManager.beginSelection();
                    } else {
                        displayManager.clearSelection();
                    }
                }
            });

            glfwSetCursorPosCallback(windowHandle, (window, x, y) -> {
                manager.getMouse().setPosition(x, y);
                displayManager.updateCursorPosition(x, y);
                displayManager.updateSelection();
            });
        }

        applyRawInput(windowHandle, manager);
    }

    public static void applyRawInput(long windowHandle, Manager manager) {
        boolean rawInputEnabled = manager.getConfig().isRawMouseInputEnabled() && glfwRawMouseMotionSupported();
        glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, rawInputEnabled ? GLFW_PRESS : GLFW_RELEASE);
        manager.setRawInputEnabled(rawInputEnabled);
    }
}
