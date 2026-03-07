package engine.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Manager {

    private final Keyboard keyboard = new Keyboard();
    private final Mouse mouse = new Mouse();
    private final List<Event> events = new ArrayList<>();

    public Keyboard getKeyboard() {
        return keyboard;
    }

    public Mouse getMouse() {
        return mouse;
    }

    public void pushEvent(Event event) {
        events.add(event);
    }

    public List<Event> drainEvents() {
        List<Event> snapshot = new ArrayList<>(events);
        events.clear();
        return Collections.unmodifiableList(snapshot);
    }
}
