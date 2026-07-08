package engine.input;

import java.util.HashSet;
import java.util.Set;

// Локальный снимок состояния клавиатуры.
public class Keyboard {

    // > Нажатые клавиши.
    private final Set<Integer> pressedKeys = new HashSet<>();

    // > Обновляет состояние клавиши.
    public void setKeyState(int key, boolean pressed) {
        if (pressed) {
            pressedKeys.add(key);
        } else {
            pressedKeys.remove(key);
        }
    }

    // > Проверяет, нажата ли клавиша.
    public boolean isPressed(int key) {
        return pressedKeys.contains(key);
    }
}