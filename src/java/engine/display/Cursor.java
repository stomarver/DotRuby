package engine.display;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;

public class Cursor {

    public enum State {
        NORMAL(GLFW_CURSOR_NORMAL),
        HIDDEN(GLFW_CURSOR_HIDDEN),
        CAPTURED(GLFW_CURSOR_DISABLED);

        private final int glfwValue;

        State(int glfwValue) {
            this.glfwValue = glfwValue;
        }

        public int glfwValue() {
            return glfwValue;
        }
    }

    private State state = State.NORMAL;

    public void setState(long windowHandle, State newState) {
        if (newState == null) {
            return;
        }

        state = newState;
        glfwSetInputMode(windowHandle, GLFW_CURSOR, newState.glfwValue());
    }

    public State getState() {
        return state;
    }
}
