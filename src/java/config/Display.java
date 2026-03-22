package config;

import engine.display.Mode;
import engine.display.VSync;

public final class Display {

    public static Display defaults() {
        return new Display(Mode.WINDOWED, Mode.BORDERLESS, false, VSync.DOUBLE_BUFFERED);
    }

    private final Mode windowMode;
    private final Mode fullscreenMode;
    private final boolean rawInputEnabled;
    private final VSync vSync;

    public Display(Mode windowMode, Mode fullscreenMode, boolean rawInputEnabled, VSync vSync) {
        this.windowMode = windowMode == null ? Mode.WINDOWED : windowMode;
        this.fullscreenMode = fullscreenMode == null ? Mode.BORDERLESS : fullscreenMode;
        this.rawInputEnabled = rawInputEnabled;
        this.vSync = vSync == null ? VSync.DOUBLE_BUFFERED : vSync;
    }

    public Mode getWindowMode() {
        return windowMode;
    }

    public Mode getFullscreenMode() {
        return fullscreenMode;
    }

    public boolean isRawInputEnabled() {
        return rawInputEnabled;
    }

    public VSync getVSync() {
        return vSync;
    }
}
