package engine.input;

public class Event {

    public enum Type {
        KEY,
        MOUSE
    }

    private final Type type;
    private final int code;
    private final int action;

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
