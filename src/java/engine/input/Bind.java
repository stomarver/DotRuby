package engine.input;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

public final class Bind {

    private Bind() {
    }

    public static void apply(long windowHandle, Manager manager) {
        Config config = manager.getConfig();

        if (config.isKeyboardTrackingEnabled()) {
            glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
                boolean pressed = action != GLFW_RELEASE;
                manager.getKeyboard().setKeyState(key, pressed);
                manager.pushEvent(new Event(Event.Type.KEY, key, action));
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

        glfwSetWindowCloseCallback(windowHandle, window -> {
            if (isAltF4Pressed(window)) {
                glfwSetWindowShouldClose(window, false);
                return;
            }
            glfwSetWindowShouldClose(window, true);
        });
    }

    private static boolean isAltF4Pressed(long windowHandle) {
        boolean f4Pressed = glfwGetKey(windowHandle, GLFW_KEY_F4) == GLFW_PRESS;
        boolean leftAltPressed = glfwGetKey(windowHandle, GLFW_KEY_LEFT_ALT) == GLFW_PRESS;
        boolean rightAltPressed = glfwGetKey(windowHandle, GLFW_KEY_RIGHT_ALT) == GLFW_PRESS;
        return f4Pressed && (leftAltPressed || rightAltPressed);
    }
}
