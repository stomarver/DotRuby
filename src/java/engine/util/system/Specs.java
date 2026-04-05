package engine.util.system;

import com.sun.management.OperatingSystemMXBean;
import engine.util.path.RuntimePaths;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glGetString;

public final class Specs {

    private static final Path LOG_DIRECTORY = RuntimePaths.logDirectory();
    private static final Path GPU_LOG_PATH = LOG_DIRECTORY.resolve("gpu.log");
    private static final Path CPU_LOG_PATH = LOG_DIRECTORY.resolve("cpu.log");
    private static final int GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX = 0x9048;
    private static final Pattern MESA_DRIVER_PATTERN = Pattern.compile("(Mesa\\s+[\\w.\\-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern METAL_DRIVER_PATTERN = Pattern.compile("(Metal[^,;)]*)", Pattern.CASE_INSENSITIVE);

    private Specs() {
    }

    public static void updateLogs() {
        Detect.updateEnvLog();
        write(GPU_LOG_PATH, describeGpu());
        write(CPU_LOG_PATH, describeCpu());
        deleteIfExists(LOG_DIRECTORY.resolve("sys.log"));
    }

    private static String describeGpu() {
        String renderer = glString(GL_RENDERER);
        String version = glString(GL_VERSION);

        StringBuilder log = new StringBuilder();
        appendLine(log, "vendor", glString(GL_VENDOR));
        appendLine(log, "renderer", renderer);
        appendLine(log, "driver", detectDriver(version));
        appendLine(log, "vram", detectVideoMemory());
        return log.toString();
    }

    private static String describeCpu() {
        StringBuilder log = new StringBuilder();
        appendLine(log, "model", cpuModel());
        appendLine(log, "arch", System.getProperty("os.arch", "<unset>"));
        appendLine(log, "cores", String.valueOf(Runtime.getRuntime().availableProcessors()));
        appendLine(log, "ram", totalPhysicalMemoryLabel());
        appendLine(log, "heapMax", formatMiB(Runtime.getRuntime().maxMemory() / (1024L * 1024L)));
        return log.toString();
    }

    private static String cpuModel() {
        String model = System.getenv("PROCESSOR_IDENTIFIER");
        if (model != null && !model.isBlank()) {
            return model;
        }

        String archName = System.getProperty("os.arch", "");
        return archName.isBlank() ? "<unavailable>" : archName;
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
        String mesa = extract(version, MESA_DRIVER_PATTERN);
        if (mesa != null) {
            return mesa;
        }

        String metal = extract(version, METAL_DRIVER_PATTERN);
        if (metal != null) {
            return metal;
        }

        return version;
    }

    private static String glString(int parameter) {
        String value = glGetString(parameter);
        return value == null || value.isBlank() ? "<unavailable>" : value;
    }

    private static String totalPhysicalMemoryLabel() {
        long totalPhysical = totalPhysicalMemory();
        if (totalPhysical <= 0L) {
            return "<unavailable>";
        }
        return formatMiB(totalPhysical / (1024L * 1024L));
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
            System.err.println("[engine.util.system.Specs] failed to write " + path + ": " + exception.getMessage());
        }
    }

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            System.err.println("[engine.util.system.Specs] failed to delete " + path + ": " + exception.getMessage());
        }
    }

    private static String extract(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).trim();
    }
}
