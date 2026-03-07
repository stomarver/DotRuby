package engine.input;

import java.util.HashSet;
import java.util.Set;

public class Keyboard {

    private final Set<Integer> pressedKeys = new HashSet<>();

    public void setKeyState(int key, boolean pressed) {
        if (pressed) {
            pressedKeys.add(key);
        } else {
            pressedKeys.remove(key);
        }
    }

    public boolean isPressed(int key) {
        return pressedKeys.contains(key);
    }
}
