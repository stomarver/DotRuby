package engine.display;

import java.util.Locale;

import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_COCOA;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_NULL;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;

public final class Detect {

    private Detect() {
    }

    public static void log() {
        StringBuilder log = new StringBuilder("[display.detect]\n");
        appendLine(log, "os", osName());
        appendLine(log, "desktop", env("XDG_CURRENT_DESKTOP"));
        appendLine(log, "sessionType", env("XDG_SESSION_TYPE"));
        appendLine(log, "sessionDesktop", env("XDG_SESSION_DESKTOP"));
        appendLine(log, "glfwPlatform", glfwPlatformName());
        appendLine(log, "display", env("DISPLAY"));
        appendLine(log, "waylandDisplay", env("WAYLAND_DISPLAY"));
        appendLine(log, "qtPlatform", env("QT_QPA_PLATFORM"));
        System.out.print(log);
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

    private static void appendLine(StringBuilder log, String key, String value) {
        log.append("  ")
                .append(String.format("%-15s", key))
                .append(" = ")
                .append(value)
                .append('\n');
    }
}
