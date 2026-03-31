package engine.visual.scene;

public enum SceneType {
    TWO_D(true, false),
    THREE_D(false, true),
    TWO_D_AND_THREE_D(true, true);

    private final boolean requires2D;
    private final boolean requires3D;

    SceneType(boolean requires2D, boolean requires3D) {
        this.requires2D = requires2D;
        this.requires3D = requires3D;
    }

    public boolean requires2D() {
        return requires2D;
    }

    public boolean requires3D() {
        return requires3D;
    }
}
