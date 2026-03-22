package engine.util;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glGetString;

public final class Specs {

    private static final Path LOG_DIRECTORY = Path.of("src/java/detect");
    private static final Path SYS_LOG_PATH = LOG_DIRECTORY.resolve("sys.log");
    private static final Path ENV_LOG_PATH = LOG_DIRECTORY.resolve("env.log");
    private static final Path GPU_LOG_PATH = LOG_DIRECTORY.resolve("gpu.log");
    private static final int GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX = 0x9048;

    private Specs() {
    }

    public static void updateLogs() {
        String sys = describeSystem();
        String env = describeEnvironment();
        String gpu = describeGpu();

        write(SYS_LOG_PATH, sys);
        write(ENV_LOG_PATH, env);
        write(GPU_LOG_PATH, gpu);

        System.out.println("[engine.util.Specs]");
        System.out.print(sys);
    }

    private static String describeSystem() {
        String renderer = glString(GL_RENDERER);
        String vendor = glString(GL_VENDOR);
        String version = glString(GL_VERSION);

        StringBuilder log = new StringBuilder();
        appendLine(log, "os", Detect.osName());
        appendLine(log, "linuxDistro", Detect.linuxDistro());
        appendLine(log, "glfwPlatform", Detect.glfwPlatformName());
        appendLine(log, "sessionType", Detect.env("XDG_SESSION_TYPE"));
        appendLine(log, "desktop", Detect.env("XDG_CURRENT_DESKTOP"));
        appendLine(log, "backend", graphicsBackend(renderer, version));
        appendLine(log, "renderer", renderer);
        appendLine(log, "vendor", vendor);
        return log.toString();
    }

    private static String describeEnvironment() {
        StringBuilder log = new StringBuilder();
        appendLine(log, "os", Detect.osName());
        appendLine(log, "linuxDistro", Detect.linuxDistro());
        appendLine(log, "display", Detect.env("DISPLAY"));
        appendLine(log, "waylandDisplay", Detect.env("WAYLAND_DISPLAY"));
        appendLine(log, "desktop", Detect.env("XDG_CURRENT_DESKTOP"));
        appendLine(log, "sessionType", Detect.env("XDG_SESSION_TYPE"));
        appendLine(log, "sessionDesktop", Detect.env("XDG_SESSION_DESKTOP"));
        appendLine(log, "qtPlatform", Detect.env("QT_QPA_PLATFORM"));
        appendLine(log, "javaVersion", System.getProperty("java.version", "<unset>"));
        appendLine(log, "osArch", System.getProperty("os.arch", "<unset>"));
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

    private static void appendLine(StringBuilder log, String key, String value) {
        log.append("  ")
                .append(String.format("%-15s", key))
                .append(" = ")
                .append(value)
                .append('\n');
    }

    private static void write(Path path, String body) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, body);
        } catch (IOException exception) {
            System.err.println("[engine.util.Specs] failed to write " + path + ": " + exception.getMessage());
        }
    }
}
