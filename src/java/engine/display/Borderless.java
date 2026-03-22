package engine.display;

import detect.Detect;
import org.lwjgl.glfw.GLFWNativeX11;

import java.io.IOException;

public final class Borderless {

    private Borderless() {
    }

    public static void apply(long windowHandle) {
        if (!Detect.isLinux() || !Detect.isGlfwX11()) {
            return;
        }

        long x11Window = getX11Window(windowHandle);
        if (x11Window == 0L) {
            return;
        }

        runIfAvailable("xprop", "-id", Long.toUnsignedString(x11Window),
                "-f", "_MOTIF_WM_HINTS", "32c",
                "-set", "_MOTIF_WM_HINTS", "2, 0, 0, 0, 0");
    }

    private static long getX11Window(long windowHandle) {
        try {
            return GLFWNativeX11.glfwGetX11Window(windowHandle);
        } catch (RuntimeException exception) {
            return 0L;
        }
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
