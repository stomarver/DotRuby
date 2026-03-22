package engine.display;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_COCOA;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_NULL;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;

public final class Detect {

    private static final Path DEFAULT_PATH = Path.of("src/java/config/Detect.txt");

    private Detect() {
    }

    public static void log() {
        String body = describe();
        System.out.println("[engine.display.Detect]");
        System.out.print(body);
        write(body);
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

    private static void write(String body) {
        try {
            Files.createDirectories(DEFAULT_PATH.getParent());
            Files.writeString(DEFAULT_PATH, body);
        } catch (IOException exception) {
            System.err.println("[engine.display.Detect] failed to write " + DEFAULT_PATH + ": " + exception.getMessage());
        }
    }
}
