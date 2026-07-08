package engine.input;

// Событие ввода.
public class Event {

    // > Тип источника события.
    public enum Type {
        KEY,
        MOUSE
    }

    // > Тип источника события.
    private final Type type;

    // > Код клавиши или кнопки.
    private final int code;

    // > Действие события.
    private final int action;

    // > Создаёт новое событие ввода.
    public Event(Type type, int code, int action) {
        this.type = type;
        this.code = code;
        this.action = action;
    }

    public Type getType() {
        return type;
    }

    public int getCode() {
        return code;
    }

    public int getAction() {
        return action;
    }
}