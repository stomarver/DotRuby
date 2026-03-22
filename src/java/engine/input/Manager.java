package engine.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Manager {

    private final Config config;
    private boolean rawInputEnabled;
    private final Keyboard keyboard = new Keyboard();
    private final Mouse mouse = new Mouse();
    private final List<Event> events = new ArrayList<>();

    public Manager() {
        this(Config.defaults());
    }

    public Manager(Config config) {
        this.config = config;
    }

    public void bind(long windowHandle, engine.display.Manager displayManager) {
        Bind.apply(windowHandle, displayManager, this);
    }

    public void toggleFullscreen(engine.display.Manager displayManager) {
        displayManager.toggleFullscreen();
    }

    Config getConfig() {
        return config;
    }

    public void setRawInputEnabled(boolean rawInputEnabled) {
        this.rawInputEnabled = rawInputEnabled;
    }

    public boolean isRawInputEnabled() {
        return rawInputEnabled;
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
