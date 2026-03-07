package engine.input;

import java.util.HashSet;
import java.util.Set;

public class Mouse {

    private final Set<Integer> pressedButtons = new HashSet<>();
    private double x;
    private double y;

    public void setButtonState(int button, boolean pressed) {
        if (pressed) {
            pressedButtons.add(button);
        } else {
            pressedButtons.remove(button);
        }
    }

    public boolean isPressed(int button) {
        return pressedButtons.contains(button);
    }

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
