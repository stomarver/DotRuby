package engine.util.system;

import engine.util.path.RuntimePaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_COCOA;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_NULL;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;

public final class Detect {

    private static final Path ENV_LOG_PATH = RuntimePaths.logPath("env.log");

    private Detect() {
    }

    public static void updateEnvLog() {
        write(ENV_LOG_PATH, describeEnvironment());
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

    public static String osName() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    }

    public static String linuxDistro() {
        if (!isLinux()) {
            return "<not-linux>";
        }

        Path osRelease = Path.of("/etc/os-release");
        if (!Files.exists(osRelease)) {
            return "<unknown-linux>";
        }

        try {
            Map<String, String> values = parseKeyValueFile(osRelease);
            String prettyName = values.get("PRETTY_NAME");
            if (prettyName != null && !prettyName.isBlank()) {
                return prettyName;
            }

            String name = values.getOrDefault("NAME", "linux");
            String version = values.getOrDefault("VERSION", "").trim();
            return version.isBlank() ? name : name + " " + version;
        } catch (IOException exception) {
            return "<unknown-linux>";
        }
    }

    public static String env(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? "<unset>" : value;
    }

    private static String describeEnvironment() {
        StringBuilder log = new StringBuilder();
        appendLine(log, "distro", distroLabel());
        if (isLinux()) {
            appendLine(log, "kernel", System.getProperty("os.version", "<unset>"));
            appendLine(log, "desktop", env("XDG_CURRENT_DESKTOP"));
            appendLine(log, "sessionType", env("XDG_SESSION_TYPE"));
        }
        appendLine(log, "platform", glfwPlatformName());
        appendLine(log, "javaVersion", System.getProperty("java.version", "<unset>"));
        return log.toString();
    }

    private static String distroLabel() {
        if (isLinux()) {
            return linuxDistro();
        }
        String osName = System.getProperty("os.name", "<unknown>");
        String osVersion = System.getProperty("os.version", "");
        return osVersion.isBlank() ? osName : osName + " " + osVersion;
    }

    private static int glfwPlatform() {
        try {
            return glfwGetPlatform();
        } catch (RuntimeException exception) {
            return -1;
        }
    }

    private static Map<String, String> parseKeyValueFile(Path path) throws IOException {
        Map<String, String> values = new HashMap<>();
        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            if (line == null || line.isBlank() || !line.contains("=")) {
                continue;
            }

            String[] parts = line.split("=", 2);
            values.put(parts[0].trim(), unquote(parts[1].trim()));
        }
        return values;
    }

    private static void appendLine(StringBuilder log, String key, String value) {
        log.append(String.format("%-11s", key))
                .append(" = ")
                .append(value)
                .append('\n');
    }

    private static void write(Path path, String body) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, body);
        } catch (IOException exception) {
            System.err.println("[engine.util.system.Detect] failed to write " + path + ": " + exception.getMessage());
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
