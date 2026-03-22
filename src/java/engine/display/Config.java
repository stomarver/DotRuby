package engine.display;

public final class Config {

    public static Config defaults() {
        return new Config("DotRuby", 960, 540, false, VSync.DOUBLE_BUFFERED, 0.0f, 0.0f, 0.0f, 1.0f);
    }

    private final String title;
    private final int width;
    private final int height;
    private final boolean resizable;
    private final VSync vSync;
    private final float clearR;
    private final float clearG;
    private final float clearB;
    private final float clearA;

    public Config(String title,
                  int width,
                  int height,
                  boolean resizable,
                  VSync vSync,
                  float clearR,
                  float clearG,
                  float clearB,
                  float clearA) {
        this.title = title == null || title.isBlank() ? "DotRuby" : title;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.resizable = resizable;
        this.vSync = vSync == null ? VSync.DOUBLE_BUFFERED : vSync;
        this.clearR = clearR;
        this.clearG = clearG;
        this.clearB = clearB;
        this.clearA = clearA;
    }

    public String getTitle() {
        return title;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isResizable() {
        return resizable;
    }

    public VSync getVSync() {
        return vSync;
    }

    public float getClearR() {
        return clearR;
    }

    public float getClearG() {
        return clearG;
    }

    public float getClearB() {
        return clearB;
    }

    public float getClearA() {
        return clearA;
    }
}
