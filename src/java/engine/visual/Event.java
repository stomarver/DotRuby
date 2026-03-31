package engine.visual;

public final class Event {

    public enum Type {
        PERFORMANCE_TEXT_UPDATED
    }

    private final Type type;
    private final String text;

    public Event(Type type, String text) {
        this.type = type;
        this.text = text == null ? "" : text;
    }

    public Type getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}
