package engine.display;

import org.lwjgl.glfw.GLFWNativeX11;

import java.io.IOException;
import java.util.Locale;

public final class Borderless {

    private Borderless() {
    }

    public static void apply(long windowHandle) {
        if (!isLinuxDesktop()) {
            return;
        }

        long x11Window = GLFWNativeX11.glfwGetX11Window(windowHandle);
        if (x11Window == 0L) {
            return;
        }

        runIfAvailable("xprop", "-id", Long.toUnsignedString(x11Window),
                "-f", "_MOTIF_WM_HINTS", "32c",
                "-set", "_MOTIF_WM_HINTS", "2, 0, 0, 0, 0");
    }

    private static boolean isLinuxDesktop() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("linux")) {
            return false;
        }

        String session = System.getenv("XDG_CURRENT_DESKTOP");
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        String qtPlatform = System.getenv("QT_QPA_PLATFORM");

        return containsAny(session, "gnome", "kde", "xfce", "mate", "cinnamon")
                || containsAny(sessionType, "x11")
                || containsAny(qtPlatform, "xcb");
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (normalized.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static void runIfAvailable(String... command) {
        try {
            new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException ignored) {
        }
    }
}
