package engine.display;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Config {

    private static final Path DEFAULT_PATH = Path.of("src/java/config/Display.txt");

    public static Config defaults() {
        return new Config("DotRuby", 960, 540, false, Mode.WINDOWED, Fullscreen.BORDERLESS, false, true, VSync.DOUBLE_BUFFERED, true, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    public static Config loadDefault() {
        return load(DEFAULT_PATH);
    }

    public static Config load(Path path) {
        Config defaults = defaults();
        Map<String, String> values = readKeyValueFile(path);
        return new Config(
                defaults.getTitle(),
                defaults.getWidth(),
                defaults.getHeight(),
                defaults.isResizable(),
                parseMode(values.get("window_mode"), defaults.getWindowMode()),
                parseFullscreen(values.get("fullscreen_type"), defaults.getFullscreen()),
                parseBoolean(values.get("raw_input"), defaults.isRawInputEnabled()),
                parseBoolean(values.get("lock_cursor"), defaults.isLockCursor()),
                parseVSync(values.get("vsync"), defaults.getVSync()),
                parseBoolean(values.get("centering"), defaults.isCentering()),
                defaults.getClearR(),
                defaults.getClearG(),
                defaults.getClearB(),
                defaults.getClearA()
        );
    }

    private final String title;
    private final int width;
    private final int height;
    private final boolean resizable;
    private final Mode windowMode;
    private final Fullscreen fullscreen;
    private final boolean rawInputEnabled;
    private final boolean lockCursor;
    private final VSync vSync;
    private final boolean centering;
    private final float clearR;
    private final float clearG;
    private final float clearB;
    private final float clearA;

    public Config(String title,
                  int width,
                  int height,
                  boolean resizable,
                  Mode windowMode,
                  Fullscreen fullscreen,
                  boolean rawInputEnabled,
                  boolean lockCursor,
                  VSync vSync,
                  boolean centering,
                  float clearR,
                  float clearG,
                  float clearB,
                  float clearA) {
        this.title = title == null || title.isBlank() ? "DotRuby" : title;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.resizable = resizable;
        this.windowMode = windowMode == null ? Mode.WINDOWED : windowMode;
        this.fullscreen = fullscreen == null ? Fullscreen.BORDERLESS : fullscreen;
        this.rawInputEnabled = rawInputEnabled;
        this.lockCursor = lockCursor;
        this.vSync = vSync == null ? VSync.DOUBLE_BUFFERED : vSync;
        this.centering = centering;
        this.clearR = clearR;
        this.clearG = clearG;
        this.clearB = clearB;
        this.clearA = clearA;
    }

    public String getTitle() {
        return title;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isResizable() {
        return resizable;
    }

    public Mode getWindowMode() {
        return windowMode;
    }

    public Fullscreen getFullscreen() {
        return fullscreen;
    }

    public boolean isRawInputEnabled() {
        return rawInputEnabled;
    }

    public boolean isLockCursor() {
        return lockCursor;
    }

    public VSync getVSync() {
        return vSync;
    }

    public boolean isCentering() {
        return centering;
    }

    public float getClearR() {
        return clearR;
    }

    public float getClearG() {
        return clearG;
    }

    public float getClearB() {
        return clearB;
    }

    public float getClearA() {
        return clearA;
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
            throw new IllegalStateException("Unable to read display config: " + path, exception);
        }
        return values;
    }

    private static String stripComment(String line) {
        int commentIndex = line.indexOf('#');
        return commentIndex >= 0 ? line.substring(0, commentIndex).trim() : line.trim();
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static Mode parseMode(String value, Mode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Mode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private static Fullscreen parseFullscreen(String value, Fullscreen fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Fullscreen.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private static VSync parseVSync(String value, VSync fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return VSync.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
