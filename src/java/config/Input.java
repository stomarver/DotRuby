package config;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F11;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public final class Input {

    public static Input defaults() {
        return new Input(GLFW_KEY_F11, 0);
    }

    private final int fullscreenToggleKey;
    private final int fullscreenToggleModifiers;

    public Input(int fullscreenToggleKey, int fullscreenToggleModifiers) {
        this.fullscreenToggleKey = fullscreenToggleKey;
        this.fullscreenToggleModifiers = fullscreenToggleModifiers;
    }

    public int getFullscreenToggleKey() {
        return fullscreenToggleKey;
    }

    public int getFullscreenToggleModifiers() {
        return fullscreenToggleModifiers;
    }

    public boolean isFullscreenToggle(int key, int action, int mods) {
        return action == GLFW_PRESS
                && key == fullscreenToggleKey
                && mods == fullscreenToggleModifiers;
    }
}
