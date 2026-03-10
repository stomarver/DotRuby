package engine.input;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

public final class Bind {

    private static final double ALT_F4_FORCE_CLOSE_SECONDS = 3.0;

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

                closeState.update(window, key, action, mods);

                if (closeState.shouldForceClose(window)) {
                    glfwSetWindowShouldClose(window, true);
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
            if (closeState.isAltF4Active(window) && !closeState.shouldForceClose(window)) {
                glfwSetWindowShouldClose(window, false);
                return;
            }
            glfwSetWindowShouldClose(window, true);
        });
    }

    private static final class CloseState {
        private boolean leftAltPressed;
        private boolean rightAltPressed;
        private boolean f4Pressed;
        private double altF4Since = -1.0;

        private void update(long windowHandle, int key, int action, int mods) {
            if (key == GLFW_KEY_LEFT_ALT) {
                leftAltPressed = action != GLFW_RELEASE;
            } else if (key == GLFW_KEY_RIGHT_ALT) {
                rightAltPressed = action != GLFW_RELEASE;
            } else if (key == GLFW_KEY_F4) {
                f4Pressed = action != GLFW_RELEASE;
            }

            boolean altDownByMods = (mods & GLFW_MOD_ALT) != 0;
            boolean altDown = altDownByMods || isAltPressed(windowHandle);
            boolean comboActive = f4Pressed && altDown;

            if (comboActive) {
                if (altF4Since < 0.0) {
                    altF4Since = glfwGetTime();
                }
            } else {
                altF4Since = -1.0;
            }
        }

        private boolean isAltF4Active(long windowHandle) {
            boolean altDown = leftAltPressed || rightAltPressed || isAltPressed(windowHandle);
            boolean f4Down = f4Pressed || glfwGetKey(windowHandle, GLFW_KEY_F4) == GLFW_PRESS;
            return altDown && f4Down;
        }

        private boolean shouldForceClose(long windowHandle) {
            if (!isAltF4Active(windowHandle)) {
                return false;
            }
            if (altF4Since < 0.0) {
                altF4Since = glfwGetTime();
                return false;
            }
            return (glfwGetTime() - altF4Since) >= ALT_F4_FORCE_CLOSE_SECONDS;
        }

        private boolean isAltPressed(long windowHandle) {
            boolean leftAlt = leftAltPressed || glfwGetKey(windowHandle, GLFW_KEY_LEFT_ALT) == GLFW_PRESS;
            boolean rightAlt = rightAltPressed || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_ALT) == GLFW_PRESS;
            return leftAlt || rightAlt;
        }
    }
}
