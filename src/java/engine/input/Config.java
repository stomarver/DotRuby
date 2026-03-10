package engine.input;

public final class Config {

    public static Config defaults() {
        return new Config(true, true, 1024);
    }

    private final boolean mouseTrackingEnabled;
    private final boolean keyboardTrackingEnabled;
    private final int maxBufferedEvents;

    public Config(boolean mouseTrackingEnabled, boolean keyboardTrackingEnabled, int maxBufferedEvents) {
        this.mouseTrackingEnabled = mouseTrackingEnabled;
        this.keyboardTrackingEnabled = keyboardTrackingEnabled;
        this.maxBufferedEvents = Math.max(1, maxBufferedEvents);
    }

    public boolean isMouseTrackingEnabled() {
        return mouseTrackingEnabled;
    }

    public boolean isKeyboardTrackingEnabled() {
        return keyboardTrackingEnabled;
    }

    public int getMaxBufferedEvents() {
        return maxBufferedEvents;
    }
}
