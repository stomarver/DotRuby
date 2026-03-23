package engine.ui;

public final class Event {

    public enum Type {
        CURSOR_MOVED,
        CURSOR_STATE_CHANGED,
        CURSOR_BUTTON_CHANGED,
        SELECTION_STARTED,
        SELECTION_UPDATED,
        SELECTION_CLEARED
    }

    private final Type type;
    private final float x;
    private final float y;

    public Event(Type type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public Type getType() {
        return type;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
