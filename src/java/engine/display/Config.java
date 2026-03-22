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
        return new Config("DotRuby", 960, 540, false, Mode.WINDOWED, FullscreenType.BORDERLESS, false, VSync.DOUBLE_BUFFERED, 0.0f, 0.0f, 0.0f, 1.0f);
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
                parseFullscreenType(values.get("fullscreen_type"), defaults.getFullscreenType()),
                parseBoolean(values.get("raw_input"), defaults.isRawInputEnabled()),
                parseVSync(values.get("vsync"), defaults.getVSync()),
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
    private final FullscreenType fullscreenType;
    private final boolean rawInputEnabled;
    private final VSync vSync;
    private final float clearR;
    private final float clearG;
    private final float clearB;
    private final float clearA;

    public Config(String title,
                  int width,
                  int height,
                  boolean resizable,
                  Mode windowMode,
                  FullscreenType fullscreenType,
                  boolean rawInputEnabled,
                  VSync vSync,
                  float clearR,
                  float clearG,
                  float clearB,
                  float clearA) {
        this.title = title == null || title.isBlank() ? "DotRuby" : title;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.resizable = resizable;
        this.windowMode = windowMode == null ? Mode.WINDOWED : windowMode;
        this.fullscreenType = fullscreenType == null ? FullscreenType.BORDERLESS : fullscreenType;
        this.rawInputEnabled = rawInputEnabled;
        this.vSync = vSync == null ? VSync.DOUBLE_BUFFERED : vSync;
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

    public FullscreenType getFullscreenType() {
        return fullscreenType;
    }

    public boolean isRawInputEnabled() {
        return rawInputEnabled;
    }

    public VSync getVSync() {
        return vSync;
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

    private static FullscreenType parseFullscreenType(String value, FullscreenType fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return FullscreenType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private static VSync parseVSync(String value, VSync fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return VSync.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
