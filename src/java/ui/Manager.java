package ui;

import engine.visual.Overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Manager {

    private final Cursor cursor = new Cursor();
    private final Selection selection = new Selection();
    private final engine.visual.Manager visualManager = new engine.visual.Manager();
    private final List<Event> events = new ArrayList<>();
    private boolean ignoreNextCursorSync;

    public void initialize(long windowHandle, boolean lockCursor) {
        cursor.loadTexture();
        visualManager.initialize();
        applyCursorLock(windowHandle, lockCursor);
    }

    public void render3D() {
        visualManager.render3D();
    }

    public void render2D(Overlay overlay, float borderThickness, float cursorWidth, float cursorHeight, float configuredVirtualScale) {
        visualManager.render2D(overlay, configuredVirtualScale);
        selection.render(overlay, borderThickness);
        cursor.render(overlay, cursorWidth, cursorHeight);
    }

    public void activateScene(int sceneHotkey) {
        visualManager.activateSceneByHotkey(sceneHotkey);
    }

    public void toggleLightingMode() {
        visualManager.toggleLightingMode();
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
                                     float virtualWidth,
                                     float virtualHeight) {
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

    public void preserveCursorGridPosition(float virtualWidth, float virtualHeight) {
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
        visualManager.destroy();
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
