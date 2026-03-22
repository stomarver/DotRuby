package engine.display;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_COCOA;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_NULL;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;

public final class Detect {

    private static final Path DEFAULT_PATH = Path.of("src/java/config/Detect.txt");
    private static final ScheduledExecutorService WRITER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "engine-display-detect-writer");
        thread.setDaemon(true);
        return thread;
    });

    private Detect() {
    }

    public static void log() {
        String body = describe();
        System.out.println("[engine.display.Detect]");
        System.out.print(body);
        ensureFileExists();
        WRITER.schedule(() -> write(body), 30, TimeUnit.SECONDS);
    }

    public static boolean isLinux() {
        return osName().contains("linux");
    }

    public static boolean isGlfwX11() {
        return glfwPlatform() == GLFW_PLATFORM_X11;
    }

    public static String glfwPlatformName() {
        return switch (glfwPlatform()) {
            case GLFW_PLATFORM_WIN32 -> "WIN32";
            case GLFW_PLATFORM_COCOA -> "COCOA";
            case GLFW_PLATFORM_WAYLAND -> "WAYLAND";
            case GLFW_PLATFORM_X11 -> "X11";
            case GLFW_PLATFORM_NULL -> "NULL";
            default -> "UNKNOWN";
        };
    }

    private static int glfwPlatform() {
        try {
            return glfwGetPlatform();
        } catch (RuntimeException exception) {
            return -1;
        }
    }

    private static String osName() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? "<unset>" : value;
    }

    private static String describe() {
        StringBuilder log = new StringBuilder();
        appendLine(log, "os", osName());
        appendLine(log, "desktop", env("XDG_CURRENT_DESKTOP"));
        appendLine(log, "sessionType", env("XDG_SESSION_TYPE"));
        appendLine(log, "sessionDesktop", env("XDG_SESSION_DESKTOP"));
        appendLine(log, "glfwPlatform", glfwPlatformName());
        appendLine(log, "display", env("DISPLAY"));
        appendLine(log, "waylandDisplay", env("WAYLAND_DISPLAY"));
        appendLine(log, "qtPlatform", env("QT_QPA_PLATFORM"));
        return log.toString();
    }

    private static void appendLine(StringBuilder log, String key, String value) {
        log.append("  ")
                .append(String.format("%-15s", key))
                .append(" = ")
                .append(value)
                .append('\n');
    }

    private static void ensureFileExists() {
        try {
            Files.createDirectories(DEFAULT_PATH.getParent());
            if (!Files.exists(DEFAULT_PATH)) {
                Files.createFile(DEFAULT_PATH);
            }
        } catch (IOException exception) {
            System.err.println("[engine.display.Detect] failed to create " + DEFAULT_PATH + ": " + exception.getMessage());
        }
    }

    private static void write(String body) {
        try {
            Files.writeString(DEFAULT_PATH, body);
        } catch (IOException exception) {
            System.err.println("[engine.display.Detect] failed to write " + DEFAULT_PATH + ": " + exception.getMessage());
        }
    }
}
