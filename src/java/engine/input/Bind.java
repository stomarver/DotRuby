package engine.input;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_ALT;
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
        CloseState closeState = new CloseState();

        if (config.isKeyboardTrackingEnabled()) {
            glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
                boolean pressed = action != GLFW_RELEASE;
                manager.getKeyboard().setKeyState(key, pressed);
                manager.pushEvent(new Event(Event.Type.KEY, key, action));

                if (shouldSuppressCloseOnHotkey(window, key, action, mods)) {
                    closeState.suppressClose = true;
                    glfwSetWindowShouldClose(window, false);
                } else if (isAltOrF4Release(key, action)) {
                    closeState.suppressClose = false;
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

        glfwSetWindowCloseCallback(windowHandle, window -> {
            if (closeState.suppressClose || isAltF4Pressed(window)) {
                glfwSetWindowShouldClose(window, false);
                return;
            }
            glfwSetWindowShouldClose(window, true);
        });
    }

    private static boolean shouldSuppressCloseOnHotkey(long windowHandle, int key, int action, int mods) {
        if (key != GLFW_KEY_F4 || action != GLFW_PRESS) {
            return false;
        }

        boolean altInModifiers = (mods & GLFW_MOD_ALT) != 0;
        return altInModifiers || isAltPressed(windowHandle);
    }

    private static boolean isAltOrF4Release(int key, int action) {
        if (action != GLFW_RELEASE) {
            return false;
        }
        return key == GLFW_KEY_F4 || key == GLFW_KEY_LEFT_ALT || key == GLFW_KEY_RIGHT_ALT;
    }

    private static boolean isAltPressed(long windowHandle) {
        boolean leftAltPressed = glfwGetKey(windowHandle, GLFW_KEY_LEFT_ALT) == GLFW_PRESS;
        boolean rightAltPressed = glfwGetKey(windowHandle, GLFW_KEY_RIGHT_ALT) == GLFW_PRESS;
        return leftAltPressed || rightAltPressed;
    }

    private static boolean isAltF4Pressed(long windowHandle) {
        boolean f4Pressed = glfwGetKey(windowHandle, GLFW_KEY_F4) == GLFW_PRESS;
        return f4Pressed && isAltPressed(windowHandle);
    }

    private static final class CloseState {
        private boolean suppressClose;
    }
}
