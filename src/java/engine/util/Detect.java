package engine.util;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_L;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_COCOA;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_NULL;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;
import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glGetString;

public final class Detect {

    private static final Path LOG_DIRECTORY = Path.of("src/java/detect");
    private static final Path SYS_LOG_PATH = LOG_DIRECTORY.resolve("sys.log");
    private static final Path ENV_LOG_PATH = LOG_DIRECTORY.resolve("env.log");
    private static final Path GPU_LOG_PATH = LOG_DIRECTORY.resolve("gpu.log");
    private static final Path LEGACY_SYS_LOG_DIRECTORY = LOG_DIRECTORY.resolve("sys.log");
    private static final Path LEGACY_SYS_PACKAGE_DIRECTORY = LOG_DIRECTORY.resolve("sys");
    private static final long WRITE_DELAY_SECONDS = 30L;
    private static final int GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX = 0x9048;
    private static final ScheduledExecutorService WRITER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "detect-writer");
        thread.setDaemon(true);
        return thread;
    });
    private static final long START_TIME_NANOS = System.nanoTime();

    private Detect() {
    }

    public static void logEnvironment() {
        String body = describeEnvironment();
        System.out.println("[engine.util.Detect]");
        System.out.print(body);
        removeLegacyDetectDirectories();
        ensureFileExists(SYS_LOG_PATH);
        ensureFileExists(ENV_LOG_PATH);
        scheduleWrite(SYS_LOG_PATH, body);
        scheduleWrite(ENV_LOG_PATH, body);
    }

    public static void logGpu() {
        String body = describeGpu();
        removeLegacyDetectDirectories();
        ensureFileExists(GPU_LOG_PATH);
        scheduleWrite(GPU_LOG_PATH, body);
    }

    public static void trigger() {
        logEnvironment();
        logGpu();
    }

    public static boolean isTriggerKey(int key, int action) {
        return key == GLFW_KEY_L && action == GLFW_PRESS;
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

    private static String describeEnvironment() {
        StringBuilder log = new StringBuilder();
        appendLine(log, "os", osName());
        appendLine(log, "linuxDistro", linuxDistro());
        appendLine(log, "desktop", env("XDG_CURRENT_DESKTOP"));
        appendLine(log, "sessionType", env("XDG_SESSION_TYPE"));
        appendLine(log, "sessionDesktop", env("XDG_SESSION_DESKTOP"));
        appendLine(log, "glfwPlatform", glfwPlatformName());
        appendLine(log, "display", env("DISPLAY"));
        appendLine(log, "waylandDisplay", env("WAYLAND_DISPLAY"));
        appendLine(log, "qtPlatform", env("QT_QPA_PLATFORM"));
        return log.toString();
    }

    private static String describeGpu() {
        String renderer = glString(GL_RENDERER);
        String vendor = glString(GL_VENDOR);
        String version = glString(GL_VERSION);

        StringBuilder log = new StringBuilder();
        appendLine(log, "renderer", renderer);
        appendLine(log, "vendor", vendor);
        appendLine(log, "driverVersion", version);
        appendLine(log, "backend", graphicsBackend(renderer, version));
        appendLine(log, "videoMemoryMiB", detectVideoMemoryMiB());
        return log.toString();
    }

    private static String graphicsBackend(String renderer, String version) {
        String normalized = (renderer + " " + version).toLowerCase(Locale.ROOT);
        if (normalized.contains("zink")) {
            return "zink";
        }
        if (normalized.contains("llvmpipe")) {
            return "llvmpipe";
        }
        return "opengl";
    }

    private static String detectVideoMemoryMiB() {
        try {
            GLCapabilities capabilities = GL.getCapabilities();
            if (capabilities != null && capabilities.GL_NVX_gpu_memory_info) {
                int totalKilobytes = glGetInteger(GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX);
                if (totalKilobytes > 0) {
                    return Integer.toString(totalKilobytes / 1024);
                }
            }
        } catch (RuntimeException exception) {
            return "<unavailable>";
        }
        return "<unavailable>";
    }

    private static String glString(int parameter) {
        String value = glGetString(parameter);
        return value == null || value.isBlank() ? "<unavailable>" : value;
    }

    private static String linuxDistro() {
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

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
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

    private static void ensureFileExists(Path path) {
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException exception) {
            System.err.println("[engine.util.Detect] failed to create " + path + ": " + exception.getMessage());
        }
    }

    private static void removeLegacyDetectDirectories() {
        deleteDirectoryIfPresent(LEGACY_SYS_LOG_DIRECTORY);
        deleteDirectoryIfPresent(LEGACY_SYS_PACKAGE_DIRECTORY);
    }

    private static void deleteDirectoryIfPresent(Path path) {
        if (!Files.isDirectory(path)) {
            return;
        }

        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(entry -> {
                        try {
                            Files.deleteIfExists(entry);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    });
        } catch (IOException | RuntimeException exception) {
            System.err.println("[engine.util.Detect] failed to remove legacy path " + path + ": " + exception.getMessage());
        }
    }

    private static void scheduleWrite(Path path, String body) {
        long elapsedNanos = System.nanoTime() - START_TIME_NANOS;
        long remainingNanos = Math.max(0L, TimeUnit.SECONDS.toNanos(WRITE_DELAY_SECONDS) - elapsedNanos);
        WRITER.schedule(() -> write(path, body), remainingNanos, TimeUnit.NANOSECONDS);
    }

    private static void write(Path path, String body) {
        try {
            Files.writeString(path, body);
        } catch (IOException exception) {
            System.err.println("[engine.util.Detect] failed to write " + path + ": " + exception.getMessage());
        }
    }
}
