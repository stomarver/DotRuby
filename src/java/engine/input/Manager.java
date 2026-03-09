package engine.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;

public class Manager {

    private final Config config;
    private final Keyboard keyboard = new Keyboard();
    private final Mouse mouse = new Mouse();
    private final List<Event> events = new ArrayList<>();

    public Manager() {
        this(Config.defaults());
    }

    public Manager(Config config) {
        this.config = config;
    }

    public void bind(long windowHandle) {
        if (config.isKeyboardTrackingEnabled()) {
            glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
                boolean pressed = action != GLFW_RELEASE;
                keyboard.setKeyState(key, pressed);
                pushEvent(new Event(Event.Type.KEY, key, action));
            });
        }

        if (config.isMouseTrackingEnabled()) {
            glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
                boolean pressed = action == GLFW_PRESS;
                mouse.setButtonState(button, pressed);
                pushEvent(new Event(Event.Type.MOUSE, button, action));
            });

            glfwSetCursorPosCallback(windowHandle, (window, x, y) -> mouse.setPosition(x, y));
        }
    }

    public Keyboard getKeyboard() {
        return keyboard;
    }

    public Mouse getMouse() {
        return mouse;
    }

    public void pushEvent(Event event) {
        if (events.size() >= config.getMaxBufferedEvents()) {
            events.remove(0);
        }
        events.add(event);
    }

    public List<Event> drainEvents() {
        List<Event> snapshot = new ArrayList<>(events);
        events.clear();
        return Collections.unmodifiableList(snapshot);
    }
}
