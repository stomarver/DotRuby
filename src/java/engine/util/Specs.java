package engine.util;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import com.sun.management.OperatingSystemMXBean;

import java.io.IOException;
import java.lang.management.ManagementFactory;
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
    private static final Path ENV_LOG_PATH = LOG_DIRECTORY.resolve("env.log");
    private static final Path GPU_LOG_PATH = LOG_DIRECTORY.resolve("gpu.log");
    private static final int GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX = 0x9048;

    private Specs() {
    }

    public static void updateLogs() {
        String env = describeEnvironment();
        String gpu = describeGpu();

        deleteIfExists(LOG_DIRECTORY.resolve("sys.log"));
        write(ENV_LOG_PATH, env);
        write(GPU_LOG_PATH, gpu);
    }

    private static String describeEnvironment() {
        StringBuilder log = new StringBuilder();
        appendLine(log, "distro", distroLabel());
        if (Detect.isLinux()) {
            appendLine(log, "kernel", System.getProperty("os.version", "<unset>"));
            appendLine(log, "desktop", Detect.env("XDG_CURRENT_DESKTOP"));
            appendLine(log, "sessionType", Detect.env("XDG_SESSION_TYPE"));
        }
        appendLine(log, "platform", Detect.glfwPlatformName());
        appendLine(log, "javaVersion", System.getProperty("java.version", "<unset>"));
        appendLine(log, "memory", systemMemory());
        return log.toString();
    }

    private static String describeGpu() {
        String renderer = glString(GL_RENDERER);
        String version = glString(GL_VERSION);

        StringBuilder log = new StringBuilder();
        appendLine(log, "renderer", renderer);
        appendLine(log, "driver", detectDriver(version));
        appendLine(log, "memory", detectVideoMemory());
        return log.toString();
    }

    private static String detectVideoMemory() {
        try {
            GLCapabilities capabilities = GL.getCapabilities();
            if (capabilities != null && capabilities.GL_NVX_gpu_memory_info) {
                int totalKilobytes = glGetInteger(GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX);
                if (totalKilobytes > 0) {
                    return formatMiB(totalKilobytes / 1024L);
                }
            }
        } catch (RuntimeException exception) {
            return "<unavailable>";
        }
        return "<unavailable>";
    }

    private static String detectDriver(String version) {
        String normalized = version.toLowerCase(Locale.ROOT);
        if (normalized.contains("mesa")) {
            return "Mesa";
        }
        if (normalized.contains("metal")) {
            return "Metal";
        }
        if (normalized.contains("zink")) {
            return "Zink";
        }
        return version;
    }

    private static String glString(int parameter) {
        String value = glGetString(parameter);
        return value == null || value.isBlank() ? "<unavailable>" : value;
    }

    private static String distroLabel() {
        if (Detect.isLinux()) {
            return Detect.linuxDistro();
        }
        String osName = System.getProperty("os.name", "<unknown>");
        String osVersion = System.getProperty("os.version", "");
        return osVersion.isBlank() ? osName : osName + " " + osVersion;
    }

    private static String systemMemory() {
        long allocated = Runtime.getRuntime().maxMemory();
        long totalPhysical = totalPhysicalMemory();
        if (totalPhysical <= 0L) {
            return formatMiB(allocated / (1024L * 1024L)) + " / <unavailable>";
        }
        return formatMiB(allocated / (1024L * 1024L)) + " / " + formatMiB(totalPhysical / (1024L * 1024L));
    }

    private static long totalPhysicalMemory() {
        try {
            if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean bean) {
                return bean.getTotalMemorySize();
            }
        } catch (RuntimeException exception) {
            return -1L;
        }
        return -1L;
    }

    private static String formatMiB(long mib) {
        return mib + " MiB";
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
            System.err.println("[engine.util.Specs] failed to write " + path + ": " + exception.getMessage());
        }
    }

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            System.err.println("[engine.util.Specs] failed to delete " + path + ": " + exception.getMessage());
        }
    }
}
