package engine.input;

import java.util.HashSet;
import java.util.Set;

// Локальный снимок состояния мыши.
public class Mouse {

    // > Нажатые кнопки мыши.
    private final Set<Integer> pressedButtons = new HashSet<>();

    // > Последняя известная позиция курсора.
    private volatile double x;
    private volatile double y;

    // > Обновляет состояние кнопки.
    public synchronized void setButtonState(int button, boolean pressed) {
        if (pressed) {
            pressedButtons.add(button);
        } else {
            pressedButtons.remove(button);
        }
    }

    // > Проверяет, нажата ли кнопка.
    public synchronized boolean isPressed(int button) {
        return pressedButtons.contains(button);
    }

    // > Обновляет позицию курсора.
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}