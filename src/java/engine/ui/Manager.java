package engine.ui;

import engine.ui.text.Text;
import engine.visual.Overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Manager {

    private final Cursor cursor = new Cursor();
    private final Selection selection = new Selection();
    private final Text text = new Text();
    private final List<Event> events = new ArrayList<>();
    private boolean ignoreNextCursorSync;

    public void initialize(long windowHandle, boolean lockCursor) {
        cursor.loadTexture();
        text.load();
        applyCursorLock(windowHandle, lockCursor);
    }

    public void render(Overlay overlay, float borderThickness, float cursorWidth, float cursorHeight) {
        text.render(overlay);
        selection.render(overlay, borderThickness);
        cursor.render(overlay, cursorWidth, cursorHeight);
    }

    public Cursor getCursor() {
        return cursor;
    }

    public void setCursorButtonState(int button, boolean pressed) {
        cursor.setButtonState(button, pressed);
        pushEvent(new Event(Event.Type.CURSOR_BUTTON_CHANGED, (float) cursor.getX(), (float) cursor.getY()));
    }

    public void beginSelection() {
        selection.begin((float) cursor.getX(), (float) cursor.getY());
        pushEvent(new Event(Event.Type.SELECTION_STARTED, (float) cursor.getX(), (float) cursor.getY()));
    }

    public void updateSelection() {
        selection.update((float) cursor.getX(), (float) cursor.getY());
        pushEvent(new Event(Event.Type.SELECTION_UPDATED, (float) cursor.getX(), (float) cursor.getY()));
    }

    public void clearSelection() {
        selection.clear();
        pushEvent(new Event(Event.Type.SELECTION_CLEARED, (float) cursor.getX(), (float) cursor.getY()));
    }

    public void applyCursorLock(long windowHandle, boolean lockCursor) {
        cursor.setState(windowHandle, lockCursor ? Cursor.State.CAPTURED : Cursor.State.NORMAL);
        pushEvent(new Event(Event.Type.CURSOR_STATE_CHANGED, (float) cursor.getX(), (float) cursor.getY()));
    }

    public void updateCursorPosition(double physicalX,
                                     double physicalY,
                                     float virtualX,
                                     float virtualY,
                                     float physicalPixelsPerVirtualX,
                                     float physicalPixelsPerVirtualY,
                                     int virtualWidth,
                                     int virtualHeight) {
        if (consumeIgnoredCursorSync()) {
            cursor.resetMotionTracking();
            return;
        }

        if (cursor.getState() == Cursor.State.CAPTURED) {
            cursor.updateCapturedPosition(
                    physicalX,
                    physicalY,
                    physicalPixelsPerVirtualX,
                    physicalPixelsPerVirtualY,
                    virtualWidth,
                    virtualHeight
            );
        } else {
            cursor.setClampedPosition(virtualX, virtualY, virtualWidth, virtualHeight);
        }

        pushEvent(new Event(Event.Type.CURSOR_MOVED, (float) cursor.getX(), (float) cursor.getY()));
    }

    public void preserveCursorGridPosition(int virtualWidth, int virtualHeight) {
        ignoreNextCursorSync = true;
        cursor.resetMotionTracking();
        cursor.setClampedPosition(cursor.getX(), cursor.getY(), virtualWidth, virtualHeight);
    }

    public List<Event> drainEvents() {
        List<Event> snapshot = new ArrayList<>(events);
        events.clear();
        return Collections.unmodifiableList(snapshot);
    }

    public void destroy() {
        text.destroy();
        cursor.destroy();
    }

    private boolean consumeIgnoredCursorSync() {
        if (!ignoreNextCursorSync) {
            return false;
        }
        ignoreNextCursorSync = false;
        return true;
    }

    private void pushEvent(Event event) {
        events.add(event);
    }
}
