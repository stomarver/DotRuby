package engine.display;

public enum VSync {
    DISABLED(0),
    DOUBLE_BUFFERED(1),
    TRIPLE_BUFFERED(1);

    private final int swapInterval;

    VSync(int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public int getSwapInterval() {
        return swapInterval;
    }

    public boolean isEnabled() {
        return this != DISABLED;
    }
}
