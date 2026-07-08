package engine.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Управляет состоянием ввода и буфером событий.
public class Manager {

    // > Конфиг ввода.
    private final Config config;

    // > Включён ли raw input.
    private boolean rawInputEnabled;

    // > Текущее состояние клавиатуры.
    private final Keyboard keyboard = new Keyboard();

    // > Текущее состояние мыши.
    private final Mouse mouse = new Mouse();

    // > Буфер входящих событий.
    private final List<Event> events = new ArrayList<>();

    // > Создаёт менеджер с конфигом по умолчанию.
    public Manager() {
        this(Config.defaults());
    }

    // > Создаёт менеджер с указанным конфигом.
    public Manager(Config config) {
        this.config = config;
    }

    // > Привязывает ввод к окну и дисплею.
    public void bind(long windowHandle, engine.display.Manager displayManager) {
        Bind.apply(windowHandle, displayManager, this);
    }

    // > Переключает полноэкранный режим.
    public void toggleFullscreen(engine.display.Manager displayManager) {
        displayManager.toggleFullscreen();
    }

    // > Возвращает конфиг.
    Config getConfig() {
        return config;
    }

    // > Включает или выключает raw input.
    public void setRawInputEnabled(boolean rawInputEnabled) {
        this.rawInputEnabled = rawInputEnabled;
    }

    // > Проверяет, включён ли raw input.
    public boolean isRawInputEnabled() {
        return rawInputEnabled;
    }

    // > Возвращает состояние клавиатуры.
    public Keyboard getKeyboard() {
        return keyboard;
    }

    // > Возвращает состояние мыши.
    public Mouse getMouse() {
        return mouse;
    }

    // > Добавляет событие в буфер.
    public void pushEvent(Event event) {
        if (events.size() >= config.getMaxBufferedEvents()) {
            events.remove(0);
        }
        events.add(event);
    }

    // > Забирает все события и очищает буфер.
    public List<Event> drainEvents() {
        List<Event> snapshot = new ArrayList<>(events);
        events.clear();
        return Collections.unmodifiableList(snapshot);
    }
}