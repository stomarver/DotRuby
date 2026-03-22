package engine.input;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Config {

    private static final Path DEFAULT_PATH = Path.of("src/java/config/Input.txt");

    public static Config defaults() {
        return new Config(true, true, false, 1024, GLFW.GLFW_KEY_F11, 0);
    }

    public static Config loadDefault(boolean rawMouseInputEnabled) {
        return load(DEFAULT_PATH, rawMouseInputEnabled);
    }

    public static Config load(Path path, boolean rawMouseInputEnabled) {
        Config defaults = defaults().withRawMouseInput(rawMouseInputEnabled);
        Map<String, String> values = readKeyValueFile(path);
        return new Config(
                defaults.isMouseTrackingEnabled(),
                defaults.isKeyboardTrackingEnabled(),
                rawMouseInputEnabled,
                defaults.getMaxBufferedEvents(),
                parseKey(values.get("fullscreen_toggle_key"), defaults.getFullscreenToggleKey()),
                parseModifiers(values.get("fullscreen_toggle_modifiers"), defaults.getFullscreenToggleModifiers())
        );
    }

    private final boolean mouseTrackingEnabled;
    private final boolean keyboardTrackingEnabled;
    private final boolean rawMouseInputEnabled;
    private final int maxBufferedEvents;
    private final int fullscreenToggleKey;
    private final int fullscreenToggleModifiers;

    public Config(boolean mouseTrackingEnabled,
                  boolean keyboardTrackingEnabled,
                  boolean rawMouseInputEnabled,
                  int maxBufferedEvents,
                  int fullscreenToggleKey,
                  int fullscreenToggleModifiers) {
        this.mouseTrackingEnabled = mouseTrackingEnabled;
        this.keyboardTrackingEnabled = keyboardTrackingEnabled;
        this.rawMouseInputEnabled = rawMouseInputEnabled;
        this.maxBufferedEvents = Math.max(1, maxBufferedEvents);
        this.fullscreenToggleKey = fullscreenToggleKey;
        this.fullscreenToggleModifiers = fullscreenToggleModifiers;
    }

    public boolean isMouseTrackingEnabled() {
        return mouseTrackingEnabled;
    }

    public boolean isKeyboardTrackingEnabled() {
        return keyboardTrackingEnabled;
    }

    public boolean isRawMouseInputEnabled() {
        return rawMouseInputEnabled;
    }

    public int getMaxBufferedEvents() {
        return maxBufferedEvents;
    }

    public int getFullscreenToggleKey() {
        return fullscreenToggleKey;
    }

    public int getFullscreenToggleModifiers() {
        return fullscreenToggleModifiers;
    }

    public boolean isFullscreenToggle(int key, int action, int mods) {
        return action == GLFW.GLFW_PRESS
                && key == fullscreenToggleKey
                && mods == fullscreenToggleModifiers;
    }

    public Config withRawMouseInput(boolean rawMouseInputEnabled) {
        return new Config(mouseTrackingEnabled, keyboardTrackingEnabled, rawMouseInputEnabled, maxBufferedEvents, fullscreenToggleKey, fullscreenToggleModifiers);
    }

    private static Map<String, String> readKeyValueFile(Path path) {
        Map<String, String> values = new HashMap<>();
        if (path == null || !Files.exists(path)) {
            return values;
        }

        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String normalized = stripComment(line);
                if (normalized.isBlank() || !normalized.contains("=")) {
                    continue;
                }

                String[] parts = normalized.split("=", 2);
                values.put(parts[0].trim().toLowerCase(Locale.ROOT), parts[1].trim());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read input config: " + path, exception);
        }
        return values;
    }

    private static String stripComment(String line) {
        int commentIndex = line.indexOf('#');
        return commentIndex >= 0 ? line.substring(0, commentIndex).trim() : line.trim();
    }

    private static int parseKey(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        String fieldName = normalized.startsWith("GLFW_KEY_") ? normalized : "GLFW_KEY_" + normalized;
        try {
            Field field = GLFW.class.getField(fieldName);
            return field.getInt(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Unknown GLFW key: " + value, exception);
        }
    }

    private static int parseModifiers(String value, int fallback) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NONE")) {
            return fallback == 0 ? 0 : fallback;
        }

        int modifiers = 0;
        for (String token : value.split("\\+")) {
            String normalized = token.trim().toUpperCase(Locale.ROOT);
            switch (normalized) {
                case "SHIFT" -> modifiers |= GLFW.GLFW_MOD_SHIFT;
                case "CONTROL", "CTRL" -> modifiers |= GLFW.GLFW_MOD_CONTROL;
                case "ALT" -> modifiers |= GLFW.GLFW_MOD_ALT;
                case "SUPER", "META" -> modifiers |= GLFW.GLFW_MOD_SUPER;
                case "CAPS_LOCK" -> modifiers |= GLFW.GLFW_MOD_CAPS_LOCK;
                case "NUM_LOCK" -> modifiers |= GLFW.GLFW_MOD_NUM_LOCK;
                default -> throw new IllegalArgumentException("Unknown GLFW modifier: " + token);
            }
        }
        return modifiers;
    }
}
